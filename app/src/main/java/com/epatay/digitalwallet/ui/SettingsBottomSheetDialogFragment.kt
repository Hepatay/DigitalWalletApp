package com.epatay.digitalwallet.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.epatay.digitalwallet.databinding.BottomSheetSettingsBinding
import com.epatay.digitalwallet.ui.tutorial.TutorialManager
import com.epatay.digitalwallet.util.setupMoneyInput
import com.epatay.digitalwallet.worker.DailyProfitLossWorker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SettingsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels({ requireActivity() })
    private val investmentViewModel: InvestmentViewModel by viewModels({ requireActivity() })

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scheduleDailyWorker()
        } else {
            binding.switchDailyNotification.isChecked = false
            Toast.makeText(context, "Bildirim izni reddedildi", Toast.LENGTH_SHORT).show()
            requireContext().getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("daily_notification_enabled", false).apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 0. Observe Fair Use Quota
        transactionViewModel.currentMonthTransactionCount.observe(viewLifecycleOwner) { count ->
            val safeCount = count ?: 0
            binding.tvQuotaTransactionCount.text = "$safeCount / 200"
            binding.progressQuotaTransaction.progress = safeCount.coerceIn(0, 200)
        }

        investmentViewModel.portfolioAssetCount.observe(viewLifecycleOwner) { count ->
            val safeCount = count ?: 0
            binding.tvQuotaPortfolioCount.text = "$safeCount / 20"
            binding.progressQuotaPortfolio.progress = safeCount.coerceIn(0, 20)
        }

        // Theme Mode Selection
        val currentThemeMode = com.epatay.digitalwallet.util.ThemeManager.getThemeMode(requireContext())
        when (currentThemeMode) {
            com.epatay.digitalwallet.util.ThemeManager.THEME_LIGHT -> binding.toggleTheme.check(binding.btnThemeLight.id)
            com.epatay.digitalwallet.util.ThemeManager.THEME_DARK -> binding.toggleTheme.check(binding.btnThemeDark.id)
            else -> binding.toggleTheme.check(binding.btnThemeSystem.id)
        }

        binding.toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newMode = when (checkedId) {
                    binding.btnThemeLight.id -> com.epatay.digitalwallet.util.ThemeManager.THEME_LIGHT
                    binding.btnThemeDark.id -> com.epatay.digitalwallet.util.ThemeManager.THEME_DARK
                    else -> com.epatay.digitalwallet.util.ThemeManager.THEME_SYSTEM
                }
                com.epatay.digitalwallet.util.ThemeManager.setThemeMode(requireContext(), newMode)
            }
        }

        // 1. Load and Save Monthly Limit
        val currentLimit = transactionViewModel.getMonthlyLimit(requireContext())
        if (currentLimit > 0.0) {
            binding.etBudgetLimit.setText(currentLimit.toString())
        }

        binding.etBudgetLimit.setupMoneyInput(layout = binding.layoutBudgetLimit)

        binding.btnSaveLimit.setOnClickListener {
            val inputStr = binding.etBudgetLimit.text?.toString()?.trim().orEmpty()
            if (inputStr.isEmpty()) {
                transactionViewModel.saveMonthlyLimit(requireContext(), 0.0)
                com.epatay.digitalwallet.util.InAppNotification.show(
                    activity,
                    "Aylık limit kaldırıldı",
                    com.epatay.digitalwallet.util.NotificationType.SUCCESS
                )
                dismiss()
                return@setOnClickListener
            }
            val limit = com.epatay.digitalwallet.util.parseMoneyValue(inputStr)
            if (limit == null || limit < 0.0 || limit > 999_999_999.99) {
                binding.layoutBudgetLimit.error = "Geçerli bir limit tutarı girin (En fazla ₺999.999.999,99)"
                binding.etBudgetLimit.requestFocus()
                return@setOnClickListener
            }
            binding.layoutBudgetLimit.error = null
            transactionViewModel.saveMonthlyLimit(requireContext(), limit)
            com.epatay.digitalwallet.util.InAppNotification.show(
                activity,
                "Aylık limit başarıyla güncellendi",
                com.epatay.digitalwallet.util.NotificationType.SUCCESS
            )
            dismiss()
        }

        // 2. Notification Preference
        val prefs = requireContext().getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        binding.switchDailyNotification.isChecked = prefs.getBoolean("daily_notification_enabled", false)

        binding.switchDailyNotification.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("daily_notification_enabled", isChecked).apply()
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        scheduleDailyWorker()
                    }
                } else {
                    scheduleDailyWorker()
                }
            } else {
                WorkManager.getInstance(requireContext()).cancelUniqueWork("DailyProfitLossWorker")
            }
        }
    }

    private fun scheduleDailyWorker() {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyProfitLossWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "DailyProfitLossWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
