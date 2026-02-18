package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import green.go.model.ChangePasswordRequest
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordFragment : Fragment() {

    private lateinit var tilOldPassword: TextInputLayout
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity).supportActionBar?.title = "Change Password"

        val view = inflater.inflate(R.layout.fragment_change_password, container, false)

        tilOldPassword = view.findViewById(R.id.tilOldPassword)
        tilNewPassword = view.findViewById(R.id.tilNewPassword)
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitChangePassword)

        setupValidation()

        btnSubmit.setOnClickListener {
            val oldPass = tilOldPassword.editText?.text.toString()
            val newPass = tilNewPassword.editText?.text.toString()
            val confirmPass = tilConfirmPassword.editText?.text.toString()

            if (validateInputs(oldPass, newPass, confirmPass)) {
                performPasswordChange(oldPass, newPass)
            }
        }

        return view
    }

    private fun setupValidation() {
        tilOldPassword.editText?.doAfterTextChanged {
            if (it.toString().isBlank()) {
                tilOldPassword.error = "Current password cannot be empty"
            } else {
                tilOldPassword.error = null
            }
        }

        tilNewPassword.editText?.doAfterTextChanged {
            val newPass = it.toString()
            val confirmPass = tilConfirmPassword.editText?.text.toString()

            if (newPass.length < 6) {
                tilNewPassword.error = "Password must be at least 6 characters"
            } else {
                tilNewPassword.error = null
            }

            if (confirmPass.isNotEmpty() && newPass != confirmPass) {
                tilConfirmPassword.error = "Passwords do not match"
            } else {
                tilConfirmPassword.error = null
            }
        }

        tilConfirmPassword.editText?.doAfterTextChanged {
            val newPass = tilNewPassword.editText?.text.toString()
            if (it.toString() != newPass) {
                tilConfirmPassword.error = "Passwords do not match"
            } else {
                tilConfirmPassword.error = null
            }
        }
    }

    private fun validateInputs(old: String, new: String, confirm: String): Boolean {
        if (old.isBlank() || new.isBlank() || confirm.isBlank()) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return tilOldPassword.error == null && tilNewPassword.error == null && tilConfirmPassword.error == null
    }

    private fun performPasswordChange(oldPass: String, newPass: String) {
        val prefs = requireContext().getSharedPreferences(SessionManager.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val email = prefs.getString(SessionManager.KEY_EMAIL, "") ?: ""

        if (email.isBlank()) {
            Toast.makeText(context, "Error: User email not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = ChangePasswordRequest(email, oldPass, newPass)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.changePassword(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, response.body()?.message ?: "Password changed successfully", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(context, "Failed to change password", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
