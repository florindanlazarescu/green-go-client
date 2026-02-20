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
import green.go.databinding.FragmentDeliveryListBinding
import green.go.model.Delivery
import green.go.model.DeliveryState
import green.go.network.DeliveryRepository
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import green.go.viewmodel.DeliveryViewModel
import green.go.viewmodel.DeliveryViewModelFactory

class InProgressFragment : Fragment() {

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
                viewModel.fetchInProgressDeliveries(courierId)
            }
            handler.postDelayed(this, 10000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.title = "In Progress"
        _binding = FragmentDeliveryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmptyTitle.setText(R.string.empty_inprogress_title)
        binding.tvEmptyDesc.setText(R.string.empty_inprogress_desc)
        binding.ivEmptyState.setImageResource(R.drawable.in_progress)

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
            if (id != -1L) viewModel.fetchInProgressDeliveries(id)
        }
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
    }

    private fun observeViewModel() {
        viewModel.deliveryState.observe(viewLifecycleOwner) { state ->
            binding.swipeRefreshLayout.isRefreshing = false
            when (state) {
                is DeliveryState.Loading -> {
                    if (!binding.swipeRefreshLayout.isRefreshing && adapter.itemCount == 0) {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.llEmptyState.visibility = View.GONE
                        binding.rvDeliveries.visibility = View.GONE
                    }
                }
                is DeliveryState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.llEmptyState.visibility = View.GONE
                    binding.rvDeliveries.visibility = View.VISIBLE
                    adapter.updateData(state.deliveries)
                }
                is DeliveryState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.llEmptyState.visibility = View.VISIBLE
                    binding.rvDeliveries.visibility = View.GONE
                    adapter.updateData(emptyList())
                }
                is DeliveryState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.llEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyTitle.text = "Error"
                    binding.tvEmptyDesc.text = state.message
                    binding.rvDeliveries.visibility = View.GONE
                }
            }
        }
    }

    private fun getCourierId(): Long {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getLong(SessionManager.KEY_ID, -1L)
    }

    private fun handleDeliveryClick(delivery: Delivery) {
        if (delivery.status == "IN_PROGRESS") {
            val bottomSheet = PickDeliveryBottomSheet(
                delivery = delivery,
                buttonText = "Picked Up",
                questionText = "Confirm you have picked up this order from the merchant."
            ) {
                viewModel.updateDeliveryStatus(it, "PICKED_UP", getCourierId())
            }
            bottomSheet.show(parentFragmentManager, "PickDeliveryBottomSheet")
        }
    }
}
