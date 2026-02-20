package green.go

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import green.go.databinding.FragmentDeliveryListBinding
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

    private var _binding: FragmentDeliveryListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: DeliveryAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            viewModel.fetchPendingDeliveries()
            handler.postDelayed(this, 10000)
        }
    }

    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.title = "Pending"
        _binding = FragmentDeliveryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmptyTitle.setText(R.string.empty_pending_title)
        binding.tvEmptyDesc.setText(R.string.empty_pending_desc)
        binding.ivEmptyState.setImageResource(R.drawable.pending)

        setupRecyclerView()
        setupPullToRefresh()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_PENDING) { delivery ->
            showPickOrderDialog(delivery)
        }
        binding.rvDeliveries.layoutManager = LinearLayoutManager(context)
        binding.rvDeliveries.adapter = adapter
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchPendingDeliveries()
        }
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
    }

    private fun observeViewModel() {
        viewModel.deliveryState.observe(viewLifecycleOwner) { state ->
            binding.swipeRefreshLayout.isRefreshing = false
            
            when (state) {
                is DeliveryState.Loading -> {
                    if (!binding.swipeRefreshLayout.isRefreshing && adapter.itemCount == 0) {
                        startShimmer()
                        binding.llEmptyState.visibility = View.GONE
                        binding.rvDeliveries.visibility = View.GONE
                    }
                }
                is DeliveryState.Success -> {
                    stopShimmer()
                    binding.llEmptyState.visibility = View.GONE
                    binding.rvDeliveries.visibility = View.VISIBLE
                    val sorted = state.deliveries.sortedBy { parseDate(it.pickUpTime).time }
                    adapter.updateData(sorted)
                }
                is DeliveryState.Empty -> {
                    stopShimmer()
                    binding.llEmptyState.visibility = View.VISIBLE
                    binding.rvDeliveries.visibility = View.GONE
                    adapter.updateData(emptyList())
                }
                is DeliveryState.Error -> {
                    stopShimmer()
                    showErrorSnackbar(state.message)
                    if (adapter.itemCount == 0) {
                        binding.llEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Connection Error"
                        binding.tvEmptyDesc.text = state.message
                    }
                }
            }
        }

        viewModel.statusUpdateResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "Order updated successfully", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Failed to update order", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun startShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
    }

    private fun stopShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_INDEFINITE)
            .setAction("RETRY") {
                viewModel.fetchPendingDeliveries()
            }
            .show()
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
