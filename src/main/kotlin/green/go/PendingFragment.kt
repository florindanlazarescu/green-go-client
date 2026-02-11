package green.go

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PendingFragment : Fragment() {

    private lateinit var adapter: DeliveryAdapter
    private lateinit var tvEmpty: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchPendingDeliveries()
            handler.postDelayed(this, 10000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivery_list, container, false)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        val rvDeliveries = view.findViewById<RecyclerView>(R.id.rvDeliveries)

        tvTitle.text = "Pending Deliveries"

        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_PENDING) { delivery ->
            showPickOrderDialog(delivery)
        }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
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
                            // Sort by pickup time
                            val sortedDeliveries = deliveries.sortedBy {
                                parseDate(it.pickUpTime).time
                            }
                            adapter.updateData(sortedDeliveries)
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

    private fun parseDate(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getDefault()
        return try {
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun showPickOrderDialog(delivery: Delivery) {
        val message = "Address: ${delivery.pickupAddress}\n" +
                "Deliver to: ${delivery.deliveryAddress}\n" +
                "Items: ${delivery.items}\n" +
                "Cost: $${delivery.cost}\n" +
                "Pickup Time: ${delivery.pickUpTime.replace("T", " ").replace("Z", "")}\n\n" +
                "Want to pick this order?"

        AlertDialog.Builder(requireContext())
            .setTitle("Order #${delivery.orderId}")
            .setMessage(message)
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
