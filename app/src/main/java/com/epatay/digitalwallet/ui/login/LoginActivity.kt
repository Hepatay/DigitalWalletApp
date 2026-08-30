package com.epatay.digitalwallet.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.epatay.digitalwallet.MainActivity
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.databinding.ActivityLoginBinding
import com.epatay.digitalwallet.ui.AuthState
import com.epatay.digitalwallet.ui.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        val isGuestMode = prefs.getBoolean("is_guest_mode", false)

        // Check if already logged in or guest
        if (FirebaseAuth.getInstance().currentUser != null || isGuestMode) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnGoogleLogin.setOnClickListener {
            authViewModel.signInWithGoogle(this)
        }

        binding.btnGuestLogin.setOnClickListener {
            prefs.edit().putBoolean("is_guest_mode", true).apply()
            navigateToMain()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: AuthState) {
        when (state) {
            is AuthState.Loading -> {
                binding.progressIndicator.visibility = View.VISIBLE
                binding.btnGoogleLogin.isEnabled = false
                binding.btnGuestLogin.isEnabled = false
            }
            is AuthState.Authenticated -> {
                val prefs = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_guest_mode", false).apply()
                navigateToMain()
            }
            is AuthState.Error -> {
                binding.progressIndicator.visibility = View.GONE
                binding.btnGoogleLogin.isEnabled = true
                binding.btnGuestLogin.isEnabled = true
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
            is AuthState.ActionSuccess -> {
                // Not used in Login normally, but just in case
            }
            is AuthState.Idle -> {
                binding.progressIndicator.visibility = View.GONE
                binding.btnGoogleLogin.isEnabled = true
                binding.btnGuestLogin.isEnabled = true
            }
            is AuthState.SignedOut -> {
                binding.progressIndicator.visibility = View.GONE
                binding.btnGoogleLogin.isEnabled = true
                binding.btnGuestLogin.isEnabled = true
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply { putExtra("from_login", true) })
        finish()
    }
}
