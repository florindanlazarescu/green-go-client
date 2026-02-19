package green.go

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import green.go.network.DeliveryRepository
import green.go.network.RetrofitClient
import green.go.viewmodel.DeliveryViewModel
import green.go.viewmodel.DeliveryViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var navView: BottomNavigationView

    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

    private val handler = Handler(Looper.getMainLooper())
    private val badgeRefreshRunnable = object : Runnable {
        override fun run() {
            viewModel.silentFetchPendingCount()
            handler.postDelayed(this, 15000) // Check every 15 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        navView = findViewById(R.id.nav_view)

        // Setup Adapter
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // Link BottomNavigationView with ViewPager2
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_pending -> viewPager.currentItem = 0
                R.id.navigation_in_progress -> viewPager.currentItem = 1
                R.id.navigation_picked_up -> viewPager.currentItem = 2
                R.id.navigation_my_deliveries -> viewPager.currentItem = 3
            }
            true
        }

        // Update BottomNav when swiping
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val menu = navView.menu
                when (position) {
                    0 -> {
                        navView.selectedItemId = R.id.navigation_pending
                        supportActionBar?.title = menu.findItem(R.id.navigation_pending).title
                    }
                    1 -> {
                        navView.selectedItemId = R.id.navigation_in_progress
                        supportActionBar?.title = menu.findItem(R.id.navigation_in_progress).title
                    }
                    2 -> {
                        navView.selectedItemId = R.id.navigation_picked_up
                        supportActionBar?.title = menu.findItem(R.id.navigation_picked_up).title
                    }
                    3 -> {
                        navView.selectedItemId = R.id.navigation_my_deliveries
                        supportActionBar?.title = menu.findItem(R.id.navigation_my_deliveries).title
                    }
                }
            }
        })

        // Observe pending count for badge
        viewModel.pendingCount.observe(this) { count ->
            val badge = navView.getOrCreateBadge(R.id.navigation_pending)
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
            } else {
                badge.isVisible = false
            }
        }

        // Set default title
        supportActionBar?.title = "Pending"
        
        // Start background badge check
        handler.post(badgeRefreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(badgeRefreshRunnable)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(android.R.id.content, ProfileFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
