package green.go.network

import android.content.Context
import green.go.BuildConfig
import green.go.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    private var sessionManager: SessionManager? = null

    fun init(context: Context) {
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

                val newRequest = if (!token.isNullOrBlank()) {
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    originalRequest
                }
                chain.proceed(newRequest)
            }
            .build()
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
