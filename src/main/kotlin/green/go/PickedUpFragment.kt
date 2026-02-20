package green.go

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class PickedUpFragment : Fragment() {

    private var _binding: FragmentDeliveryListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DeliveryAdapter

    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            val courierId = getCourierId()
            if (courierId != -1L) {
                viewModel.fetchPickedUpDeliveries(courierId)
            }
            handler.postDelayed(this, 10000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.title = "Picked Up"
        _binding = FragmentDeliveryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmptyTitle.setText(R.string.empty_pickedup_title)
        binding.tvEmptyDesc.setText(R.string.empty_pickedup_desc)
        binding.ivEmptyState.setImageResource(R.drawable.picked_up)

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
        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_ACTIVE) { delivery ->
            handleDeliveryClick(delivery)
        }
        binding.rvDeliveries.layoutManager = LinearLayoutManager(context)
        binding.rvDeliveries.adapter = adapter
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            val id = getCourierId()
            if (id != -1L) viewModel.fetchPickedUpDeliveries(id)
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
                    adapter.updateData(state.deliveries)
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
                        binding.tvEmptyTitle.text = "Error"
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
                val id = getCourierId()
                if (id != -1L) viewModel.fetchPickedUpDeliveries(id)
            }
            .show()
    }

    private fun getCourierId(): Long {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getLong(SessionManager.KEY_ID, -1L)
    }

    private fun handleDeliveryClick(delivery: Delivery) {
        if (delivery.status == "PICKED_UP") {
            val bottomSheet = PickDeliveryBottomSheet(
                delivery = delivery,
                buttonText = "Delivered",
                questionText = "Confirm you have successfully delivered this order."
            ) {
                viewModel.updateDeliveryStatus(it, "DELIVERED", getCourierId())
            }
            bottomSheet.show(parentFragmentManager, "PickDeliveryBottomSheet")
        }
    }
}
