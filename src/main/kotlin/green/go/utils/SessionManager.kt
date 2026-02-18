package green.go.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREF_NAME = "GreenGoSession"
        const val KEY_TOKEN = "token"
        const val KEY_ID = "id"
        const val KEY_EMAIL = "email"
        const val KEY_ROLE = "role"
    }

    fun saveAuthToken(token: String, id: Long, email: String, role: String?) {
        val editor = prefs.edit()
        editor.putString(KEY_TOKEN, token)
        editor.putLong(KEY_ID, id)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_ROLE, role ?: "USER") // Default to USER if null
        editor.apply()
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
