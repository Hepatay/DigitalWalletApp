package com.epatay.digitalwallet.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyManager
import com.epatay.digitalwallet.data.InvestmentItem
import com.epatay.digitalwallet.databinding.ItemInvestmentBinding
import java.util.Locale
import kotlin.math.abs

class InvestmentAdapter(
    private val onEditClick: (InvestmentItem) -> Unit,
    private val onDeleteClick: (InvestmentItem) -> Unit
) : RecyclerView.Adapter<InvestmentAdapter.InvestmentViewHolder>() {

    private var investmentList: List<InvestmentItem> =
        emptyList()

    class InvestmentViewHolder(
        val binding: ItemInvestmentBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InvestmentViewHolder {

        val binding = ItemInvestmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return InvestmentViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: InvestmentViewHolder,
        position: Int
    ) {
        val currentItem = investmentList[position]

        // Varlık adını yalnızca bir kez oluşturuyoruz
        val assetName =
            currentItem.assetName
                .trim()
                .uppercase(Locale.ROOT)

        holder.binding.tvAssetName.text =
            currentItem.assetName

        // Gram altında "gram", dövizlerde "Adet" gösterilir
        holder.binding.tvAmount.text =
            if (assetName == "GRAM ALTIN") {
                "${currentItem.amount} gram"
            } else {
                "${currentItem.amount} Adet"
            }

        holder.binding.tvBuyDate.text =
            currentItem.buyDate

        // Varlığa göre ikon veya bayrak
        val flagRes = when (assetName) {
            "USD" -> R.drawable.flag_usd
            "EUR" -> R.drawable.flag_eur
            "GBP" -> R.drawable.flag_gbp
            "JPY" -> R.drawable.flag_jpy
            "AUD" -> R.drawable.flag_aud
            "CAD" -> R.drawable.flag_cad
            "CHF" -> R.drawable.flag_chf
            "RUB" -> R.drawable.flag_rub
            "CNY" -> R.drawable.flag_cny
            "GRAM ALTIN" -> R.drawable.ic_gold
            else -> R.mipmap.ic_launcher
        }

        holder.binding.ivFlag.setImageResource(flagRes)

        val currencyManager =
            CurrencyManager(holder.itemView.context)

        val rates =
            currencyManager
                .getSavedRates()
                ?.conversion_rates

        // Gram altın için API üzerindeki XAU kodu kullanılır
        val rateCode =
            if (assetName == "GRAM ALTIN") {
                "XAU"
            } else {
                assetName
            }

        val rawRate =
            rates?.get(rateCode)

        /*
         * API'nin döndürdüğü değer TL tabanlı olmadığı için
         * 1 / kur dönüşümü yapılıyor.
         *
         * XAU değeri ons fiyatıdır.
         * 1 ons = 31.1034768 gram
         */
        val currentRate =
            if (assetName == "GRAM ALTIN") {

                currencyManager
                    .getSavedGramGoldPrice()
                    ?: currentItem.buyPrice

            } else {

                val rawRate =
                    rates?.get(assetName)

                if (rawRate != null && rawRate > 0.0) {
                    1.0 / rawRate
                } else {
                    currentItem.buyPrice
                }
            }

        val totalBuyCost =
            currentItem.amount * currentItem.buyPrice

        val currentValue =
            currentItem.amount * currentRate

        val difference =
            currentValue - totalBuyCost

        holder.binding.tvBuyPrice.text =
            "Alış Kuru: ${formatRate(currentItem.buyPrice)}"

        holder.binding.tvCurrentRate.text =
            "Anlık Kur: ${formatRate(currentRate)}"

        holder.binding.tvTotalBuyCost.text =
            "Alış Mlyt: ${formatCurrency(totalBuyCost)}"

        holder.binding.tvCurrentValue.text =
            "Güncel: ${formatCurrency(currentValue)}"

        when {

            abs(difference) < 0.01 -> {

                holder.binding.tvProfitLoss.text =
                    "0,00 ₺"

                holder.binding.tvProfitLoss.setTextColor(
                    Color.parseColor("#888888")
                )
            }

            difference > 0.0 -> {

                holder.binding.tvProfitLoss.text =
                    "+${formatCurrency(difference)} Kâr"

                holder.binding.tvProfitLoss.setTextColor(
                    Color.parseColor("#4CAF50")
                )
            }

            else -> {

                holder.binding.tvProfitLoss.text =
                    "-${formatCurrency(abs(difference))} Zarar"

                holder.binding.tvProfitLoss.setTextColor(
                    Color.parseColor("#F44336")
                )
            }
        }

        // Ayar butonuna basınca alış fiyatını düzenler
        holder.binding.ivEditPrice.setOnClickListener {
            onEditClick(currentItem)
        }

        // Çöp kutusuna basınca silme onayını açar
        holder.binding.btnDeleteInvestment.setOnClickListener {
            onDeleteClick(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return investmentList.size
    }

    fun setData(investments: List<InvestmentItem>) {
        investmentList = investments
        notifyDataSetChanged()
    }

    private fun formatRate(value: Double): String {
        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%,.4f ₺",
            value
        )
    }

    private fun formatCurrency(value: Double): String {
        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%,.2f ₺",
            value
        )
    }
}