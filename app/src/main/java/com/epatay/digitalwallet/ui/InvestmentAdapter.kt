package com.epatay.digitalwallet.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyFlagProvider
import com.epatay.digitalwallet.databinding.ItemInvestmentBinding
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class InvestmentAdapter(
    private val onEditClick: (PortfolioAssetItem) -> Unit,
    private val onDeleteClick: (PortfolioAssetItem) -> Unit
) : RecyclerView.Adapter<InvestmentAdapter.InvestmentViewHolder>() {

    private var items: List<PortfolioAssetItem> = emptyList()
    private val expandedItemKeys: MutableSet<String> = mutableSetOf()

    class InvestmentViewHolder(val binding: ItemInvestmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InvestmentViewHolder =
        InvestmentViewHolder(
            ItemInvestmentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: InvestmentViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        binding.tvAssetName.text = item.displayName
        binding.tvAmount.text =
            "${formatQuantity(item.quantity)} ${item.unitLabel}"
        binding.tvBuyDate.text = "Alış Tarihi: ${item.purchaseDateText}"
        binding.tvBuyPrice.text =
            "Kullanıcı alış fiyatı: ${formatRate(item.purchaseUnitPrice)}"
        binding.tvTotalBuyCost.text =
            "Toplam maliyet: ${formatCurrency(item.totalPurchaseCost)}"

        if (item.kind == PortfolioAssetKind.GOLD) {
            binding.ivFlag.setImageResource(R.drawable.ic_gold_coin)
        } else {
            binding.ivFlag.setImageResource(
                CurrencyFlagProvider.getFlagResId(item.code)
            )
        }

        val marketBuying = item.marketBuyingPrice
        if (marketBuying == null || item.currentValue == null) {
            binding.tvSummaryCurrentValue.text = "-"
            binding.tvSummaryProfitLoss.text = "Kâr/Zarar: -"
            binding.tvSummaryProfitLoss.setTextColor(Color.parseColor("#888888"))

            binding.tvCurrentRate.text = "Güncel fiyat bilgisi bulunamadı."
            binding.tvMarketSelling.text = "Piyasa satış fiyatı: -"
            binding.tvSpread.text = "Makas: -"
            binding.tvCurrentValue.text = "Tahmini satış değeri: -"
            binding.tvProfitLoss.text = "Kâr/Zarar: -"
            binding.tvProfitLoss.setTextColor(Color.parseColor("#888888"))
        } else {
            val formattedValue = formatCurrency(item.currentValue)
            binding.tvSummaryCurrentValue.text = formattedValue
            binding.tvCurrentValue.text = "Tahmini satış değeri: $formattedValue"

            binding.tvCurrentRate.text =
                "Piyasa alış fiyatı: ${formatRate(marketBuying)}"
            binding.tvMarketSelling.text =
                "Piyasa satış fiyatı: ${formatRate(item.marketSellingPrice)}"
            binding.tvSpread.text =
                "Makas: ${formatCurrency(item.spread)} " +
                    "(%${formatNumber(item.spreadPercentage)})"

            bindProfitLoss(binding, item.profitLoss, item.profitLossPercentage)
        }

        val sourceUpdate =
            item.sourceUpdatedText
                ?.takeIf(String::isNotBlank)
                ?: item.sourceUpdatedAt?.let(
                    GoldRateFormatter::fetchedAt
                )
        val fetchedAt =
            item.sourceFetchedAt
                ?.takeIf {
                    item.sourceUpdatedText != null ||
                        it != item.sourceUpdatedAt
                }
                ?.let(GoldRateFormatter::fetchedAt)
        binding.tvSource.text =
            PortfolioSourceLabelFormatter.format(
                source = item.source,
                sourceUpdatedText = sourceUpdate,
                fetchedAtText = fetchedAt
            )
        binding.tvReference.text = "Referans bilgi amaçlıdır • Yatırım tavsiyesi değildir"
        binding.tvNote.text = "Not: ${item.note.orEmpty()}"
        binding.tvNote.visibility =
            if (item.note.isNullOrBlank()) View.GONE else View.VISIBLE

        // Açılır / Kapanır Durumu
        val isExpanded = expandedItemKeys.contains(item.stableKey)
        binding.layoutExpandedDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE
        binding.ivExpandChevron.rotation = if (isExpanded) 180f else 0f

        val toggleExpand = {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                if (expandedItemKeys.contains(item.stableKey)) {
                    expandedItemKeys.remove(item.stableKey)
                } else {
                    expandedItemKeys.add(item.stableKey)
                }
                notifyItemChanged(currentPos)
            }
        }

        binding.cardInvestmentItem.setOnClickListener { toggleExpand() }
        binding.layoutSummaryHeader.setOnClickListener { toggleExpand() }
        binding.ivExpandChevron.setOnClickListener { toggleExpand() }

        binding.ivMoreOptions.setOnClickListener { view: android.view.View ->
            val popup = PopupMenu(view.context, view)
            popup.inflate(R.menu.menu_investment_options)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEditClick(item)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun bindProfitLoss(
        binding: ItemInvestmentBinding,
        difference: Double?,
        percentage: Double?
    ) {
        if (difference == null) {
            binding.tvProfitLoss.text = "Kâr/Zarar: -"
            binding.tvProfitLoss.setTextColor(Color.parseColor("#888888"))
            binding.tvSummaryProfitLoss.text = "Kâr/Zarar: -"
            binding.tvSummaryProfitLoss.setTextColor(Color.parseColor("#888888"))
            return
        }

        when {
            abs(difference) < 0.01 -> {
                binding.tvProfitLoss.text = "0,00 TL"
                binding.tvProfitLoss.setTextColor(Color.parseColor("#888888"))
                binding.tvSummaryProfitLoss.text = "0,00 TL"
                binding.tvSummaryProfitLoss.setTextColor(Color.parseColor("#888888"))
            }
            difference > 0.0 -> {
                binding.tvProfitLoss.text =
                    "+${formatCurrency(difference)} Kâr (%${formatNumber(percentage)})"
                binding.tvProfitLoss.setTextColor(Color.parseColor("#2E7D32"))
                binding.tvSummaryProfitLoss.text =
                    "+${formatCurrency(difference)} (%${formatNumber(percentage)})"
                binding.tvSummaryProfitLoss.setTextColor(Color.parseColor("#2E7D32"))
            }
            else -> {
                binding.tvProfitLoss.text =
                    "-${formatCurrency(abs(difference))} Zarar (%${formatNumber(percentage)})"
                binding.tvProfitLoss.setTextColor(Color.parseColor("#C62828"))
                binding.tvSummaryProfitLoss.text =
                    "-${formatCurrency(abs(difference))} (%${formatNumber(percentage)})"
                binding.tvSummaryProfitLoss.setTextColor(Color.parseColor("#C62828"))
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun setData(newItems: List<PortfolioAssetItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun formatRate(value: Double?): String =
        value?.let {
            NumberFormat.getNumberInstance(TR_LOCALE).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 4
            }.format(it) + " TL"
        } ?: "-"

    private fun formatQuantity(value: Double): String =
        NumberFormat.getNumberInstance(TR_LOCALE).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 4
        }.format(value)

    private fun formatCurrency(value: Double?): String =
        value?.let {
            NumberFormat.getNumberInstance(TR_LOCALE).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }.format(it) + " TL"
        } ?: "-"

    private fun formatNumber(value: Double?): String =
        value?.let {
            NumberFormat.getNumberInstance(TR_LOCALE).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }.format(it)
        } ?: "-"

    private companion object {
        val TR_LOCALE: Locale = Locale.forLanguageTag("tr-TR")
    }
}
