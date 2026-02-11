package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyDeliveriesFragment : Fragment() {

    private lateinit var adapter: DeliveryAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Reuse the generic list layout
        val view = inflater.inflate(R.layout.fragment_delivery_list, container, false)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        val rvDeliveries = view.findViewById<RecyclerView>(R.id.rvDeliveries)

        tvTitle.text = "Delivered Orders"

        // No click action defined for history items yet
        adapter = DeliveryAdapter(emptyList(), DeliveryAdapter.MODE_STANDARD) { }
        rvDeliveries.layoutManager = LinearLayoutManager(context)
        rvDeliveries.adapter = adapter

        fetchMyDeliveries()

        return view
    }

    private fun fetchMyDeliveries() {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val id = prefs.getLong(SessionManager.KEY_ID, -1L)

        if (id == -1L) {
             tvEmpty.visibility = View.VISIBLE
             tvEmpty.text = "Error: User ID not found in session."
             return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getDeliveriesByCourier(id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val deliveries = response.body()?.deliveries ?: emptyList()

                        // Filter for only DELIVERED
                        val filtered = deliveries.filter { it.status == "DELIVERED" }

                        if (filtered.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "No delivered orders found."
                            adapter.updateData(emptyList())
                        } else {
                            tvEmpty.visibility = View.GONE
                            adapter.updateData(filtered)
                        }
                    } else {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Error: ${response.code()} ${response.message()}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Exception: ${e.message}"
                }
            }
        }
    }
}
