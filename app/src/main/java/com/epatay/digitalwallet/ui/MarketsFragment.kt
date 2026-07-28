package com.epatay.digitalwallet.ui

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyConversionRate
import com.epatay.digitalwallet.data.CurrencyConversionRequest
import com.epatay.digitalwallet.data.CurrencyConverter
import com.epatay.digitalwallet.data.CurrencyRateKind
import com.epatay.digitalwallet.data.DecimalInputResult
import com.epatay.digitalwallet.data.DecimalInputValidator
import com.epatay.digitalwallet.databinding.BottomSheetCurrencyConverterBinding
import com.epatay.digitalwallet.databinding.FragmentMarketsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

class MarketsFragment : Fragment(R.layout.fragment_markets) {

    private var _binding: FragmentMarketsBinding? = null
    private val binding get() = _binding!!

    private val currencyViewModel:
        CurrencyViewModel by viewModels()

    private var conversionRates:
        List<CurrencyConversionRate> = emptyList()

    private val activeDialogs =
        linkedSetOf<Dialog>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        _binding =
            FragmentMarketsBinding.bind(view)

        binding.marketsPager.adapter =
            object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = 2

                override fun createFragment(position: Int): Fragment =
                    if (position == 0) CurrencyFragment() else GoldFragment()
            }
        binding.marketsPager.offscreenPageLimit = 2
        binding.marketsPager.isUserInputEnabled = false

        TabLayoutMediator(
            binding.marketsTabs,
            binding.marketsPager
        ) { tab, position ->
            tab.text = getString(
                if (position == 0) R.string.market_currency_tab
                else R.string.market_gold_tab
            )
        }.attach()

        binding.fabCurrencyConverter.setOnClickListener {
            showCurrencyConverter()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                currencyViewModel.uiState.collect { state ->
                    conversionRates =
                        state.ratesForConversion()
                }
            }
        }
    }

    private fun showCurrencyConverter() {
        val rates =
            conversionRates.ifEmpty {
                currencyViewModel.uiState.value
                    .ratesForConversion()
            }
                .takeIf { it.size > 1 }

        if (rates == null) {
            Snackbar.make(
                binding.root,
                R.string.currency_converter_no_rates,
                Snackbar.LENGTH_SHORT
            ).show()
            currencyViewModel.loadRates()
            return
        }

        val dialog =
            trackDialog(
                BottomSheetDialog(requireContext())
            )
        val sheet =
            BottomSheetCurrencyConverterBinding.inflate(layoutInflater)

        dialog.setContentView(sheet.root)
        dialog.behavior.state =
            BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed =
            true
        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        val options =
            rates.map(CurrencyOption::fromRate)
        val optionByDisplay =
            options.associateBy(CurrencyOption::displayName)
        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options.map(CurrencyOption::displayName)
            )

        sheet.etConverterAmount.keyListener =
            DigitsKeyListener.getInstance("0123456789,.")
        sheet.etConverterAmount.filters =
            arrayOf(InputFilter.LengthFilter(24))

        listOf(
            sheet.etFromCurrency,
            sheet.etToCurrency
        ).forEach { field ->
            field.setAdapter(adapter)
            field.inputType =
                android.text.InputType.TYPE_NULL
            field.keyListener = null
            field.showSoftInputOnFocus = false
            field.isCursorVisible = false
            field.setOnClickListener {
                hideKeyboard(sheet.etConverterAmount)
                field.showDropDown()
            }
            field.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    hideKeyboard(sheet.etConverterAmount)
                    field.showDropDown()
                }
            }
        }

        val defaultFrom =
            options.firstOrNull { it.code == "USD" }
                ?: options.first()
        val defaultTo =
            options.firstOrNull {
                it.code == CurrencyConversionRate.TRY_CODE
            }
                ?: options.first()

        sheet.etFromCurrency.setText(
            defaultFrom.displayName,
            false
        )
        sheet.etToCurrency.setText(
            defaultTo.displayName,
            false
        )

        fun selectedOption(displayName: String): CurrencyOption? =
            optionByDisplay[displayName.trim()]

        fun recalculate() {
            sheet.layoutAmount.error = null
            sheet.layoutFromCurrency.error = null
            sheet.layoutToCurrency.error = null

            val amountResult =
                DecimalInputValidator.positiveQuantity(
                    rawValue = sheet.etConverterAmount.text,
                    fieldName = "Tutar"
                )

            if (amountResult is DecimalInputResult.Invalid) {
                sheet.layoutAmount.error =
                    amountResult.message
                sheet.tvConverterResult.text = ""
                sheet.tvConverterRateInfo.text =
                    getString(
                        R.string.currency_converter_empty_result
                    )
                return
            }

            val from =
                selectedOption(
                    sheet.etFromCurrency.text
                        ?.toString()
                        .orEmpty()
                )
            val to =
                selectedOption(
                    sheet.etToCurrency.text
                        ?.toString()
                        .orEmpty()
                )

            if (from == null) {
                sheet.layoutFromCurrency.error =
                    "Listeden para birimi seçin"
                return
            }

            if (to == null) {
                sheet.layoutToCurrency.error =
                    "Listeden para birimi seçin"
                return
            }

            if (from.code == to.code) {
                sheet.layoutToCurrency.error =
                    "Kaynak ve hedef farklı olmalı"
                sheet.tvConverterResult.text = ""
                sheet.tvConverterRateInfo.text =
                    "Çapraz dönüşüm için farklı para birimleri seçin."
                return
            }

            val rateKind =
                if (
                    sheet.rateKindToggle.checkedButtonId ==
                    R.id.btnBuyingRate
                ) {
                    CurrencyRateKind.BUYING
                } else {
                    CurrencyRateKind.SELLING
                }

            val amount =
                (amountResult as DecimalInputResult.Valid)
                    .value

            val result =
                CurrencyConverter.convert(
                    request =
                        CurrencyConversionRequest(
                            amount = amount,
                            fromCode = from.code,
                            toCode = to.code,
                            rateKind = rateKind
                        ),
                    rates = rates
                )

            if (result == null) {
                sheet.tvConverterResult.text = ""
                sheet.tvConverterRateInfo.text =
                    "Seçilen para birimi için kur bulunamadı."
                return
            }

            sheet.tvConverterResult.text =
                "${formatDecimal(result.targetAmount)} ${result.toCode}"
            sheet.tvConverterRateInfo.text =
                conversionInfo(
                    from = from.code,
                    to = to.code,
                    rateKind = rateKind,
                    rates = rates
                )
        }

        val watcher =
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) = Unit

                override fun afterTextChanged(
                    s: Editable?
                ) {
                    recalculate()
                }
            }

        sheet.etConverterAmount.addTextChangedListener(watcher)
        sheet.etFromCurrency.setOnItemClickListener { _, _, _, _ ->
            hideKeyboard(sheet.etConverterAmount)
            recalculate()
        }
        sheet.etToCurrency.setOnItemClickListener { _, _, _, _ ->
            hideKeyboard(sheet.etConverterAmount)
            recalculate()
        }
        sheet.rateKindToggle.addOnButtonCheckedListener { _, _, checked ->
            if (checked) {
                recalculate()
            }
        }

        dialog.show()
    }

    private fun hideKeyboard(
        view: View
    ) {
        requireContext()
            .getSystemService<InputMethodManager>()
            ?.hideSoftInputFromWindow(
                view.windowToken,
                0
            )
        view.clearFocus()
    }

    private fun CurrencyUiState.ratesForConversion():
        List<CurrencyConversionRate> =
        when (this) {
            is CurrencyUiState.Success ->
                CurrencyConverter.buildRates(rates)

            is CurrencyUiState.ShowingCachedData ->
                CurrencyConverter.buildRates(rates)

            else ->
                emptyList()
        }

    private fun conversionInfo(
        from: String,
        to: String,
        rateKind: CurrencyRateKind,
        rates: List<CurrencyConversionRate>
    ): String {
        val unitResult =
            CurrencyConverter.convert(
                request =
                    CurrencyConversionRequest(
                        amount = BigDecimal.ONE,
                        fromCode = from,
                        toCode = to,
                        rateKind = rateKind
                    ),
                rates = rates
            )

        val rateName =
            if (rateKind == CurrencyRateKind.BUYING) {
                "Alış"
            } else {
                "Satış"
            }

        return if (unitResult == null) {
            "$rateName kuru ile hesaplanıyor."
        } else {
            "$rateName kuru • 1 $from = " +
                "${formatDecimal(unitResult.targetAmount)} $to"
        }
    }

    private fun formatDecimal(
        value: BigDecimal
    ): String {
        val normalized =
            value.stripTrailingZeros()

        return NumberFormat
            .getNumberInstance(TR_LOCALE)
            .apply {
                minimumFractionDigits =
                    if (normalized.scale() <= 0) 0 else 2
                maximumFractionDigits = 6
            }
            .format(normalized)
    }

    private fun <T : Dialog> trackDialog(dialog: T): T {
        activeDialogs += dialog
        dialog.setOnDismissListener {
            activeDialogs -= dialog
        }
        return dialog
    }

    override fun onDestroyView() {
        activeDialogs.toList().forEach { dialog ->
            runCatching { dialog.dismiss() }
        }
        activeDialogs.clear()
        binding.marketsPager.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private data class CurrencyOption(
        val code: String,
        val displayName: String
    ) {
        companion object {
            fun fromRate(rate: CurrencyConversionRate): CurrencyOption {
                val name =
                    rate.name.ifBlank { rate.code }

                return CurrencyOption(
                    code = rate.code,
                    displayName = "${rate.code} - $name"
                )
            }
        }
    }

    private companion object {
        val TR_LOCALE: Locale =
            Locale.forLanguageTag("tr-TR")
    }
}
