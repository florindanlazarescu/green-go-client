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
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.navigation_my_deliveries -> MyDeliveriesFragment()
                R.id.navigation_pending -> PendingFragment()
                R.id.navigation_in_progress -> InProgressFragment()
                R.id.navigation_picked_up -> PickedUpFragment()
                else -> null
            }
            
            if (selectedFragment != null) {
                supportActionBar?.title = item.title
                replaceFragment(selectedFragment)
            }
            true
        }

        // Set default fragment and initial title
        if (savedInstanceState == null) {
            navView.selectedItemId = R.id.navigation_pending
            supportActionBar?.title = "Pending"
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
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
                supportActionBar?.title = "Profile"
                replaceFragment(ProfileFragment())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
