package com.epatay.digitalwallet.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.epatay.digitalwallet.databinding.FragmentProfileBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ProfileBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentProfileBottomSheetBinding? = null
    private val binding get() = _binding!!

    // viewModel attached to activity so it persists during config changes
    private val authViewModel: AuthViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // requireContext() YERİNE requireActivity() YAZIYORUZ
        binding.btnGoogleSignIn.setOnClickListener {
            authViewModel.signInWithGoogle(requireActivity())
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.signOut(requireActivity())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: AuthState) {
        when (state) {
            is AuthState.Idle -> {
                binding.layoutLoggedOut.visibility = View.VISIBLE
                binding.layoutLoggedIn.visibility = View.GONE
                binding.btnGoogleSignIn.isEnabled = true
            }
            is AuthState.Loading -> {
                binding.btnGoogleSignIn.isEnabled = false
                binding.btnLogout.isEnabled = false
            }
            is AuthState.Authenticated -> {
                binding.layoutLoggedOut.visibility = View.GONE
                binding.layoutLoggedIn.visibility = View.VISIBLE
                binding.btnLogout.isEnabled = true
                
                binding.tvUserName.text = state.name ?: "Bilinmeyen Kullanıcı"
                binding.tvUserEmail.text = state.email ?: ""
                state.photoUrl?.let { 
                    binding.ivUserProfile.load(it) { 
                        crossfade(true)
                        fallback(com.epatay.digitalwallet.R.drawable.ic_account_circle)
                        error(com.epatay.digitalwallet.R.drawable.ic_account_circle)
                    } 
                }
            }
            is AuthState.Error -> {
                binding.btnGoogleSignIn.isEnabled = true
                binding.btnLogout.isEnabled = true
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                // Revert to Idle UI if we were previously idle, but checkCurrentUser() in ViewModel 
                // makes it easier to just let it show the error and stay in the same visual state.
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
