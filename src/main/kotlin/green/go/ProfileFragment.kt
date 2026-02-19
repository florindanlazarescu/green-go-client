package green.go

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import green.go.network.DeliveryRepository
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import green.go.viewmodel.DeliveryViewModel
import green.go.viewmodel.DeliveryViewModelFactory

class ProfileFragment : Fragment() {

    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity).supportActionBar?.title = "Profile"

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val tvUserInfo = view.findViewById<TextView>(R.id.tvUserInfo)
        val tvRole = view.findViewById<TextView>(R.id.tvRole)
        val tvDeliveriesCount = view.findViewById<TextView>(R.id.tvDeliveriesCount)
        val tvEarningsAmount = view.findViewById<TextView>(R.id.tvEarningsAmount)
        val cvChangePassword = view.findViewById<CardView>(R.id.cvChangePassword)
        val cvLogout = view.findViewById<CardView>(R.id.cvLogout)

        val sessionManager = SessionManager(requireContext())
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val email = prefs.getString(SessionManager.KEY_EMAIL, "Unknown User")
        val role = prefs.getString(SessionManager.KEY_ROLE, "USER")
        val courierId = prefs.getLong(SessionManager.KEY_ID, -1L)

        tvUserInfo.text = email
        
        tvRole.text = when(role?.uppercase()) {
            "ADMIN" -> "Administrator"
            "COURIER" -> "Courier"
            "MERCHANT" -> "Merchant"
            else -> "User"
        }

        // Observe stats from ViewModel
        viewModel.todayStats.observe(viewLifecycleOwner) { stats ->
            tvDeliveriesCount.text = stats.count.toString()
            tvEarningsAmount.text = String.format("%.2f RON", stats.earnings)
        }

        // Initial fetch of stats
        if (courierId != -1L) {
            viewModel.fetchTodayStats(courierId)
        }

        cvChangePassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(android.R.id.content, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        cvLogout.setOnClickListener {
            showLogoutConfirmation(sessionManager)
        }

        return view
    }

    private fun showLogoutConfirmation(sessionManager: SessionManager) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out from GreenGO?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Logout") { _, _ ->
                sessionManager.clearSession()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .show()
    }
}
