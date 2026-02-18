package green.go

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import green.go.model.LoginRequest
import green.go.network.RetrofitClient
import green.go.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Splash Screen before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        setupInputValidation()

        btnLogin.setOnClickListener {
            val email = tilEmail.editText?.text.toString()
            val password = tilPassword.editText?.text.toString()

            if (validateInputs(email, password)) {
                performLogin(email, password)
            }
        }
    }

    private fun setupInputValidation() {
        tilEmail.editText?.doAfterTextChanged {
            if (it.toString().isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(it.toString()).matches()) {
                tilEmail.error = "Invalid email format"
            } else {
                tilEmail.error = null
            }
        }

        tilPassword.editText?.doAfterTextChanged {
            if (it.toString().length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
            } else {
                tilPassword.error = null
            }
        }
    }

    private fun validateInputs(email: String, pass: String): Boolean {
        if (email.isBlank() || pass.isBlank()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        if (tilEmail.error != null || tilPassword.error != null) {
            return false
        }
        return true
    }

    private fun performLogin(email: String, pass: String) {
        findViewById<Button>(R.id.btnLogin).isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, pass))
                withContext(Dispatchers.Main) {
                    findViewById<Button>(R.id.btnLogin).isEnabled = true
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.token != null) {
                            val sessionManager = SessionManager(this@LoginActivity)
                            sessionManager.saveAuthToken(body.token, body.id, body.email, body.role)
                            
                            Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Login Failed: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findViewById<Button>(R.id.btnLogin).isEnabled = true
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
