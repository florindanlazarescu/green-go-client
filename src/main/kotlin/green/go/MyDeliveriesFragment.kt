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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import green.go.model.DeliveryState
import green.go.network.DeliveryRepository
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import green.go.viewmodel.DeliveryViewModel
import green.go.viewmodel.DeliveryViewModelFactory

class MyDeliveriesFragment : Fragment() {

    private lateinit var adapter: DeliveryAdapter
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyDesc: TextView
    private lateinit var rvDeliveries: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

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

        setupRecyclerView()
        setupPullToRefresh()
        observeViewModel()

        fetchData(isManualRefresh = false)

        return view
    }

    private fun setupRecyclerView() {
        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_STANDARD) { delivery ->
            val bottomSheet = DeliveryDetailsBottomSheet(delivery)
            bottomSheet.show(parentFragmentManager, "DeliveryDetailsBottomSheet")
        }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            fetchData(isManualRefresh = true)
        }
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
    }

    private fun observeViewModel() {
        viewModel.deliveryState.observe(viewLifecycleOwner) { state ->
            swipeRefreshLayout.isRefreshing = false
            when (state) {
                is DeliveryState.Loading -> {
                    if (!swipeRefreshLayout.isRefreshing && adapter.itemCount == 0) {
                        progressBar.visibility = View.VISIBLE
                        llEmptyState.visibility = View.GONE
                        rvDeliveries.visibility = View.GONE
                    }
                }
                is DeliveryState.Success -> {
                    progressBar.visibility = View.GONE
                    llEmptyState.visibility = View.GONE
                    rvDeliveries.visibility = View.VISIBLE
                    adapter.updateData(state.deliveries)
                }
                is DeliveryState.Empty -> {
                    progressBar.visibility = View.GONE
                    llEmptyState.visibility = View.VISIBLE
                    rvDeliveries.visibility = View.GONE
                    adapter.updateData(emptyList())
                }
                is DeliveryState.Error -> {
                    progressBar.visibility = View.GONE
                    llEmptyState.visibility = View.VISIBLE
                    tvEmptyTitle.text = "Error"
                    tvEmptyDesc.text = state.message
                    rvDeliveries.visibility = View.GONE
                }
            }
        }
    }

    private fun fetchData(isManualRefresh: Boolean) {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val id = prefs.getLong(SessionManager.KEY_ID, -1L)
        if (id != -1L) {
            viewModel.fetchHistory(id)
        }
    }
}
