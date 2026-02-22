package green.go

import android.content.Context
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
import green.go.utils.SessionManager
import green.go.viewmodel.DeliveryViewModel
import green.go.viewmodel.DeliveryViewModelFactory

class MainActivity : AppCompatActivity() {

    companion object {
        // --- CONFIGURARE REFRESH ---
        private const val REFRESH_INTERVAL_MS = 15000L
    }

    private lateinit var binding: ActivityMainBinding
    private var doubleBackToExitPressedOnce = false

    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(DeliveryRepository(RetrofitClient.instance))
    }

    private val handler = Handler(Looper.getMainLooper())
    
    private val refreshRunnable = object : Runnable {
        override fun run() {
            val courierId = getCourierId()
            val currentTab = binding.viewPager.currentItem

            if (courierId != -1L) {
                // Dacă suntem pe ecranul Pending, un singur apel actualizează tot (listă + badge)
                if (currentTab == 0) {
                    viewModel.fetchPendingDeliveries()
                } else {
                    // Dacă suntem pe alt ecran, actualizăm doar badge-ul (silent)
                    viewModel.silentFetchPendingCount()
                }
            }

            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackPressedHandling()
        setupViewPager()
        setupBadgeObservation()

        supportActionBar?.title = "Pending"
        handler.post(refreshRunnable)
        
        supportFragmentManager.addOnBackStackChangedListener {
            val hasBackStack = supportFragmentManager.backStackEntryCount > 0
            binding.viewPager.isUserInputEnabled = !hasBackStack
            supportActionBar?.setDisplayHomeAsUpEnabled(hasBackStack)
            if (!hasBackStack) {
                updateTitleFromPager(binding.viewPager.currentItem)
            }
        }
    }

    private fun getCourierId(): Long {
        val prefs = getSharedPreferences(SessionManager.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(SessionManager.KEY_ID, -1L)
    }

    private fun setupBackPressedHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    return
                }

                if (doubleBackToExitPressedOnce) {
                    isEnabled = false
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
            val id = getCourierId()
            when (item.itemId) {
                R.id.navigation_pending -> {
                    binding.viewPager.currentItem = 0
                    viewModel.fetchPendingDeliveries() // Un singur apel
                }
                R.id.navigation_in_progress -> {
                    binding.viewPager.currentItem = 1
                    if (id != -1L) viewModel.fetchInProgressDeliveries(id)
                }
                R.id.navigation_picked_up -> {
                    binding.viewPager.currentItem = 2
                    if (id != -1L) viewModel.fetchPickedUpDeliveries(id)
                }
                R.id.navigation_my_deliveries -> {
                    binding.viewPager.currentItem = 3
                    if (id != -1L) viewModel.fetchHistory(id)
                }
            }
            true
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTitleFromPager(position)
            }
        })
    }

    private fun updateTitleFromPager(position: Int) {
        val menu = binding.navView.menu
        val itemId = when (position) {
            0 -> R.id.navigation_pending
            1 -> R.id.navigation_in_progress
            2 -> R.id.navigation_picked_up
            3 -> R.id.navigation_my_deliveries
            else -> R.id.navigation_pending
        }
        binding.navView.selectedItemId = itemId
        supportActionBar?.title = menu.findItem(itemId).title
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

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                supportActionBar?.title = "Profile"
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(android.R.id.content, ProfileFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            android.R.id.home -> {
                onSupportNavigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
