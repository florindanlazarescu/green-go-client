package green.go

import android.app.Application
import green.go.network.RetrofitClient

class GreenGoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
