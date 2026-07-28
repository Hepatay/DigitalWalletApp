package com.epatay.digitalwallet.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.epatay.digitalwallet.data.DecimalInputResult
import com.epatay.digitalwallet.data.DecimalInputValidator
import com.epatay.digitalwallet.data.SavingsGoal
import com.epatay.digitalwallet.data.SavingsGoalProgress
import com.epatay.digitalwallet.data.TransactionDateUtils
import com.epatay.digitalwallet.databinding.BottomSheetAddSavingsEntryBinding
import com.epatay.digitalwallet.databinding.BottomSheetEditSavingsGoalBinding
import com.epatay.digitalwallet.databinding.BottomSheetSavingsGoalsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class SavingsGoalsBottomSheetDialogFragment :
    BottomSheetDialogFragment() {

    private var _binding:
        BottomSheetSavingsGoalsBinding? = null

    private val binding:
        BottomSheetSavingsGoalsBinding
        get() = requireNotNull(_binding)

    private val savingsGoalViewModel:
        SavingsGoalViewModel by activityViewModels()

    private val childDialogs =
        linkedSetOf<Dialog>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            BottomSheetSavingsGoalsBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        binding.btnAddSavingsGoal.setOnClickListener {
            showGoalForm()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                savingsGoalViewModel
                    .goalsWithProgress
                    .collectLatest(::renderGoals)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        (dialog as? BottomSheetDialog)
            ?.behavior
            ?.apply {
                state =
                    BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
    }

    private fun renderGoals(
        goals: List<SavingsGoalProgress>
    ) {
        binding.llSavingsGoals.removeAllViews()

        binding.tvSavingsGoalsEmpty.visibility =
            if (goals.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.llSavingsGoals.visibility =
            if (goals.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }

        goals.forEach { progress ->
            binding.llSavingsGoals.addView(
                createGoalCard(progress)
            )
        }
    }

    private fun createGoalCard(
        progress: SavingsGoalProgress
    ): View {
        val context = requireContext()
        val goal = progress.goal

        val card =
            MaterialCardView(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(12)
                    }

                radius = dp(16).toFloat()
                cardElevation = dp(1).toFloat()
                strokeWidth = dp(1)
                strokeColor =
                    color(
                        com.google.android.material.R.attr
                            .colorOutlineVariant,
                        Color.LTGRAY
                    )
                setCardBackgroundColor(
                    color(
                        com.google.android.material.R.attr
                            .colorSurface,
                        Color.WHITE
                    )
                )
            }

        val content =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(16),
                    dp(14),
                    dp(16),
                    dp(14)
                )
            }

        val title =
            TextView(context).apply {
                text =
                    if (goal.isArchived) {
                        "${goal.title} • Arşivlendi"
                    } else {
                        goal.title
                    }
                textSize = 17f
                setTextColor(
                    color(
                        com.google.android.material.R.attr
                            .colorOnSurface,
                        Color.BLACK
                    )
                )
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }

        val amounts =
            TextView(context).apply {
                text =
                    "${formatCurrency(progress.savedAmount)} / " +
                        formatCurrency(goal.targetAmount)
                textSize = 15f
                setTextColor(
                    color(
                        com.google.android.material.R.attr
                            .colorOnSurface,
                        Color.BLACK
                    )
                )
                setPadding(
                    0,
                    dp(7),
                    0,
                    0
                )
            }

        val status =
            TextView(context).apply {
                text =
                    if (progress.isCompleted) {
                        "Tamamlandı • %${progress.progressPercent}"
                    } else {
                        "Kalan: ${
                            formatCurrency(
                                progress.remainingAmount
                            )
                        } • %${progress.progressPercent}"
                    }
                textSize = 13f
                setTextColor(
                    color(
                        if (progress.isCompleted) {
                            com.google.android.material.R.attr
                                .colorPrimary
                        } else {
                            com.google.android.material.R.attr
                                .colorOnSurfaceVariant
                        },
                        if (progress.isCompleted) {
                            Color.rgb(46, 125, 50)
                        } else {
                            Color.DKGRAY
                        }
                    )
                )
                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }

        val progressBar =
            LinearProgressIndicator(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(8)
                    ).apply {
                        topMargin = dp(10)
                    }
                max = 100
                trackThickness = dp(8)
                trackCornerRadius = dp(4)
                setIndicatorColor(
                    color(
                        com.google.android.material.R.attr
                            .colorPrimary,
                        Color.rgb(46, 125, 50)
                    )
                )
                setProgressCompat(
                    progress.progressBarPercent,
                    false
                )
                contentDescription =
                    "Birikim ilerlemesi yüzde " +
                        progress.progressPercent
            }

        val targetDate =
            TextView(context).apply {
                text =
                    goal.targetDateKey
                        ?.takeIf(
                            TransactionDateUtils::isValidDateKey
                        )
                        ?.let { dateKey ->
                            "Hedef tarihi: ${
                                dateKey.toDisplayDate()
                            }"
                        }
                        ?: "Hedef tarihi: Belirlenmedi"
                textSize = 12f
                setTextColor(
                    color(
                        com.google.android.material.R.attr
                            .colorOnSurfaceVariant,
                        Color.DKGRAY
                    )
                )
                setPadding(
                    0,
                    dp(8),
                    0,
                    0
                )
            }

        val actions =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity =
                    android.view.Gravity.CENTER_VERTICAL
                setPadding(
                    0,
                    dp(12),
                    0,
                    0
                )
            }

        val addButton =
            createActionButton(
                text = "Ekle",
                startMargin = 0
            ) {
                showMovementForm(
                    progress = progress,
                    movementType =
                        MovementType.DEPOSIT
                )
            }

        val withdrawButton =
            createActionButton(
                text = "Çek",
                startMargin = dp(6)
            ) {
                showMovementForm(
                    progress = progress,
                    movementType =
                        MovementType.WITHDRAW
                )
            }.apply {
                isEnabled =
                    !goal.isArchived &&
                        progress.savedAmount >
                        BALANCE_EPSILON
            }

        val editButton =
            createActionButton(
                text = "Düzenle",
                startMargin = dp(6)
            ) {
                showGoalForm(goal)
            }

        addButton.isEnabled = !goal.isArchived

        actions.addView(addButton)
        actions.addView(withdrawButton)
        actions.addView(editButton)

        content.addView(title)
        content.addView(amounts)
        content.addView(status)
        content.addView(progressBar)
        content.addView(targetDate)
        content.addView(actions)
        card.addView(content)

        return card
    }

    private fun createActionButton(
        text: String,
        startMargin: Int,
        onClick: () -> Unit
    ): MaterialButton {
        return MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr
                .materialButtonOutlinedStyle
        ).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f
                ).apply {
                    marginStart = startMargin
                }
            this.text = text
            isAllCaps = false
            minHeight = dp(48)
            minimumHeight = dp(48)
            minimumWidth = 0
            textSize = 13f
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun showGoalForm(
        goalToEdit: SavingsGoal? = null
    ) {
        val dialogContext =
            context ?: return
        val viewModel =
            savingsGoalViewModel
        val dialog =
            trackChildDialog(
                BottomSheetDialog(dialogContext)
            )
        val form =
            BottomSheetEditSavingsGoalBinding.inflate(
                LayoutInflater.from(dialogContext)
            )

        dialog.setContentView(form.root)
        expand(dialog)

        var selectedTargetDateKey =
            goalToEdit
                ?.targetDateKey
                ?.takeIf(
                    TransactionDateUtils::isValidDateKey
                )

        fun updateTargetDateField() {
            form.etSavingsGoalTargetDate.setText(
                selectedTargetDateKey
                    ?.toDisplayDate()
                    .orEmpty()
            )
        }

        form.layoutSavingsGoalTargetDate.apply {
            endIconMode =
                TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(
                android.R.drawable
                    .ic_menu_close_clear_cancel
            )
            endIconContentDescription =
                "Hedef tarihini temizle"
            setEndIconOnClickListener {
                selectedTargetDateKey = null
                error = null
                updateTargetDateField()
            }
        }

        form.etSavingsGoalTargetDate
            .setOnClickListener {
                showDatePicker(
                    context = dialogContext,
                    title = "Hedef tarihi",
                    currentDateKey =
                        selectedTargetDateKey
                ) { dateKey ->
                    selectedTargetDateKey = dateKey
                    form.layoutSavingsGoalTargetDate
                        .error = null
                    updateTargetDateField()
                }
            }

        if (goalToEdit == null) {
            form.tvSavingsGoalFormTitle.text =
                "Birikim Hedefi Ekle"
            form.btnSaveSavingsGoal.text =
                "Hedefi Kaydet"
            form.btnDeleteSavingsGoal.visibility =
                View.GONE
        } else {
            form.tvSavingsGoalFormTitle.text =
                "Birikim Hedefini Düzenle"
            form.btnSaveSavingsGoal.text =
                "Değişiklikleri Kaydet"
            form.btnDeleteSavingsGoal.visibility =
                View.VISIBLE

            form.etSavingsGoalTitle.setText(
                goalToEdit.title
            )
            form.etSavingsGoalTargetAmount.setText(
                goalToEdit.targetAmount.toPlainAmount()
            )
        }

        updateTargetDateField()

        form.btnDeleteSavingsGoal
            .setOnClickListener {
                val goal =
                    goalToEdit
                        ?: return@setOnClickListener

                val confirmationDialog =
                    MaterialAlertDialogBuilder(
                        dialogContext
                    )
                    .setTitle("Birikim hedefini sil")
                    .setMessage(
                        "\"${goal.title}\" hedefi ve tüm " +
                            "birikim hareketleri kalıcı olarak " +
                            "silinecek. Devam edilsin mi?"
                    )
                    .setNegativeButton(
                        "Vazgeç",
                        null
                    )
                    .setPositiveButton(
                        "Sil"
                    ) { _, _ ->
                        viewModel
                            .deleteGoal(goal)

                        showInAppMessage(
                            "Birikim hedefi silindi"
                        )

                        dialog.dismiss()
                    }
                    .create()

                trackChildDialog(
                    confirmationDialog
                ).show()
            }

        form.btnSaveSavingsGoal
            .setOnClickListener {
                val title =
                    form.etSavingsGoalTitle.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val targetAmountResult =
                    DecimalInputValidator.positiveMoney(
                        rawValue =
                            form.etSavingsGoalTargetAmount.text,
                        fieldName = "Hedef tutarı"
                    )

                if (title.isEmpty()) {
                    form.layoutSavingsGoalTitle.error =
                        "Hedef adı boş bırakılamaz."
                    return@setOnClickListener
                }

                form.layoutSavingsGoalTitle.error = null

                if (
                    targetAmountResult is
                        DecimalInputResult.Invalid
                ) {
                    form.layoutSavingsGoalTargetAmount.error =
                        targetAmountResult.message
                    return@setOnClickListener
                }

                val targetAmount =
                    (targetAmountResult as
                        DecimalInputResult.Valid)
                        .value
                        .toDouble()

                form.layoutSavingsGoalTargetAmount.error =
                    null

                if (goalToEdit == null) {
                    viewModel.createGoal(
                        title = title,
                        targetAmount = targetAmount,
                        targetDateKey =
                            selectedTargetDateKey
                    )
                } else {
                    viewModel.updateGoal(
                        goalToEdit.copy(
                            title = title,
                            targetAmount = targetAmount,
                            targetDateKey =
                                selectedTargetDateKey
                        )
                    )
                }

                showInAppMessage(
                    if (goalToEdit == null) {
                        "Birikim hedefi eklendi"
                    } else {
                        "Birikim hedefi güncellendi"
                    }
                )

                dialog.dismiss()
            }

        dialog.show()
    }

    private fun showMovementForm(
        progress: SavingsGoalProgress,
        movementType: MovementType
    ) {
        val dialogContext =
            context ?: return
        val viewModel =
            savingsGoalViewModel
        val dialog =
            trackChildDialog(
                BottomSheetDialog(dialogContext)
            )
        val form =
            BottomSheetAddSavingsEntryBinding.inflate(
                LayoutInflater.from(dialogContext)
            )

        dialog.setContentView(form.root)
        expand(dialog)

        form.tvSavingsEntryGoalName.text =
            "${progress.goal.title} • Mevcut: ${
                formatCurrency(progress.savedAmount)
            }"

        form.rgSavingsEntryType.check(
            when (movementType) {
                MovementType.DEPOSIT ->
                    form.rbSavingsDeposit.id

                MovementType.WITHDRAW ->
                    form.rbSavingsWithdraw.id
            }
        )

        fun updateMovementLabels() {
            val isWithdrawal =
                form.rgSavingsEntryType
                    .checkedRadioButtonId ==
                    form.rbSavingsWithdraw.id

            form.tvSavingsEntryFormTitle.text =
                if (isWithdrawal) {
                    "Birikimden Para Çek"
                } else {
                    "Birikime Para Ekle"
                }

            form.btnSaveSavingsEntry.text =
                if (isWithdrawal) {
                    "Çekimi Kaydet"
                } else {
                    "Biriktir"
                }

            form.layoutSavingsEntryAmount.error = null
        }

        form.rgSavingsEntryType
            .setOnCheckedChangeListener { _, _ ->
                updateMovementLabels()
            }

        updateMovementLabels()

        form.btnSaveSavingsEntry
            .setOnClickListener {
                val amountResult =
                    DecimalInputValidator.positiveMoney(
                        rawValue =
                            form.etSavingsEntryAmount.text,
                        fieldName = "Tutar"
                    )

                if (amountResult is DecimalInputResult.Invalid) {
                    form.layoutSavingsEntryAmount.error =
                        amountResult.message
                    return@setOnClickListener
                }

                val amount =
                    (amountResult as DecimalInputResult.Valid)
                        .value
                        .toDouble()

                val isWithdrawal =
                    form.rgSavingsEntryType
                        .checkedRadioButtonId ==
                        form.rbSavingsWithdraw.id

                val currentProgress =
                    viewModel
                        .goalsWithProgress
                        .value
                        .firstOrNull { current ->
                            current.goal.id ==
                                progress.goal.id
                        }
                        ?: progress

                if (
                    isWithdrawal &&
                    amount >
                    currentProgress.savedAmount +
                        BALANCE_EPSILON
                ) {
                    form.layoutSavingsEntryAmount.error =
                        "Mevcut birikimden fazlasını " +
                            "çekemezsiniz. En fazla ${
                                formatCurrency(
                                    currentProgress.savedAmount
                                )
                            } çekilebilir."
                    return@setOnClickListener
                }

                form.layoutSavingsEntryAmount.error = null

                val note =
                    form.etSavingsEntryNote.text
                        ?.toString()
                        ?.trim()
                        ?.takeIf(String::isNotEmpty)

                viewModel.addEntry(
                    goalId = progress.goal.id,
                    amountDelta =
                        if (isWithdrawal) {
                            -amount
                        } else {
                            amount
                        },
                    note = note
                )

                showInAppMessage(
                    if (isWithdrawal) {
                        "Birikimden para çekildi"
                    } else {
                        "Birikime para eklendi"
                    }
                )

                dialog.dismiss()
            }

        dialog.show()
    }

    private fun showDatePicker(
        context: Context,
        title: String,
        currentDateKey: Int?,
        onDateSelected: (Int) -> Unit
    ) {
        val initialDate =
            currentDateKey
                ?.takeIf(
                    TransactionDateUtils::isValidDateKey
                )
                ?.toCalendar()
                ?: Calendar.getInstance()

        val datePickerDialog =
            DatePickerDialog(
                context,
            {
                    _,
                    year,
                    month,
                    dayOfMonth ->

                val selectedDate =
                    GregorianCalendar().apply {
                        isLenient = false
                        clear()
                        set(
                            year,
                            month,
                            dayOfMonth
                        )
                    }

                onDateSelected(
                    TransactionDateUtils.currentDateKey(
                        selectedDate
                    )
                )
            },
            initialDate.get(Calendar.YEAR),
            initialDate.get(Calendar.MONTH),
            initialDate.get(Calendar.DAY_OF_MONTH)
            ).apply {
            setTitle(title)
        }

        trackChildDialog(
            datePickerDialog
        ).show()
    }

    private fun expand(
        dialog: BottomSheetDialog
    ) {
        dialog.behavior.apply {
            state =
                BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun formatCurrency(
        amount: Double
    ): String {
        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%,.2f ₺",
            amount.coerceAtLeast(0.0)
        )
    }

    private fun Double.toPlainAmount(): String {
        return java.math.BigDecimal
            .valueOf(this)
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun Int.toDisplayDate(): String {
        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%02d.%02d.%04d",
            this % 100,
            this / 100 % 100,
            this / 10_000
        )
    }

    private fun Int.toCalendar(): Calendar {
        return GregorianCalendar(
            this / 10_000,
            this / 100 % 100 - 1,
            this % 100
        )
    }

    private fun color(
        attribute: Int,
        fallback: Int
    ): Int {
        return MaterialColors.getColor(
            binding.root,
            attribute,
            fallback
        )
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    private fun showInAppMessage(
        message: String
    ) {
        val root =
            _binding
                ?.root
                ?: return

        Snackbar
            .make(
                root,
                message,
                Snackbar.LENGTH_SHORT
            )
            .show()
    }

    private fun <T : Dialog> trackChildDialog(
        childDialog: T
    ): T {
        childDialogs.add(childDialog)

        childDialog.setOnDismissListener {
            childDialogs.remove(childDialog)
        }

        return childDialog
    }

    private fun dismissChildDialogs() {
        val dialogsToDismiss =
            childDialogs.toList()

        childDialogs.clear()

        dialogsToDismiss.forEach { childDialog ->
            runCatching {
                childDialog.dismiss()
            }
        }
    }

    override fun onDismiss(
        dialog: DialogInterface
    ) {
        dismissChildDialogs()
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        dismissChildDialogs()
        _binding = null
        super.onDestroyView()
    }

    private enum class MovementType {
        DEPOSIT,
        WITHDRAW
    }

    private companion object {
        const val BALANCE_EPSILON = 0.000_001
    }
}
