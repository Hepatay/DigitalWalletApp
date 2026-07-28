package com.epatay.digitalwallet.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        binding.tvBuyDate.text = item.purchaseDateText
        binding.tvBuyPrice.text =
            "Alış fiyatı: ${formatRate(item.purchaseUnitPrice)}"
        binding.tvTotalBuyCost.text =
            "Toplam maliyet: ${formatCurrency(item.totalPurchaseCost)}"

        if (item.kind == PortfolioAssetKind.GOLD) {
            binding.ivFlag.setImageResource(R.drawable.ic_golds)
        } else {
            binding.ivFlag.setImageResource(
                CurrencyFlagProvider.getFlagResId(item.code)
            )
        }

        val marketBuying = item.marketBuyingPrice
        if (marketBuying == null || item.currentValue == null) {
            binding.tvCurrentRate.text = "Güncel fiyat bilgisi bulunamadı."
            binding.tvCurrentValue.text = "Tahmini güncel değer: -"
            binding.tvProfitLoss.text = "Kâr/Zarar: -"
            binding.tvProfitLoss.setTextColor(Color.parseColor("#888888"))
        } else {
            binding.tvCurrentRate.text =
                "Referans güncel kur: ${formatRate(marketBuying)}"
            binding.tvCurrentValue.text =
                "Tahmini güncel değer: ${formatCurrency(item.currentValue)}"
            bindProfitLoss(binding, item.profitLoss)
        }

        binding.tvSource.text =
            buildString {
                append("Kaynak: ")
                append(item.source ?: "Bulunamadı")
                item.sourceUpdatedAt?.let {
                    append(" • ")
                    append(GoldRateFormatter.fetchedAt(it))
                }
            }
        binding.tvReference.text = "Referans değer • Yatırım tavsiyesi değildir"
        binding.tvNote.text = item.note.orEmpty()
        binding.tvNote.visibility =
            if (item.note.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.ivEditPrice.setOnClickListener { onEditClick(item) }
        binding.btnDeleteInvestment.setOnClickListener { onDeleteClick(item) }
    }

    private fun bindProfitLoss(
        binding: ItemInvestmentBinding,
        difference: Double?
    ) {
        if (difference == null) {
            binding.tvProfitLoss.text = "Kâr/Zarar: -"
            binding.tvProfitLoss.setTextColor(Color.parseColor("#888888"))
            return
        }

        when {
            abs(difference) < 0.01 -> {
                binding.tvProfitLoss.text = "0,00 TL"
                binding.tvProfitLoss.setTextColor(Color.parseColor("#888888"))
            }
            difference > 0.0 -> {
                binding.tvProfitLoss.text =
                    "+${formatCurrency(difference)} Kâr"
                binding.tvProfitLoss.setTextColor(Color.parseColor("#2E7D32"))
            }
            else -> {
                binding.tvProfitLoss.text =
                    "-${formatCurrency(abs(difference))} Zarar"
                binding.tvProfitLoss.setTextColor(Color.parseColor("#C62828"))
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

    private companion object {
        val TR_LOCALE: Locale = Locale.forLanguageTag("tr-TR")
    }
}
