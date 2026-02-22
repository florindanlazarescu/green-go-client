package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
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

abstract class BaseDeliveryFragment : Fragment() {

    private var _binding: FragmentDeliveryListBinding? = null
    protected val binding get() = _binding!!

    protected lateinit var adapter: DeliveryAdapter

    protected val viewModel: DeliveryViewModel by activityViewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

    abstract fun getTitle(): String
    abstract fun getEmptyTitleRes(): Int
    abstract fun getEmptyDescRes(): Int
    abstract fun getEmptyImageRes(): Int
    abstract fun getAdapterMode(): Int
    abstract fun fetchData(isManualRefresh: Boolean)
    abstract fun onDeliveryClick(delivery: Delivery)
    
    // NOU: Fiecare fragment trebuie sa specifice ce LiveData de stare foloseste
    abstract fun getStateLiveData(): LiveData<DeliveryState>
    
    open fun isAutoRefreshEnabled(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as? AppCompatActivity)?.supportActionBar?.title = getTitle()
        _binding = FragmentDeliveryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmptyTitle.setText(getEmptyTitleRes())
        binding.tvEmptyDesc.setText(getEmptyDescRes())
        binding.ivEmptyState.setImageResource(getEmptyImageRes())

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
        adapter = DeliveryAdapter(emptyList(), getAdapterMode()) { delivery ->
            onDeliveryClick(delivery)
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
        // Acum ascultam de LiveData-ul specific fragmentului
        getStateLiveData().observe(viewLifecycleOwner) { state ->
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
                    adapter.updateData(sortDeliveries(state.deliveries))
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

    protected open fun sortDeliveries(deliveries: List<Delivery>): List<Delivery> = deliveries

    protected fun startShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
    }

    protected fun stopShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_INDEFINITE)
            .setAction("RETRY") { fetchData(isManualRefresh = true) }
            .show()
    }

    protected fun getCourierId(): Long {
        val context = context ?: return -1L
        val prefs = context.getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getLong(SessionManager.KEY_ID, -1L)
    }

    protected fun parseDate(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getDefault()
        return try {
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}
