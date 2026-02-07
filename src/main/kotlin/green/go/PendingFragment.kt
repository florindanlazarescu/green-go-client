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
import green.go.model.Delivery
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PendingFragment : Fragment() {

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

        tvTitle.text = "Pending Deliveries"

        adapter = DeliveryAdapter(emptyList()) { delivery ->
            showPickOrderDialog(delivery)
        }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter

        fetchPendingDeliveries()

        return view
    }

    private fun fetchPendingDeliveries() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getPendingDeliveries()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val deliveries = response.body()?.deliveries ?: emptyList()
                        if (deliveries.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "No pending deliveries."
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

    private fun showPickOrderDialog(delivery: Delivery) {
        AlertDialog.Builder(requireContext())
            .setMessage("Want to pick this order?")
            .setPositiveButton("Yes") { _, _ ->
                confirmPickOrder(delivery)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun confirmPickOrder(delivery: Delivery) {
        val sessionManager = SessionManager(requireContext())
        // We know we saved ID as long
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
                    status = "IN_PROGRESS",
                    courierId = courierId
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, response.body()?.message ?: "Order Updated", Toast.LENGTH_SHORT).show()
                        // Refresh list to remove the updated item
                        fetchPendingDeliveries()
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
}
