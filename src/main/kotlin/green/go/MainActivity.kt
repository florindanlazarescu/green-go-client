package green.go

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var navView: BottomNavigationView

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

        // Set default title
        supportActionBar?.title = "Pending"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                // Open Profile as a separate transaction over the pager
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
