package com.icc.silent_help.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.icc.silent_help.R
import com.icc.silent_help.ui.biometrics.BiometricsFragment
import com.icc.silent_help.ui.history.HistoryFragment
import com.icc.silent_help.ui.home.HomeFragment
import com.icc.silent_help.ui.sensors.SensorsFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.navigation_home -> selectedFragment = HomeFragment()
                R.id.navigation_history -> selectedFragment = HistoryFragment()
                R.id.navigation_biometrics -> selectedFragment = BiometricsFragment()
                R.id.navigation_sensors -> selectedFragment = SensorsFragment()
            }

            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }

        // Cargar el fragmento inicial
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_home
        }
    }
}
