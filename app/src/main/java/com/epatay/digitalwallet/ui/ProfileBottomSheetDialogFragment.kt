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
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.epatay.digitalwallet.worker.DailyProfitLossWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ProfileBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentProfileBottomSheetBinding? = null
    private val binding get() = _binding!!

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

        binding.btnGoogleSignIn.setOnClickListener {
            authViewModel.signInWithGoogle(requireActivity())
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.signOut(requireActivity())
        }

        binding.btnDeleteAccount.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(com.epatay.digitalwallet.R.string.delete_account_dialog_title))
                .setMessage(getString(com.epatay.digitalwallet.R.string.delete_account_dialog_message))
                .setPositiveButton(getString(com.epatay.digitalwallet.R.string.delete_account_confirm)) { _, _ ->
                    authViewModel.deleteAccountAndData(requireActivity())
                }
                .setNegativeButton(getString(com.epatay.digitalwallet.R.string.delete_account_cancel), null)
                .show()
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
                isCancelable = true
                binding.layoutLoading.visibility = View.GONE
                binding.layoutLoggedOut.visibility = View.VISIBLE
                binding.layoutLoggedIn.visibility = View.GONE
                binding.btnGoogleSignIn.isEnabled = true
            }
            is AuthState.Loading -> {
                isCancelable = false
                binding.layoutLoading.visibility = View.VISIBLE
                binding.tvLoadingMessage.text = state.message
                binding.layoutLoggedOut.visibility = View.GONE
                binding.layoutLoggedIn.visibility = View.GONE
            }
            is AuthState.Authenticated -> {
                isCancelable = true
                binding.layoutLoading.visibility = View.GONE
                binding.layoutLoggedOut.visibility = View.GONE
                binding.layoutLoggedIn.visibility = View.VISIBLE
                binding.btnLogout.isEnabled = true
                binding.btnDeleteAccount.isEnabled = true
                
                binding.tvUserName.text = state.name ?: "Kullanıcı"
                binding.tvUserEmail.text = state.email ?: ""
                state.photoUrl?.let { 
                    binding.ivUserProfile.load(it) { 
                        crossfade(true)
                        fallback(com.epatay.digitalwallet.R.drawable.ic_account_circle)
                        error(com.epatay.digitalwallet.R.drawable.ic_account_circle)
                    } 
                }
            }
            is AuthState.ActionSuccess -> {
                isCancelable = true
                binding.layoutLoading.visibility = View.GONE
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                val intent = android.content.Intent(requireActivity(), com.epatay.digitalwallet.ui.login.LoginActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                dismissAllowingStateLoss()
            }
            is AuthState.SignedOut -> {
                isCancelable = true
                binding.layoutLoading.visibility = View.GONE
                val intent = android.content.Intent(requireActivity(), com.epatay.digitalwallet.ui.login.LoginActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                dismissAllowingStateLoss()
            }
            is AuthState.Error -> {
                isCancelable = true
                binding.layoutLoading.visibility = View.GONE
                binding.btnGoogleSignIn.isEnabled = true
                binding.btnLogout.isEnabled = true
                binding.btnDeleteAccount.isEnabled = true
                com.epatay.digitalwallet.util.InAppNotification.show(
                    activity,
                    state.message,
                    com.epatay.digitalwallet.util.NotificationType.ERROR
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
