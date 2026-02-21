package green.go

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
    private var doubleBackToExitPressedOnce = false

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

        setupBackPressedHandling()
        setupViewPager()
        setupBadgeObservation()

        // Set default title
        supportActionBar?.title = "Pending"
        
        // Start background badge check
        handler.post(badgeRefreshRunnable)
    }

    private fun setupBackPressedHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    return
                }

                if (doubleBackToExitPressedOnce) {
                    isEnabled = false // Disable callback to allow default behavior
                    onBackPressedDispatcher.onBackPressed()
                    return
                }

                doubleBackToExitPressedOnce = true
                Toast.makeText(this@MainActivity, "Press again to exit", Toast.LENGTH_SHORT).show()

                handler.postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
            }
        })
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_pending -> binding.viewPager.currentItem = 0
                R.id.navigation_in_progress -> binding.viewPager.currentItem = 1
                R.id.navigation_picked_up -> binding.viewPager.currentItem = 2
                R.id.navigation_my_deliveries -> binding.viewPager.currentItem = 3
            }
            true
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val menu = binding.navView.menu
                when (position) {
                    0 -> updateUIState(R.id.navigation_pending, menu.findItem(R.id.navigation_pending).title.toString())
                    1 -> updateUIState(R.id.navigation_in_progress, menu.findItem(R.id.navigation_in_progress).title.toString())
                    2 -> updateUIState(R.id.navigation_picked_up, menu.findItem(R.id.navigation_picked_up).title.toString())
                    3 -> updateUIState(R.id.navigation_my_deliveries, menu.findItem(R.id.navigation_my_deliveries).title.toString())
                }
            }
        })
    }

    private fun updateUIState(itemId: Int, title: String) {
        binding.navView.selectedItemId = itemId
        supportActionBar?.title = title
    }

    private fun setupBadgeObservation() {
        viewModel.pendingCount.observe(this) { count ->
            val badge = binding.navView.getOrCreateBadge(R.id.navigation_pending)
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
            } else {
                badge.isVisible = false
            }
        }
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
