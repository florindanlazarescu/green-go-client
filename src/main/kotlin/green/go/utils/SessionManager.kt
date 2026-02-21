package green.go.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREF_NAME = "GreenGoSession"
        const val KEY_TOKEN = "token"
        const val KEY_ID = "id"
        const val KEY_EMAIL = "email"
        const val KEY_ROLE = "role"
        const val KEY_TARIFF = "tariff"
    }

    fun saveAuthToken(token: String, id: Long, email: String, role: String?, tariff: Double?) {
        val editor = prefs.edit()
        editor.putString(KEY_TOKEN, token)
        editor.putLong(KEY_ID, id)
        editor.putString(KEY_EMAIL, email)
        
        // Dacă rolul lipsește, încercăm să-l extragem din token
        val finalRole = role ?: extractRoleFromToken(token) ?: "USER"
        
        editor.putString(KEY_ROLE, finalRole)
        editor.putFloat(KEY_TARIFF, (tariff ?: 0.0).toFloat())
        editor.apply()
    }

    fun extractRoleFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            
            val payload = String(Base64.decode(parts[1], Base64.DEFAULT))
            val jsonObject = JSONObject(payload)
            jsonObject.optString("role", null)
        } catch (e: Exception) {
            null
        }
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
