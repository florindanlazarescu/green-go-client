package green.go

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.widget.doAfterTextChanged
import green.go.databinding.ActivityLoginBinding
import green.go.model.LoginRequest
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputValidation()

        binding.btnLogin.setOnClickListener {
            val email = binding.tilEmail.editText?.text.toString()
            val password = binding.tilPassword.editText?.text.toString()

            if (validateInputs(email, password)) {
                performLogin(email, password)
            }
        }
    }

    private fun setupInputValidation() {
        binding.tilEmail.editText?.doAfterTextChanged {
            if (it.toString().isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(it.toString()).matches()) {
                binding.tilEmail.error = "Invalid email format"
            } else {
                binding.tilEmail.error = null
            }
        }

        binding.tilPassword.editText?.doAfterTextChanged {
            if (it.toString().length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
            } else {
                binding.tilPassword.error = null
            }
        }
    }

    private fun validateInputs(email: String, pass: String): Boolean {
        if (email.isBlank() || pass.isBlank()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return binding.tilEmail.error == null && binding.tilPassword.error == null
    }

    private fun performLogin(email: String, pass: String) {
        binding.btnLogin.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, pass))
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.token != null) {
                            // DEBUG MESSAGE: Vedem exact ce a venit de la server
                            val debugInfo = "Role: ${body.role}, Tariff: ${body.tariff}"
                            Toast.makeText(this@LoginActivity, debugInfo, Toast.LENGTH_LONG).show()

                            val sessionManager = SessionManager(this@LoginActivity)
                            sessionManager.saveAuthToken(body.token, body.id, body.email, body.role, body.tariff)
                            
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Login Failed: No Token", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Server error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this@LoginActivity, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
