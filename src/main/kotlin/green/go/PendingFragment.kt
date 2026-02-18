package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var rvDeliveries: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity).supportActionBar?.title = "Pending"

        val view = inflater.inflate(R.layout.fragment_delivery_list, container, false)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = view.findViewById(R.id.tvEmptyDesc)
        rvDeliveries = view.findViewById(R.id.rvDeliveries)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        progressBar = view.findViewById(R.id.progressBar)

        tvEmptyTitle.setText(R.string.empty_pending_title)
        tvEmptyDesc.setText(R.string.empty_pending_desc)

        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_PENDING) { delivery ->
            showPickOrderDialog(delivery)
        }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            fetchPendingDeliveries(isManualRefresh = true)
        }
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)

        fetchPendingDeliveries(isManualRefresh = false)

        return view
    }

    private fun fetchPendingDeliveries(isManualRefresh: Boolean) {
        if (!isManualRefresh) {
            progressBar.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
            rvDeliveries.visibility = View.GONE
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getPendingDeliveries()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    
                    if (response.isSuccessful) {
                        val deliveries = response.body()?.deliveries ?: emptyList()
                        if (deliveries.isEmpty()) {
                            llEmptyState.visibility = View.VISIBLE
                            rvDeliveries.visibility = View.GONE
                            adapter.updateData(emptyList())
                        } else {
                            llEmptyState.visibility = View.GONE
                            rvDeliveries.visibility = View.VISIBLE
                            val sortedDeliveries = deliveries.sortedBy { parseDate(it.pickUpTime).time }
                            adapter.updateData(sortedDeliveries)
                        }
                    } else {
                        llEmptyState.visibility = View.VISIBLE
                        rvDeliveries.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    llEmptyState.visibility = View.VISIBLE
                    rvDeliveries.visibility = View.GONE
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
        val bottomSheet = PickDeliveryBottomSheet(delivery, onConfirm = {
            confirmPickOrder(it)
        })
        bottomSheet.show(parentFragmentManager, "PickDeliveryBottomSheet")
    }

    private fun confirmPickOrder(delivery: Delivery) {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val courierId = prefs.getLong(SessionManager.KEY_ID, -1L)

        if (courierId == -1L) {
            Toast.makeText(context, "Error: User ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.updateDeliveryStatus(
                    orderId = delivery.orderId,
                    status = "IN_PROGRESS",
                    courierId = courierId
                )
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(context, response.body()?.message ?: "Order Updated", Toast.LENGTH_SHORT).show()
                        fetchPendingDeliveries(isManualRefresh = false)
                    } else {
                        Toast.makeText(context, "Update Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
