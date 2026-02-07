package green.go

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import green.go.utils.SessionManager

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val tvUserInfo = view.findViewById<TextView>(R.id.tvUserInfo)
        val btnChangePassword = view.findViewById<Button>(R.id.btnChangePassword)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        val sessionManager = SessionManager(requireContext())
        // Accessing prefs directly to get email for display
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val email = prefs.getString(SessionManager.KEY_EMAIL, "Unknown User")

        tvUserInfo.text = "Logged in as: $email"

        btnChangePassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        btnLogout.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            // Clear back stack and start fresh
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }
}
