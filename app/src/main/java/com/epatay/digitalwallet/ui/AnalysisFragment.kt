package com.epatay.digitalwallet.ui

import android.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.InputType
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyFlagProvider
import com.epatay.digitalwallet.data.DecimalInputResult
import com.epatay.digitalwallet.data.DecimalInputValidator
import com.epatay.digitalwallet.data.DecimalMath
import com.epatay.digitalwallet.data.GoldInputUnit
import com.epatay.digitalwallet.data.GoldType
import com.epatay.digitalwallet.data.TcmbXmlParser
import com.epatay.digitalwallet.databinding.BottomSheetAddInvestmentBinding
import com.epatay.digitalwallet.databinding.FragmentAnalysisBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.epatay.digitalwallet.util.setupMoneyInput
import com.epatay.digitalwallet.util.setupTextInput
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class AnalysisFragment : Fragment(R.layout.fragment_analysis) {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InvestmentViewModel by activityViewModels()
    private lateinit var adapter: InvestmentAdapter

    private val activeDialogs =
        linkedSetOf<Dialog>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalysisBinding.bind(view)



        adapter = InvestmentAdapter(
            onEditClick = ::showEditDialog,
            onDeleteClick = ::showDeleteDialog
        )
        binding.rvInvestments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInvestments.adapter = adapter

        viewModel.portfolioItems.observe(viewLifecycleOwner, ::renderPortfolio)
        viewModel.portfolioAssetCount.observe(viewLifecycleOwner) { /* keep LiveData active */ }
        viewModel.errorEvent.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrBlank()) {
                com.epatay.digitalwallet.util.InAppNotification.show(
                    activity,
                    errorMsg,
                    com.epatay.digitalwallet.util.NotificationType.WARNING
                )
            }
        }
        val onAddInvestmentClick = {
            val currentCount = viewModel.portfolioItems.value?.size ?: (viewModel.portfolioAssetCount.value ?: 0)
            if (currentCount >= 20) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Portföy Kapasitesi Doldu")
                    .setMessage("Portföyünüze en fazla 20 farklı varlık ekleyebilirsiniz. Yeni bir varlık eklemek için mevcut bir varlığı silebilirsiniz.")
                    .setPositiveButton("Tamam", null)
                    .show()
            } else {
                showAddBottomSheet()
            }
        }
        binding.fabAddInvestment.setOnClickListener { onAddInvestmentClick() }
        binding.btnAddFirstInvestment.setOnClickListener { onAddInvestmentClick() }

        binding.layoutToggleBreakdown.setOnClickListener {
            isGrandTotalExpanded = !isGrandTotalExpanded
            binding.layoutGrandTotalBreakdown.visibility =
                if (isGrandTotalExpanded) View.VISIBLE else View.GONE
            binding.ivExpandGrandTotal.rotation =
                if (isGrandTotalExpanded) 180f else 0f
            binding.tvToggleBreakdownTitle.text =
                if (isGrandTotalExpanded) "Varlık Dağılımını Gizle" else "Tüm Varlıkları ve Dağılımı Göster"
        }
    }

    private var isGrandTotalExpanded: Boolean = false

    private fun renderPortfolio(items: List<PortfolioAssetItem>) {
        adapter.setData(items)
        val isEmpty = items.isEmpty()
        binding.rvInvestments.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmptyInvestments.visibility =
            if (isEmpty) View.VISIBLE else View.GONE

        val valuedItems = items.filter { it.currentValue != null }
        val excludedCount = items.size - valuedItems.size
        val currentTotal =
            DecimalMath.sumMoney(
                valuedItems.mapNotNull(PortfolioAssetItem::currentValue)
            )
        val comparableItems = valuedItems.filter { it.totalPurchaseCost != null }
        val totalProfit =
            DecimalMath.sumMoney(
                comparableItems.mapNotNull(PortfolioAssetItem::profitLoss)
            )

        binding.tvTotalInvestmentAmount.text = formatCurrency(currentTotal)
        bindTotalProfit(totalProfit, comparableItems.isNotEmpty())
        binding.tvPortfolioWarning.text =
            "Güncel fiyatı bulunmayan $excludedCount varlık toplamın dışında tutuldu."
        binding.tvPortfolioWarning.visibility =
            if (excludedCount > 0) View.VISIBLE else View.GONE

        setupPieChart(valuedItems)
        setupDetailedBreakdown(valuedItems, currentTotal)
    }

    private fun bindTotalProfit(value: Double, hasComparableItems: Boolean) {
        if (!hasComparableItems) {
            binding.tvTotalProfitLoss.text = "Kâr/Zarar: -"
            binding.tvTotalProfitLoss.setTextColor(Color.parseColor("#888888"))
            return
        }
        when {
            abs(value) < 0.01 -> {
                binding.tvTotalProfitLoss.text = "Kâr/Zarar: 0,00 TL"
                binding.tvTotalProfitLoss.setTextColor(Color.parseColor("#888888"))
            }
            value > 0.0 -> {
                binding.tvTotalProfitLoss.text = "+${formatCurrency(value)} Kâr"
                binding.tvTotalProfitLoss.setTextColor(Color.parseColor("#2E7D32"))
            }
            else -> {
                binding.tvTotalProfitLoss.text =
                    "-${formatCurrency(abs(value))} Zarar"
                binding.tvTotalProfitLoss.setTextColor(Color.parseColor("#C62828"))
            }
        }
    }

    private fun showAddBottomSheet() {
        val dialog =
            trackDialog(
                BottomSheetDialog(requireContext())
            )
        val sheet = BottomSheetAddInvestmentBinding.inflate(layoutInflater)
        dialog.setContentView(sheet.root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        var isGold = false
        var selectedGoldType: GoldType? = null
        var selectedDate = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT)
        var availableAssetOptions: List<String> = emptyList()

        fun updateDateButton() {
            sheet.btnPurchaseDate.text =
                "Alış tarihi: ${dateFormatter.format(selectedDate.time)}"
        }

        fun configureAssets(gold: Boolean) {
            isGold = gold
            selectedGoldType = null
            sheet.etAssetType.setText("", false)
            val options =
                if (gold) {
                    GoldType.entries.map(GoldType::displayName)
                } else {
                    viewModel.currencyCodes.value
                        ?.takeIf(List<String>::isNotEmpty)
                        ?: TcmbXmlParser.currencyPriority
                }
            availableAssetOptions = options
            val adapter = object : ArrayAdapter<String>(
                requireContext(),
                R.layout.item_asset_dropdown,
                R.id.tvAssetDropdownCode,
                options
            ) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    return createCustomView(position, convertView, parent)
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    return createCustomView(position, convertView, parent)
                }

                private fun createCustomView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val row = convertView ?: android.view.LayoutInflater.from(context).inflate(
                        R.layout.item_asset_dropdown,
                        parent,
                        false
                    )
                    val item = getItem(position).orEmpty()
                    val ivIcon = row.findViewById<android.widget.ImageView>(R.id.ivAssetDropdownIcon)
                    val tvCode = row.findViewById<android.widget.TextView>(R.id.tvAssetDropdownCode)
                    val tvName = row.findViewById<android.widget.TextView>(R.id.tvAssetDropdownName)

                    if (gold) {
                        tvCode.text = item
                        val goldType = GoldType.entries.firstOrNull { it.displayName == item }
                        tvName.text = if (goldType?.inputUnit == GoldInputUnit.PIECE) "Adet bazlı" else "Gram bazlı"
                        ivIcon.setImageResource(R.drawable.ic_gold_custom)
                    } else {
                        tvCode.text = item
                        tvName.text = CurrencyFlagProvider.getCurrencyDisplayName(item)
                        ivIcon.setImageResource(CurrencyFlagProvider.getFlagResIdSafe(item))
                    }
                    return row
                }
            }
            sheet.etAssetType.setAdapter(adapter)
            sheet.layoutAssetType.hint = if (gold) "Altın türü" else "Para birimi"
            sheet.layoutInvestmentAmount.placeholderText =
                if (gold) "Gram için ondalıklı, diğerleri için adet"
                else "Örnek: 150"
        }

        configureAssets(false)
        updateDateButton()
        sheet.etAssetType.setOnClickListener { sheet.etAssetType.showDropDown() }
        sheet.etAssetType.setOnItemClickListener { _, _, _, _ ->
            selectedGoldType =
                GoldType.entries.firstOrNull {
                    it.displayName == sheet.etAssetType.text.toString()
                }
            val isPiece = selectedGoldType?.inputUnit == GoldInputUnit.PIECE
            sheet.etInvestmentAmount.inputType =
                if (isPiece) InputType.TYPE_CLASS_NUMBER
                else InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            sheet.layoutInvestmentAmount.hint =
                when {
                    selectedGoldType?.inputUnit == GoldInputUnit.GRAM -> "Miktar (gram)"
                    isPiece -> "Miktar (adet)"
                    else -> "Miktar"
                }
        }

        sheet.assetKindToggle.addOnButtonCheckedListener { _, checkedId, checked ->
            if (checked) configureAssets(checkedId == R.id.btnGoldKind)
        }
        sheet.rgRateType.setOnCheckedChangeListener { _, checkedId ->
            sheet.layoutManualRate.visibility =
                if (checkedId == R.id.rbManualRate) View.VISIBLE else View.GONE
        }
        sheet.btnPurchaseDate.setOnClickListener {
            trackDialog(
                DatePickerDialog(
                    requireContext(),
                    { _, year, month, day ->
                        selectedDate = Calendar.getInstance().apply {
                            set(year, month, day, 12, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        updateDateButton()
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
                )
            ).show()
        }

        sheet.etInvestmentAmount.setupMoneyInput(maxValue = 999_999_999.99, maxDecimals = 4, layout = sheet.layoutInvestmentAmount)
        sheet.etManualRate.setupMoneyInput(maxValue = 999_999_999.99, maxDecimals = 4, layout = sheet.layoutManualRate)
        sheet.etInvestmentNote.setupTextInput(maxLength = 150, layout = sheet.layoutInvestmentNote)

        sheet.btnSaveInvestment.setOnClickListener {
            val selectedName = sheet.etAssetType.text?.toString()?.trim().orEmpty()
            if (selectedName !in availableAssetOptions) {
                sheet.layoutAssetType.error = "Listeden geçerli bir varlık seçin"
                sheet.etAssetType.requestFocus()
                return@setOnClickListener
            }

            if (isGold) {
                selectedGoldType =
                    GoldType.entries.firstOrNull { type ->
                        type.displayName == selectedName
                    }

                if (selectedGoldType == null) {
                    sheet.layoutAssetType.error =
                        "Listeden geçerli bir altın türü seçin"
                    sheet.etAssetType.requestFocus()
                    return@setOnClickListener
                }
            }
            sheet.layoutAssetType.error = null

            val quantityResult =
                DecimalInputValidator.positiveQuantity(
                    rawValue = sheet.etInvestmentAmount.text,
                    fieldName = "Miktar",
                    wholeNumberOnly =
                        selectedGoldType?.inputUnit ==
                                GoldInputUnit.PIECE
                )

            if (quantityResult is DecimalInputResult.Invalid) {
                sheet.layoutInvestmentAmount.error =
                    quantityResult.message
                sheet.etInvestmentAmount.requestFocus()
                return@setOnClickListener
            }

            val quantity =
                (quantityResult as DecimalInputResult.Valid)
                    .value
                    .toDouble()
            sheet.layoutInvestmentAmount.error = null

            val code = selectedName.uppercase(Locale.ROOT)
            val purchasePrice =
                if (sheet.rbManualRate.isChecked) {
                    when (
                        val result =
                            DecimalInputValidator.positiveMoney(
                                rawValue = sheet.etManualRate.text,
                                fieldName = "Alış fiyatı",
                                maxScale = 6
                            )
                    ) {
                        is DecimalInputResult.Valid ->
                            result.value.toDouble()

                        is DecimalInputResult.Invalid -> {
                            sheet.layoutManualRate.error =
                                result.message
                            sheet.etManualRate.requestFocus()
                            return@setOnClickListener
                        }
                    }
                } else {
                    viewModel.currentPurchasePrice(code, selectedGoldType)
                }
            if (purchasePrice == null || purchasePrice <= 0.0 || !purchasePrice.isFinite()) {
                sheet.layoutManualRate.error =
                    if (sheet.rbManualRate.isChecked) "Geçerli bir fiyat girin"
                    else "Güncel satış fiyatı bulunamadı; manuel fiyat girin"
                if (!sheet.rbManualRate.isChecked) sheet.rbManualRate.isChecked = true
                sheet.etManualRate.requestFocus()
                return@setOnClickListener
            }
            sheet.layoutManualRate.error = null

            val currentPortfolioCount = viewModel.portfolioItems.value?.size ?: (viewModel.portfolioAssetCount.value ?: 0)
            if (currentPortfolioCount >= 20) {
                dialog.dismiss()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Portföy Kapasitesi Doldu")
                    .setMessage("Portföyünüze en fazla 20 farklı varlık ekleyebilirsiniz.")
                    .setPositiveButton("Tamam", null)
                    .show()
                return@setOnClickListener
            }

            val note = sheet.etInvestmentNote.text?.toString()?.trim()?.ifBlank { null }
            val goldType = selectedGoldType
            if (isGold && goldType != null) {
                viewModel.insertGold(
                    type = goldType,
                    quantity = quantity,
                    purchaseUnitPrice = purchasePrice,
                    purchaseDate = selectedDate.timeInMillis,
                    note = note
                )
            } else {
                viewModel.insertCurrency(
                    code = code,
                    quantity = quantity,
                    purchaseUnitPrice = purchasePrice,
                    purchaseDateText = dateFormatter.format(selectedDate.time),
                    note = note
                )
            }
            com.epatay.digitalwallet.util.InAppNotification.show(
                activity,
                "Varlık kaydedildi",
                com.epatay.digitalwallet.util.NotificationType.SUCCESS
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditDialog(item: PortfolioAssetItem) {
        val padding = (20 * resources.displayMetrics.density).toInt()
        val amount = EditText(requireContext()).apply {
            hint = "Miktar (${item.unitLabel})"
            inputType =
                if (item.requiresWholeQuantity) InputType.TYPE_CLASS_NUMBER
                else InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item.quantity.toString())
            keyListener =
                android.text.method.DigitsKeyListener.getInstance(
                    "0123456789,."
                )
            filters =
                arrayOf(android.text.InputFilter.LengthFilter(24))
        }
        val price = EditText(requireContext()).apply {
            hint = "Birim alış fiyatı"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item.purchaseUnitPrice?.toString().orEmpty())
            keyListener =
                android.text.method.DigitsKeyListener.getInstance(
                    "0123456789,."
                )
            filters =
                arrayOf(android.text.InputFilter.LengthFilter(24))
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
            addView(amount)
            addView(price)
        }
        val dialog = trackDialog(
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("${item.displayName} varlığını düzenle")
                .setView(container)
                .setNegativeButton("İptal", null)
                .setPositiveButton("Güncelle", null)
                .create()
        )
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amountResult =
                    DecimalInputValidator.positiveQuantity(
                        rawValue = amount.text,
                        fieldName = "Miktar",
                        wholeNumberOnly = item.requiresWholeQuantity
                    )
                val priceResult =
                    DecimalInputValidator.positiveMoney(
                        rawValue = price.text,
                        fieldName = "Alış fiyatı",
                        maxScale = 6
                    )

                if (amountResult is DecimalInputResult.Invalid) {
                    amount.error = amountResult.message
                    return@setOnClickListener
                }
                if (priceResult is DecimalInputResult.Invalid) {
                    price.error = priceResult.message
                    return@setOnClickListener
                }

                val newAmount =
                    (amountResult as DecimalInputResult.Valid)
                        .value
                        .toDouble()
                val newPrice =
                    (priceResult as DecimalInputResult.Valid)
                        .value
                        .toDouble()
                viewModel.update(item, newAmount, newPrice)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showDeleteDialog(item: PortfolioAssetItem) {
        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Varlığı sil")
                .setMessage("${item.displayName} kaydını silmek istediğinizden emin misiniz?")
                .setNegativeButton("İptal", null)
                .setPositiveButton("Sil") { _, _ -> viewModel.delete(item) }
                .create()

        trackDialog(dialog).show()
    }

    private fun setupPieChart(items: List<PortfolioAssetItem>) {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        val onSurfaceColor = typedValue.data

        binding.llInvestmentDetails.removeAllViews()
        val positive =
            items
                .groupBy { item ->
                    if (item.kind == PortfolioAssetKind.GOLD) {
                        Triple(item.kind, "GLD", "Altın")
                    } else {
                        Triple(item.kind, item.code, item.displayName)
                    }
                }
                .map { (identity, group) ->
                    PortfolioChartSlice(
                        kind = identity.first,
                        code = identity.second,
                        displayName = identity.third,
                        value = group.sumOf { it.currentValue ?: 0.0 }.toFloat()
                    )
                }
                .filter { it.value > 0f }
                .sortedByDescending(PortfolioChartSlice::value)
        binding.tvInvestmentChartEmpty.visibility =
            if (positive.isEmpty()) View.VISIBLE else View.GONE
        binding.pieChart.visibility =
            if (positive.isEmpty()) View.GONE else View.VISIBLE
        if (positive.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        val entries =
            positive.map { slice ->
                PieEntry(slice.value, slice.displayName)
            }
        val dataSet = PieDataSet(entries, "").apply {
            colors =
                positive.map(::portfolioChartColor)
            setDrawValues(false)
            sliceSpace = 3f
        }
        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 58f
            setHoleColor(android.graphics.Color.TRANSPARENT)
            centerText = "Portföy"
            setCenterTextSize(13f)
            setCenterTextColor(onSurfaceColor)
            invalidate()
        }

        val dpToPx = { dp: Int -> (dp * resources.displayMetrics.density).toInt() }
        positive.take(3).forEach { slice ->
            val row = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }

            val icon = android.widget.ImageView(requireContext()).apply {
                if (slice.kind == PortfolioAssetKind.GOLD) {
                    setImageResource(R.drawable.ic_gold_coin)
                } else {
                    setImageResource(CurrencyFlagProvider.getFlagResId(slice.code))
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(dpToPx(16), dpToPx(16)).apply {
                    marginEnd = dpToPx(8)
                }
            }

            val tv = android.widget.TextView(requireContext()).apply {
                text = "${slice.displayName}  ${formatCurrency(slice.value.toDouble())}"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(onSurfaceColor)
            }

            row.addView(icon)
            row.addView(tv)
            binding.llInvestmentDetails.addView(row)
        }
    }

    private fun setupDetailedBreakdown(items: List<PortfolioAssetItem>, grandTotal: Double) {
        val hasItems = items.isNotEmpty()
        binding.layoutToggleBreakdown.visibility = if (hasItems) View.VISIBLE else View.GONE
        if (!hasItems) {
            binding.layoutGrandTotalBreakdown.visibility = View.GONE
            binding.llAllAssetsBreakdown.removeAllViews()
            return
        }

        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        val onSurfaceColor = typedValue.data

        val typedVariant = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedVariant, true)
        val onSurfaceVariantColor = typedVariant.data

        val typedPrimary = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedPrimary, true)
        val primaryColor = typedPrimary.data

        binding.llAllAssetsBreakdown.removeAllViews()

        // Altın türlerini birbirinden ayırarak (Gram, Çeyrek, Yarım, Tam vb.) ve dövizleri listeliyoruz
        val groupedAssets = items
            .groupBy { Triple(it.kind, it.code, it.displayName) }
            .map { (identity, group) ->
                val kind = identity.first
                val code = identity.second
                val displayName = identity.third
                val totalQuantity = group.sumOf { it.quantity }
                val unitLabel = group.firstOrNull()?.unitLabel.orEmpty()
                val totalValue = group.sumOf { it.currentValue ?: 0.0 }
                val costs = group.mapNotNull { it.totalPurchaseCost }
                val totalCost = if (costs.isNotEmpty()) costs.sum() else null
                val totalProfit = if (totalCost != null) totalValue - totalCost else null
                val profitPercentage = if (totalCost != null && totalCost > 0.0) (totalProfit!! / totalCost) * 100.0 else null
                val sharePercentage = if (grandTotal > 0.0) (totalValue / grandTotal) * 100.0 else 0.0

                AssetBreakdownItem(
                    kind = kind,
                    code = code,
                    displayName = displayName,
                    totalQuantity = totalQuantity,
                    unitLabel = unitLabel,
                    totalValue = totalValue,
                    totalProfit = totalProfit,
                    profitPercentage = profitPercentage,
                    sharePercentage = sharePercentage
                )
            }
            .filter { it.totalValue > 0.0 }
            .sortedByDescending { it.totalValue }

        val dpToPx = { dp: Int -> (dp * resources.displayMetrics.density).toInt() }

        groupedAssets.forEach { asset ->
            val row = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(6), 0, dpToPx(6))
            }

            val icon = android.widget.ImageView(requireContext()).apply {
                if (asset.kind == PortfolioAssetKind.GOLD) {
                    setImageResource(R.drawable.ic_gold_coin)
                } else {
                    setImageResource(CurrencyFlagProvider.getFlagResIdSafe(asset.code))
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)).apply {
                    marginEnd = dpToPx(10)
                }
            }

            val leftCol = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvName = android.widget.TextView(requireContext()).apply {
                text = asset.displayName
                textSize = 13.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(onSurfaceColor)
            }

            val tvSub = android.widget.TextView(requireContext()).apply {
                val qtyStr = "${formatQuantity(asset.totalQuantity)} ${asset.unitLabel}"
                val shareStr = "%${formatNumber(asset.sharePercentage)} pay"
                text = "$qtyStr • $shareStr"
                textSize = 11.5f
                setTextColor(onSurfaceVariantColor)
            }
            leftCol.addView(tvName)
            leftCol.addView(tvSub)

            val rightCol = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.END
            }

            val tvVal = android.widget.TextView(requireContext()).apply {
                text = formatCurrency(asset.totalValue)
                textSize = 13.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(primaryColor)
            }

            val tvProfit = android.widget.TextView(requireContext()).apply {
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                if (asset.totalProfit == null) {
                    text = "-"
                    setTextColor(Color.parseColor("#888888"))
                } else when {
                    abs(asset.totalProfit) < 0.01 -> {
                        text = "0,00 TL"
                        setTextColor(Color.parseColor("#888888"))
                    }
                    asset.totalProfit > 0.0 -> {
                        text = "+${formatCurrency(asset.totalProfit)} (%${formatNumber(asset.profitPercentage)})"
                        setTextColor(Color.parseColor("#2E7D32"))
                    }
                    else -> {
                        text = "-${formatCurrency(abs(asset.totalProfit))} (%${formatNumber(asset.profitPercentage)})"
                        setTextColor(Color.parseColor("#C62828"))
                    }
                }
            }
            rightCol.addView(tvVal)
            rightCol.addView(tvProfit)

            row.addView(icon)
            row.addView(leftCol)
            row.addView(rightCol)

            binding.llAllAssetsBreakdown.addView(row)
        }
    }

    private data class AssetBreakdownItem(
        val kind: PortfolioAssetKind,
        val code: String,
        val displayName: String,
        val totalQuantity: Double,
        val unitLabel: String,
        val totalValue: Double,
        val totalProfit: Double?,
        val profitPercentage: Double?,
        val sharePercentage: Double
    )

    private data class PortfolioChartSlice(
        val kind: PortfolioAssetKind,
        val code: String,
        val displayName: String,
        val value: Float
    )

    private fun portfolioChartColor(slice: PortfolioChartSlice): Int =
        if (slice.kind == PortfolioAssetKind.GOLD) {
            GOLD_CHART_COLOR
        } else {
            CurrencyFlagProvider.getChartColor(slice.code)
        }

    private fun formatCurrency(value: Double): String =
        NumberFormat.getNumberInstance(TR_LOCALE).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(value) + " TL"

    private fun formatQuantity(value: Double): String =
        NumberFormat.getNumberInstance(TR_LOCALE).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 4
        }.format(value)

    private fun formatNumber(value: Double?): String =
        value?.let {
            NumberFormat.getNumberInstance(TR_LOCALE).apply {
                minimumFractionDigits = 1
                maximumFractionDigits = 2
            }.format(it)
        } ?: "-"

    override fun onDestroyView() {
        activeDialogs.toList().forEach { dialog ->
            runCatching { dialog.dismiss() }
        }
        activeDialogs.clear()
        binding.rvInvestments.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun <T : Dialog> trackDialog(dialog: T): T {
        activeDialogs += dialog
        dialog.setOnDismissListener {
            activeDialogs -= dialog
        }
        return dialog
    }

    private companion object {
        val TR_LOCALE: Locale = Locale.forLanguageTag("tr-TR")
        val GOLD_CHART_COLOR: Int = 0xFFFFC107.toInt()
    }

    
    }