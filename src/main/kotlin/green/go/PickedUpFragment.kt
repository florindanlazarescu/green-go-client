package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var rvDeliveries: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var ivEmptyState: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity).supportActionBar?.title = "Picked Up"

        val view = inflater.inflate(R.layout.fragment_delivery_list, container, false)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = view.findViewById(R.id.tvEmptyDesc)
        rvDeliveries = view.findViewById(R.id.rvDeliveries)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        progressBar = view.findViewById(R.id.progressBar)
        ivEmptyState = view.findViewById(R.id.ivEmptyState)

        tvEmptyTitle.setText(R.string.empty_pickedup_title)
        tvEmptyDesc.setText(R.string.empty_pickedup_desc)
        ivEmptyState.setImageResource(R.drawable.picked_up)

        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_ACTIVE) { delivery ->
            handleDeliveryClick(delivery)
        }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            fetchPickedUpDeliveries(isManualRefresh = true)
        }
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)

        fetchPickedUpDeliveries(isManualRefresh = false)

        return view
    }

    private fun handleDeliveryClick(delivery: Delivery) {
        if (delivery.status == "PICKED_UP") {
            val bottomSheet = PickDeliveryBottomSheet(
                delivery = delivery,
                buttonText = "Delivered",
                questionText = "Confirm you have successfully delivered this order."
            ) {
                updateDeliveryStatus(it, "DELIVERED")
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

        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.updateDeliveryStatus(
                    orderId = delivery.orderId,
                    status = newStatus,
                    courierId = courierId
                )
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(context, response.body()?.message ?: "Order Updated", Toast.LENGTH_SHORT).show()
                        fetchPickedUpDeliveries(isManualRefresh = false)
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

    private fun fetchPickedUpDeliveries(isManualRefresh: Boolean) {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val id = prefs.getLong(SessionManager.KEY_ID, -1L)

        if (id == -1L) {
             llEmptyState.visibility = View.VISIBLE
             rvDeliveries.visibility = View.GONE
             return
        }

        if (!isManualRefresh) {
            progressBar.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
            rvDeliveries.visibility = View.GONE
        }

        val request = DeliverySearchRequest(
            queryFilter = QueryFilter(ByCourier(id)),
            statuses = listOf("PICKED_UP")
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.searchDeliveries(request)
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
                            adapter.updateData(deliveries)
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
}
