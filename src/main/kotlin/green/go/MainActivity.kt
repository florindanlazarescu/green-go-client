package green.go

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        navView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.navigation_my_deliveries -> selectedFragment = MyDeliveriesFragment()
                R.id.navigation_pending -> selectedFragment = PendingFragment()
                R.id.navigation_in_progress -> selectedFragment = InProgressFragment()
                R.id.navigation_picked_up -> selectedFragment = PickedUpFragment()
            }
            if (selectedFragment != null) {
                replaceFragment(selectedFragment)
            }
            true
        }

        // Set default fragment
        if (savedInstanceState == null) {
            navView.selectedItemId = R.id.navigation_pending
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                replaceFragment(ProfileFragment())
                // Optionally deselect bottom nav items or handle back stack
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
