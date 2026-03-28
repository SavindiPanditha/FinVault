package com.example.imilipocket.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.imilipocket.R
import com.example.imilipocket.auth.SupabaseAuthService
import com.example.imilipocket.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var authService: SupabaseAuthService
    private val financeViewModel: FinanceViewModel by viewModels()
    private val TAG = "MainActivity"
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        authService = SupabaseAuthService(this)
        if (!authService.isSignedIn()) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsets()
        requestNotificationPermissionIfNeeded()
        financeViewModel.scheduleDailyReminder()
        setupNavigation()
    }

    private fun applyInsets() {
        val navHost = binding.navHostFragment
        val navContainer = binding.bottomNavContainer

        val navHostTopPadding = navHost.paddingTop
        val navContainerBaseMarginBottom =
            (navContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).bottomMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            navHost.setPadding(
                navHost.paddingLeft,
                navHostTopPadding + systemBars.top,
                navHost.paddingRight,
                navHost.paddingBottom
            )

            navContainer.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                bottomMargin = navContainerBaseMarginBottom + systemBars.bottom
            }

            insets
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment == null) {
            Log.e(TAG, "NavHostFragment not found with ID nav_host_fragment. Check activity_main.xml and nav_graph.xml")
            return
        }
        val navController = navHostFragment.navController
        val navView: BottomNavigationView = binding.navView
        navView.setupWithNavController(navController)
    }
}