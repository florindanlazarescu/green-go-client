package green.go

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import green.go.databinding.ActivityMainBinding
import green.go.network.DeliveryRepository
import green.go.network.RetrofitClient
import green.go.viewmodel.DeliveryViewModel
import green.go.viewmodel.DeliveryViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Adapter
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Link BottomNavigationView with ViewPager2
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_pending -> binding.viewPager.currentItem = 0
                R.id.navigation_in_progress -> binding.viewPager.currentItem = 1
                R.id.navigation_picked_up -> binding.viewPager.currentItem = 2
                R.id.navigation_my_deliveries -> binding.viewPager.currentItem = 3
            }
            true
        }

        // Update BottomNav when swiping
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val menu = binding.navView.menu
                when (position) {
                    0 -> {
                        binding.navView.selectedItemId = R.id.navigation_pending
                        supportActionBar?.title = menu.findItem(R.id.navigation_pending).title
                    }
                    1 -> {
                        binding.navView.selectedItemId = R.id.navigation_in_progress
                        supportActionBar?.title = menu.findItem(R.id.navigation_in_progress).title
                    }
                    2 -> {
                        binding.navView.selectedItemId = R.id.navigation_picked_up
                        supportActionBar?.title = menu.findItem(R.id.navigation_picked_up).title
                    }
                    3 -> {
                        binding.navView.selectedItemId = R.id.navigation_my_deliveries
                        supportActionBar?.title = menu.findItem(R.id.navigation_my_deliveries).title
                    }
                }
            }
        })

        // Observe pending count for badge
        viewModel.pendingCount.observe(this) { count ->
            val badge = binding.navView.getOrCreateBadge(R.id.navigation_pending)
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
