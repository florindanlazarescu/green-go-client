package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class InProgressFragment : Fragment() {

    private lateinit var adapter: DeliveryAdapter
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var rvDeliveries: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity).supportActionBar?.title = "In Progress"

        val view = inflater.inflate(R.layout.fragment_delivery_list, container, false)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = view.findViewById(R.id.tvEmptyDesc)
        rvDeliveries = view.findViewById(R.id.rvDeliveries)

        tvEmptyTitle.setText(R.string.empty_inprogress_title)
        tvEmptyDesc.setText(R.string.empty_inprogress_desc)

        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_ACTIVE) { delivery ->
            handleDeliveryClick(delivery)
        }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter

        fetchInProgressDeliveries()

        return view
    }

    private fun handleDeliveryClick(delivery: Delivery) {
        if (delivery.status == "IN_PROGRESS") {
            val bottomSheet = PickDeliveryBottomSheet(
                delivery = delivery,
                buttonText = "Picked Up",
                questionText = "Confirm you have picked up this order from the merchant."
            ) {
                updateDeliveryStatus(it, "PICKED_UP")
            }
            bottomSheet.show(parentFragmentManager, "PickDeliveryBottomSheet")
        }
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
                        fetchInProgressDeliveries()
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

    private fun fetchInProgressDeliveries() {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val id = prefs.getLong(SessionManager.KEY_ID, -1L)

        if (id == -1L) {
             llEmptyState.visibility = View.VISIBLE
             rvDeliveries.visibility = View.GONE
             return
        }

        val request = DeliverySearchRequest(
            queryFilter = QueryFilter(ByCourier(id)),
            statuses = listOf("IN_PROGRESS")
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.searchDeliveries(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val deliveries = response.body()?.deliveries ?: emptyList()
                        if (deliveries.isEmpty()) {
                            llEmptyState.visibility = View.VISIBLE
                            rvDeliveries.visibility = View.GONE
                            adapter.updateData(emptyList())
                        } else {
                            llEmptyState.visibility = View.GONE
                            rvDeliveries.visibility = View.VISIBLE
                            adapter.updateData(deliveries)
                        }
                    } else {
                        llEmptyState.visibility = View.VISIBLE
                        rvDeliveries.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    llEmptyState.visibility = View.VISIBLE
                    rvDeliveries.visibility = View.GONE
                }
            }
        }
    }
}
