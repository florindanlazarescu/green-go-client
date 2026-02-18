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
import green.go.utils.SessionManager

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity).supportActionBar?.title = "Profile"

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val tvUserInfo = view.findViewById<TextView>(R.id.tvUserInfo)
        val tvRole = view.findViewById<TextView>(R.id.tvRole)
        val cvChangePassword = view.findViewById<CardView>(R.id.cvChangePassword)
        val cvLogout = view.findViewById<CardView>(R.id.cvLogout)

        val sessionManager = SessionManager(requireContext())
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val email = prefs.getString(SessionManager.KEY_EMAIL, "Unknown User")
        val role = prefs.getString(SessionManager.KEY_ROLE, "USER")

        tvUserInfo.text = email
        
        tvRole.text = when(role?.uppercase()) {
            "ADMIN" -> "Administrator"
            "COURIER" -> "Courier"
            "MERCHANT" -> "Merchant"
            else -> "User"
        }

        cvChangePassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.nav_host_fragment, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        cvLogout.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }
}
