package green.go.network

import android.content.Context
import android.content.Intent
import green.go.BuildConfig
import green.go.LoginActivity
import green.go.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    private var sessionManager: SessionManager? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        if (sessionManager == null) {
            sessionManager = SessionManager(context.applicationContext)
        }
    }

    private val client by lazy {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val token = sessionManager?.fetchAuthToken()

                val requestBuilder = originalRequest.newBuilder()
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                
                val request = requestBuilder.build()
                val response = chain.proceed(request)

                // Verificăm dacă sesiunea a expirat (Eroare 401)
                if (response.code == 401) {
                    handleUnauthorized()
                }

                response
            }
            .build()
    }

    private fun handleUnauthorized() {
        // Ștergem sesiunea locală
        sessionManager?.clearSession()

        // Trimitem utilizatorul la LoginActivity și închidem restul ecranelor
        appContext?.let { context ->
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
