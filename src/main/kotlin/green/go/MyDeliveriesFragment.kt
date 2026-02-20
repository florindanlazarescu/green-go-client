package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import green.go.databinding.FragmentDeliveryListBinding
import green.go.model.DeliveryState
import green.go.network.DeliveryRepository
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import green.go.viewmodel.DeliveryViewModel
import green.go.viewmodel.DeliveryViewModelFactory

class MyDeliveriesFragment : Fragment() {

    private var _binding: FragmentDeliveryListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DeliveryAdapter

    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.title = "History"
        _binding = FragmentDeliveryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmptyTitle.text = "No order history"
        binding.tvEmptyDesc.text = "Your delivered orders will appear here"

        setupRecyclerView()
        setupPullToRefresh()
        observeViewModel()

        fetchData(isManualRefresh = false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_STANDARD) { delivery ->
            val bottomSheet = DeliveryDetailsBottomSheet(delivery)
            bottomSheet.show(parentFragmentManager, "DeliveryDetailsBottomSheet")
        }
        binding.rvDeliveries.layoutManager = LinearLayoutManager(context)
        binding.rvDeliveries.adapter = adapter
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchData(isManualRefresh = true)
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

    private fun fetchData(isManualRefresh: Boolean) {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val id = prefs.getLong(SessionManager.KEY_ID, -1L)
        if (id != -1L) {
            viewModel.fetchHistory(id)
        }
    }
}
