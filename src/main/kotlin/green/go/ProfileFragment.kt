package green.go

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import green.go.databinding.FragmentProfileBinding
import green.go.network.DeliveryRepository
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import green.go.viewmodel.DeliveryViewModel
import green.go.viewmodel.DeliveryViewModelFactory
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.title = "Profile"
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val email = prefs.getString(SessionManager.KEY_EMAIL, "Unknown User")
        val role = prefs.getString(SessionManager.KEY_ROLE, "USER")
        val courierId = prefs.getLong(SessionManager.KEY_ID, -1L)

        // Set version info
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val version = pInfo.versionName
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                pInfo.versionCode.toLong()
            }
            binding.tvAppVersion.text = "Version $version ($code)"
        } catch (e: Exception) {
            binding.tvAppVersion.visibility = View.GONE
        }

        binding.tvUserInfo.text = email
        
        binding.tvRole.text = when(role?.uppercase()) {
            "ADMIN" -> "Administrator"
            "COURIER" -> "Courier"
            "MERCHANT" -> "Merchant"
            else -> "User"
        }

        // Observe stats from ViewModel
        viewModel.todayStats.observe(viewLifecycleOwner) { stats ->
            binding.tvDeliveriesCount.text = stats.count.toString()
            binding.tvEarningsAmount.text = String.format(Locale.getDefault(), "%.2f RON", stats.earnings)
        }

        // Initial fetch of stats using the new server-side calculation endpoint
        if (courierId != -1L) {
            viewModel.fetchTodayStats(courierId)
        }

        binding.cvChangePassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(android.R.id.content, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cvLogout.setOnClickListener {
            showLogoutConfirmation(sessionManager)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
