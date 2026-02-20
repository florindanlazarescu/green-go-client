package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import green.go.databinding.FragmentChangePasswordBinding
import green.go.model.ChangePasswordRequest
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.title = "Change Password"
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupValidation()

        binding.btnSubmitChangePassword.setOnClickListener {
            val oldPass = binding.tilOldPassword.editText?.text.toString()
            val newPass = binding.tilNewPassword.editText?.text.toString()
            val confirmPass = binding.tilConfirmPassword.editText?.text.toString()

            if (validateInputs(oldPass, newPass, confirmPass)) {
                performPasswordChange(oldPass, newPass)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupValidation() {
        binding.tilOldPassword.editText?.doAfterTextChanged {
            if (it.toString().isBlank()) {
                binding.tilOldPassword.error = "Current password cannot be empty"
            } else {
                binding.tilOldPassword.error = null
            }
        }

        binding.tilNewPassword.editText?.doAfterTextChanged {
            val newPass = it.toString()
            val confirmPass = binding.tilConfirmPassword.editText?.text.toString()

            if (newPass.length < 6) {
                binding.tilNewPassword.error = "Password must be at least 6 characters"
            } else {
                binding.tilNewPassword.error = null
            }

            if (confirmPass.isNotEmpty() && newPass != confirmPass) {
                binding.tilConfirmPassword.error = "Passwords do not match"
            } else {
                binding.tilConfirmPassword.error = null
            }
        }

        binding.tilConfirmPassword.editText?.doAfterTextChanged {
            val newPass = binding.tilNewPassword.editText?.text.toString()
            if (it.toString() != newPass) {
                binding.tilConfirmPassword.error = "Passwords do not match"
            } else {
                binding.tilConfirmPassword.error = null
            }
        }
    }

    private fun validateInputs(old: String, new: String, confirm: String): Boolean {
        if (old.isBlank() || new.isBlank() || confirm.isBlank()) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return binding.tilOldPassword.error == null && 
               binding.tilNewPassword.error == null && 
               binding.tilConfirmPassword.error == null
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
