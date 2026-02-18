package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyDeliveriesFragment : Fragment() {

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
        (activity as AppCompatActivity).supportActionBar?.title = "History"

        val view = inflater.inflate(R.layout.fragment_delivery_list, container, false)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = view.findViewById(R.id.tvEmptyDesc)
        rvDeliveries = view.findViewById(R.id.rvDeliveries)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        progressBar = view.findViewById(R.id.progressBar)

        tvEmptyTitle.text = "No order history"
        tvEmptyDesc.text = "Your delivered orders will appear here"

        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_STANDARD) { }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            fetchMyDeliveries(isManualRefresh = true)
        }
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)

        fetchMyDeliveries(isManualRefresh = false)

        return view
    }

    private fun fetchMyDeliveries(isManualRefresh: Boolean) {
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getDeliveriesByCourier(id)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    
                    if (response.isSuccessful) {
                        val deliveries = response.body()?.deliveries ?: emptyList()
                        val filtered = deliveries.filter { it.status == "DELIVERED" }

                        if (filtered.isEmpty()) {
                            llEmptyState.visibility = View.VISIBLE
                            rvDeliveries.visibility = View.GONE
                            adapter.updateData(emptyList())
                        } else {
                            llEmptyState.visibility = View.GONE
                            rvDeliveries.visibility = View.VISIBLE
                            adapter.updateData(filtered)
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
