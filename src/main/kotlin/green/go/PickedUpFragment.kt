package green.go

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

class PickedUpFragment : Fragment() {

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

        setupRecyclerView()
        setupPullToRefresh()
        observeViewModel()

        return view
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun setupRecyclerView() {
        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_ACTIVE) { delivery ->
            handleDeliveryClick(delivery)
        }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter
    }

    private fun setupPullToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            val id = getCourierId()
            if (id != -1L) viewModel.fetchPickedUpDeliveries(id)
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
