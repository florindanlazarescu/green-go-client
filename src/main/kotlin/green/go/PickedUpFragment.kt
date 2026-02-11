package green.go

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import green.go.model.ByCourier
import green.go.model.Delivery
import green.go.model.DeliverySearchRequest
import green.go.model.QueryFilter
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickedUpFragment : Fragment() {

    private lateinit var adapter: DeliveryAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivery_list, container, false)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        val rvDeliveries = view.findViewById<RecyclerView>(R.id.rvDeliveries)

        tvTitle.text = "Picked Up"

        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_ACTIVE) { delivery ->
            handleDeliveryClick(delivery)
        }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter

        fetchPickedUpDeliveries()

        return view
    }

    private fun handleDeliveryClick(delivery: Delivery) {
        if (delivery.status == "PICKED_UP") {
            showConfirmationDialog(
                message = "Confirm you have successfully delivered this order.",
                delivery = delivery,
                nextStatus = "DELIVERED"
            )
        }
    }

    private fun showConfirmationDialog(message: String, delivery: Delivery, nextStatus: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                updateDeliveryStatus(delivery, nextStatus)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateDeliveryStatus(delivery: Delivery, newStatus: String) {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val courierId = prefs.getLong(SessionManager.KEY_ID, -1L)

        if (courierId == -1L) {
            Toast.makeText(context, "Error: User ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.updateDeliveryStatus(
                    orderId = delivery.orderId,
                    status = newStatus,
                    courierId = courierId
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, response.body()?.message ?: "Order Updated", Toast.LENGTH_SHORT).show()
                        fetchPickedUpDeliveries()
                    } else {
                        Toast.makeText(context, "Update Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchPickedUpDeliveries() {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val id = prefs.getLong(SessionManager.KEY_ID, -1L)

        if (id == -1L) {
             tvEmpty.visibility = View.VISIBLE
             tvEmpty.text = "Error: User ID not found in session."
             return
        }

        val request = DeliverySearchRequest(
            queryFilter = QueryFilter(ByCourier(id)),
            statuses = listOf("PICKED_UP")
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.searchDeliveries(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val deliveries = response.body()?.deliveries ?: emptyList()
                        if (deliveries.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "No picked up deliveries."
                            adapter.updateData(emptyList())
                        } else {
                            tvEmpty.visibility = View.GONE
                            adapter.updateData(deliveries)
                        }
                    } else {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Error: ${response.code()} ${response.message()}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Exception: ${e.message}"
                }
            }
        }
    }
}
