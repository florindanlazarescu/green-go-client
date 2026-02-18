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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import green.go.model.Delivery
import green.go.model.DeliveryState
import green.go.network.DeliveryRepository
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import green.go.viewmodel.DeliveryViewModel
import green.go.viewmodel.DeliveryViewModelFactory
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
    private lateinit var ivEmptyState: ImageView

    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

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
        ivEmptyState = view.findViewById(R.id.ivEmptyState)

        tvEmptyTitle.setText(R.string.empty_pending_title)
        tvEmptyDesc.setText(R.string.empty_pending_desc)
        ivEmptyState.setImageResource(R.drawable.pending)

        setupRecyclerView()
        setupPullToRefresh()
        observeViewModel()

        viewModel.fetchPendingDeliveries()

        return view
    }

    private fun setupRecyclerView() {
        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_PENDING) { delivery ->
            showPickOrderDialog(delivery)
        }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchPendingDeliveries()
        }
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
    }

    private fun observeViewModel() {
        viewModel.deliveryState.observe(viewLifecycleOwner) { state ->
            swipeRefreshLayout.isRefreshing = false
            
            when (state) {
                is DeliveryState.Loading -> {
                    if (!swipeRefreshLayout.isRefreshing) progressBar.visibility = View.VISIBLE
                    llEmptyState.visibility = View.GONE
                    rvDeliveries.visibility = View.GONE
                }
                is DeliveryState.Success -> {
                    progressBar.visibility = View.GONE
                    llEmptyState.visibility = View.GONE
                    rvDeliveries.visibility = View.VISIBLE
                    val sorted = state.deliveries.sortedBy { parseDate(it.pickUpTime).time }
                    adapter.updateData(sorted)
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
                    tvEmptyTitle.text = "Connection Error"
                    tvEmptyDesc.text = state.message
                    rvDeliveries.visibility = View.GONE
                }
            }
        }

        viewModel.statusUpdateResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Order updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to update order", Toast.LENGTH_SHORT).show()
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
        val bottomSheet = PickDeliveryBottomSheet(delivery) {
            confirmPickOrder(it)
        }
        bottomSheet.show(parentFragmentManager, "PickDeliveryBottomSheet")
    }

    private fun confirmPickOrder(delivery: Delivery) {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val courierId = prefs.getLong(SessionManager.KEY_ID, -1L)

        if (courierId == -1L) {
            Toast.makeText(context, "Error: User ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.updateDeliveryStatus(delivery, "IN_PROGRESS", courierId)
    }
}
