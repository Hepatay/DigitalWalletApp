package com.epatay.digitalwallet.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyManager
import com.epatay.digitalwallet.data.InvestmentItem
import com.epatay.digitalwallet.databinding.BottomSheetAddInvestmentBinding
import com.epatay.digitalwallet.databinding.FragmentAnalysisBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisFragment : Fragment(R.layout.fragment_analysis) {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    private val investmentViewModel: InvestmentViewModel by activityViewModels()

    private lateinit var adapter: InvestmentAdapter

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding =
            FragmentAnalysisBinding.bind(view)

        adapter = InvestmentAdapter(

            onEditClick = { investment ->
                showEditPriceDialog(investment)
            },

            onDeleteClick = { investment ->
                showDeleteInvestmentDialog(investment)
            }
        )

        binding.rvInvestments.adapter = adapter

        binding.rvInvestments.layoutManager =
            LinearLayoutManager(requireContext())

        investmentViewModel.allInvestments.observe(
            viewLifecycleOwner
        ) { investments ->

            adapter.setData(investments)

            calculateGrandTotal(investments)
        }

        binding.fabAddInvestment.setOnClickListener {
            showAddInvestmentBottomSheet()
        }
    }

    private fun showEditPriceDialog(
        investment: InvestmentItem
    ) {

        val isGramGold =
            investment.assetName.equals(
                "GRAM ALTIN",
                ignoreCase = true
            )

        val unitName =
            if (isGramGold) {
                "Gram miktarı"
            } else {
                "Adet"
            }

        val padding =
            (20 * resources.displayMetrics.density)
                .toInt()

        val container =
            LinearLayout(requireContext()).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    padding,
                    padding / 2,
                    padding,
                    0
                )
            }

        val amountLabel =
            TextView(requireContext()).apply {

                text = unitName
                textSize = 14f

                setPadding(
                    0,
                    padding / 2,
                    0,
                    4
                )
            }

        val amountEditText =
            EditText(requireContext()).apply {

                hint = unitName

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                setText(
                    investment.amount.toString()
                )

                setSelection(
                    text?.length ?: 0
                )
            }

        val priceLabel =
            TextView(requireContext()).apply {

                text = "Birim alış fiyatı"
                textSize = 14f

                setPadding(
                    0,
                    padding / 2,
                    0,
                    4
                )
            }

        val priceEditText =
            EditText(requireContext()).apply {

                hint = "Birim alış fiyatı"

                inputType =
                    InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_DECIMAL

                setText(
                    investment.buyPrice.toString()
                )

                setSelection(
                    text?.length ?: 0
                )
            }

        container.addView(amountLabel)
        container.addView(amountEditText)
        container.addView(priceLabel)
        container.addView(priceEditText)

        val dialog =
            AlertDialog.Builder(requireContext())
                .setTitle(
                    "${investment.assetName} Yatırımını Düzenle"
                )
                .setView(container)
                .setNegativeButton(
                    "İptal",
                    null
                )
                .setPositiveButton(
                    "Güncelle",
                    null
                )
                .create()

        /*
         * setPositiveButton içindeki standart işlem,
         * geçersiz girişte bile pencereyi kapatır.
         * Bu nedenle tıklama olayını pencere açıldıktan
         * sonra ayrıca tanımlıyoruz.
         */
        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val newAmount =
                    amountEditText.text
                        ?.toString()
                        ?.trim()
                        ?.toIntOrNull()

                val newPrice =
                    priceEditText.text
                        ?.toString()
                        ?.trim()
                        ?.replace(",", ".")
                        ?.toDoubleOrNull()

                if (
                    newAmount == null ||
                    newAmount <= 0
                ) {

                    amountEditText.error =
                        if (isGramGold) {
                            "Gram miktarı pozitif tam sayı olmalıdır"
                        } else {
                            "Adet pozitif tam sayı olmalıdır"
                        }

                    amountEditText.requestFocus()

                    return@setOnClickListener
                }

                amountEditText.error = null

                if (
                    newPrice == null ||
                    !newPrice.isFinite() ||
                    newPrice <= 0.0
                ) {

                    priceEditText.error =
                        "Geçerli bir alış fiyatı girin"

                    priceEditText.requestFocus()

                    return@setOnClickListener
                }

                priceEditText.error = null

                val updatedInvestment =
                    investment.copy(
                        amount = newAmount,
                        buyPrice = newPrice
                    )

                /*
                 * Mevcut yatırım türü ve alış tarihi korunur.
                 * Yalnızca miktar ve alış fiyatı değiştirilir.
                 */
                investmentViewModel.update(
                    updatedInvestment
                )

                val amountText =
                    if (isGramGold) {
                        "$newAmount gram"
                    } else {
                        "$newAmount adet"
                    }

                Toast.makeText(
                    requireContext(),
                    "${investment.assetName} yatırımı " +
                            "$amountText olarak güncellendi",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showDeleteInvestmentDialog(
        investment: InvestmentItem
    ) {
        val isGramGold =
            investment.assetName.equals(
                "GRAM ALTIN",
                ignoreCase = true
            )

        val amountText =
            if (isGramGold) {
                "${investment.amount} gram"
            } else {
                "${investment.amount} adet"
            }

        AlertDialog.Builder(requireContext())
            .setTitle("Yatırımı sil")
            .setMessage(
                "\"${investment.assetName}\" adlı " +
                        "$amountText yatırım kaydını " +
                        "silmek istediğinizden emin misiniz?"
            )
            .setNegativeButton(
                "İptal",
                null
            )
            .setPositiveButton(
                "Sil"
            ) { _, _ ->

                investmentViewModel.delete(
                    investment
                )

                Toast.makeText(
                    requireContext(),
                    "${investment.assetName} kaydı silindi",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun calculateGrandTotal(
        investments: List<InvestmentItem>
    ) {
        val currencyManager =
            CurrencyManager(requireContext())

        val rates =
            currencyManager
                .getSavedRates()
                ?.conversion_rates

        var grandTotal = 0.0
        var totalCost = 0.0

        val assetTotalsMap =
            mutableMapOf<String, Float>()

        for (item in investments) {

            val assetName =
                item.assetName
                    .trim()
                    .uppercase(Locale.ROOT)

            /*
             * Gram altın fiyatı ayrı altın API'sinden
             * hesaplanıp CurrencyManager içine kaydediliyor.
             *
             * Dövizler ise TRY bazlı kur listesinden
             * 1 / rawRate şeklinde hesaplanıyor.
             */
            val currentRate =
                getCurrentAssetRate(
                    assetName = assetName,
                    savedBuyPrice = item.buyPrice,
                    currencyManager = currencyManager,
                    rates = rates
                )

            val currentValue =
                item.amount * currentRate

            val buyCost =
                item.amount * item.buyPrice

            grandTotal += currentValue
            totalCost += buyCost

            val oldTotal =
                assetTotalsMap[assetName] ?: 0f

            assetTotalsMap[assetName] =
                oldTotal + currentValue.toFloat()
        }

        binding.tvTotalInvestmentAmount.text =
            formatCurrency(grandTotal)

        val totalProfit =
            grandTotal - totalCost

        when {

            totalProfit > 0.0 -> {

                binding.tvTotalProfitLoss.text =
                    "+${formatCurrency(totalProfit)} Kâr"

                binding.tvTotalProfitLoss.setTextColor(
                    Color.parseColor("#4CAF50")
                )
            }

            totalProfit < 0.0 -> {

                binding.tvTotalProfitLoss.text =
                    "${formatCurrency(totalProfit)} Zarar"

                binding.tvTotalProfitLoss.setTextColor(
                    Color.parseColor("#F44336")
                )
            }

            else -> {

                binding.tvTotalProfitLoss.text =
                    "0,00 ₺"

                binding.tvTotalProfitLoss.setTextColor(
                    Color.parseColor("#888888")
                )
            }
        }

        setupPieChart(assetTotalsMap)
    }

    private fun getCurrentAssetRate(
        assetName: String,
        savedBuyPrice: Double,
        currencyManager: CurrencyManager,
        rates: Map<String, Double>?
    ): Double {

        if (assetName == "GRAM ALTIN") {

            val savedGramGoldPrice =
                currencyManager
                    .getSavedGramGoldPrice()

            return if (
                savedGramGoldPrice != null &&
                savedGramGoldPrice.isFinite() &&
                savedGramGoldPrice > 0.0
            ) {
                savedGramGoldPrice
            } else {
                savedBuyPrice
            }
        }

        val rawRate =
            rates?.get(assetName)

        return if (
            rawRate != null &&
            rawRate.isFinite() &&
            rawRate > 0.0
        ) {
            1.0 / rawRate
        } else {
            savedBuyPrice
        }
    }

    private fun setupPieChart(
        assetTotalsMap: Map<String, Float>
    ) {
        val pieChart =
            binding.pieChart

        pieChart.clear()

        binding.llInvestmentDetails
            .removeAllViews()

        val surfaceColor =
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorSurface,
                Color.WHITE
            )

        val onSurfaceColor =
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnSurface,
                Color.BLACK
            )

        val onSurfaceVariantColor =
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Color.DKGRAY
            )

        pieChart.setNoDataText(
            "Henüz yatırım bulunmuyor"
        )

        pieChart.setNoDataTextColor(
            onSurfaceVariantColor
        )

        pieChart.setBackgroundColor(
            Color.TRANSPARENT
        )

        if (assetTotalsMap.isEmpty()) {
            pieChart.invalidate()
            return
        }

        val sortedAssets =
            assetTotalsMap
                .toList()
                .filter { (_, totalValue) ->
                    totalValue > 0f
                }
                .sortedByDescending { (_, totalValue) ->
                    totalValue
                }

        if (sortedAssets.isEmpty()) {
            pieChart.invalidate()
            return
        }

        val entries =
            ArrayList<PieEntry>()

        val colors =
            ArrayList<Int>()

        val assetColors = mapOf(
            "USD" to Color.parseColor("#27AE60"),
            "EUR" to Color.parseColor("#2980B9"),
            "GBP" to Color.parseColor("#C0392B"),
            "JPY" to Color.parseColor("#8E44AD"),
            "AUD" to Color.parseColor("#16A085"),
            "CAD" to Color.parseColor("#D35400"),
            "CHF" to Color.parseColor("#607D8B"),
            "RUB" to Color.parseColor("#7F8C8D"),
            "CNY" to Color.parseColor("#F39C12"),
            "GRAM ALTIN" to Color.parseColor("#F1C40F")
        )

        val fallbackColors = listOf(
            Color.parseColor("#E74C3C"),
            Color.parseColor("#3498DB"),
            Color.parseColor("#2ECC71"),
            Color.parseColor("#F1C40F"),
            Color.parseColor("#9B59B6"),
            Color.parseColor("#1ABC9C")
        )

        var fallbackIndex = 0

        for ((assetName, totalValue) in sortedAssets) {

            entries.add(
                PieEntry(
                    totalValue,
                    assetName
                )
            )

            val currentColor =
                assetColors[assetName]
                    ?: fallbackColors[
                        fallbackIndex++ %
                                fallbackColors.size
                    ]

            colors.add(currentColor)

            val row =
                LinearLayout(requireContext()).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        android.view.Gravity.CENTER_VERTICAL

                    setPadding(
                        0,
                        6,
                        0,
                        6
                    )
                }

            val colorView =
                View(requireContext()).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            30,
                            30
                        ).apply {

                            setMargins(
                                0,
                                0,
                                16,
                                0
                            )
                        }

                    setBackgroundColor(
                        currentColor
                    )
                }

            val textView =
                TextView(requireContext()).apply {

                    text =
                        "$assetName  ${
                            formatCurrency(
                                totalValue.toDouble()
                            )
                        }"

                    textSize = 13f

                    setTextColor(
                        onSurfaceColor
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }

            row.addView(colorView)
            row.addView(textView)

            binding.llInvestmentDetails
                .addView(row)
        }

        val dataSet =
            PieDataSet(
                entries,
                ""
            ).apply {

                this.colors = colors

                sliceSpace = 3f
                selectionShift = 8f

                setDrawValues(false)

                valueTextColor =
                    onSurfaceColor

                valueTextSize = 11f
            }

        val pieData =
            PieData(dataSet).apply {

                setValueTextColor(
                    onSurfaceColor
                )
            }

        pieChart.apply {

            data = pieData

            description.isEnabled = false
            legend.isEnabled = false

            setDrawEntryLabels(false)

            setEntryLabelColor(
                onSurfaceColor
            )

            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 63f

            setHoleColor(
                surfaceColor
            )

            setTransparentCircleColor(
                surfaceColor
            )

            setTransparentCircleAlpha(100)

            setDrawCenterText(true)

            centerText = "Portföy"

            setCenterTextColor(
                onSurfaceColor
            )

            setCenterTextSize(13f)

            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            setUsePercentValues(false)

            animateY(
                800,
                com.github.mikephil.charting.animation
                    .Easing
                    .EaseInOutQuad
            )

            invalidate()
        }
    }

    private fun showAddInvestmentBottomSheet() {

        val bottomSheetDialog =
            BottomSheetDialog(requireContext())

        val dialogBinding =
            BottomSheetAddInvestmentBinding.inflate(
                layoutInflater
            )
        bottomSheetDialog.setContentView(
            dialogBinding.root)


        val bottomSheetSurfaceColor =
            MaterialColors.getColor(
                dialogBinding.root,
                com.google.android.material.R.attr.colorSurface,
                Color.WHITE
            )

        dialogBinding.root.setBackgroundColor(
            bottomSheetSurfaceColor
        )

        val assets = arrayOf(
            "USD",
            "EUR",
            "GBP",
            "CHF",
            "JPY",
            "CAD",
            "AUD",
            "RUB",
            "CNY",
            "GRAM ALTIN"
        )

        dialogBinding.etAssetType.apply {

            setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    assets
                )
            )

            inputType =
                InputType.TYPE_NULL

            showSoftInputOnFocus =
                false

            isCursorVisible =
                false

            setOnClickListener {
                showDropDown()
            }

            setOnFocusChangeListener { _, hasFocus ->

                if (hasFocus) {
                    showDropDown()
                }
            }

            setOnItemClickListener { _, _, position, _ ->

                val selectedAsset =
                    assets[position]

                dialogBinding.etInvestmentAmount.hint =
                    if (selectedAsset == "GRAM ALTIN") {
                        "Gram miktarı (Örnek: 1, 5, 10)"
                    } else {
                        "Satın alınan adet"
                    }
            }
        }

        dialogBinding.rgRateType
            .setOnCheckedChangeListener { _, checkedId ->

                dialogBinding.layoutManualRate.visibility =
                    if (checkedId == R.id.rbManualRate) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }

        dialogBinding.btnSaveInvestment
            .setOnClickListener {

                val selectedAsset =
                    dialogBinding.etAssetType.text
                        ?.toString()
                        ?.trim()
                        ?.uppercase(Locale.ROOT)
                        .orEmpty()

                val amountText =
                    dialogBinding.etInvestmentAmount.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val amount =
                    amountText.toIntOrNull()

                if (selectedAsset.isEmpty()) {

                    Toast.makeText(
                        requireContext(),
                        "Lütfen yatırım türünü seçin",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                if (amount == null || amount <= 0) {

                    val message =
                        if (selectedAsset == "GRAM ALTIN") {
                            "Gram miktarı pozitif tam sayı olmalıdır"
                        } else {
                            "Miktar pozitif tam sayı olmalıdır"
                        }

                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val isManualSelected =
                    dialogBinding.rbManualRate.isChecked

                var savedRate = 0.0

                if (isManualSelected) {

                    val manualRate =
                        dialogBinding.etManualRate.text
                            ?.toString()
                            ?.trim()
                            ?.replace(",", ".")
                            ?.toDoubleOrNull()

                    if (
                        manualRate == null ||
                        !manualRate.isFinite() ||
                        manualRate <= 0.0
                    ) {

                        Toast.makeText(
                            requireContext(),
                            "Geçerli bir alış kuru girin",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    savedRate =
                        manualRate

                } else {

                    val currencyManager =
                        CurrencyManager(requireContext())

                    if (selectedAsset == "GRAM ALTIN") {

                        savedRate =
                            currencyManager
                                .getSavedGramGoldPrice()
                                ?: 0.0

                    } else {

                        val rates =
                            currencyManager
                                .getSavedRates()
                                ?.conversion_rates

                        val rawRate =
                            rates?.get(selectedAsset)

                        if (
                            rawRate != null &&
                            rawRate.isFinite() &&
                            rawRate > 0.0
                        ) {
                            savedRate =
                                1.0 / rawRate
                        }
                    }
                }

                if (
                    !savedRate.isFinite() ||
                    savedRate <= 0.0
                ) {

                    val errorMessage =
                        if (selectedAsset == "GRAM ALTIN") {
                            "Gram altın fiyatı bulunamadı. Önce Piyasalar sayfasında Güncelle butonuna basın."
                        } else {
                            "Kur verisi alınamadı. Önce Piyasalar sayfasında Güncelle butonuna basın."
                        }

                    Toast.makeText(
                        requireContext(),
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }

                val currentDate =
                    SimpleDateFormat(
                        "dd.MM.yyyy HH:mm",
                        Locale.getDefault()
                    ).format(Date())

                val newInvestment =
                    InvestmentItem(
                        assetName = selectedAsset,
                        amount = amount,
                        buyPrice = savedRate,
                        buyDate = currentDate
                    )

                investmentViewModel.insert(
                    newInvestment
                )

                val unitText =
                    if (selectedAsset == "GRAM ALTIN") {
                        "$amount gram"
                    } else {
                        "$amount adet"
                    }

                val rateTypeText =
                    if (isManualSelected) {
                        "manuel kurla"
                    } else {
                        "güncel kurla"
                    }

                Toast.makeText(
                    requireContext(),
                    "$unitText yatırım $rateTypeText eklendi",
                    Toast.LENGTH_SHORT
                ).show()

                bottomSheetDialog.dismiss()
            }

        bottomSheetDialog.show()
    }

    private fun formatCurrency(
        value: Double
    ): String {

        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%,.2f ₺",
            value
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}