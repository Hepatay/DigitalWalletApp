package com.epatay.digitalwallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.data.GoldRate
import com.epatay.digitalwallet.databinding.ItemGoldRateBinding

class GoldAdapter : RecyclerView.Adapter<GoldAdapter.GoldViewHolder>() {

    private var rates: List<GoldRate> = emptyList()

    class GoldViewHolder(val binding: ItemGoldRateBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoldViewHolder =
        GoldViewHolder(
            ItemGoldRateBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: GoldViewHolder, position: Int) {
        val rate = rates[position]
        holder.binding.tvGoldName.text = rate.type.displayName
        holder.binding.tvGoldBuying.text =
            "Alış: ${GoldRateFormatter.price(rate.buyingPrice)}"
        holder.binding.tvGoldSelling.text =
            "Satış: ${GoldRateFormatter.price(rate.sellingPrice)}"
        holder.binding.tvGoldSource.text = "Kaynak: ${rate.source}"
        holder.binding.tvGoldSourceDate.text =
            "Veri tarihi: ${GoldRateFormatter.sourceDate(rate.sourceDate)}"
        holder.binding.tvGoldFetchedAt.text =
            "Uygulamada güncellendi: ${GoldRateFormatter.fetchedAt(rate.fetchedAt)}"
        holder.binding.tvGoldReference.text =
            if (rate.isReference) "Referans fiyat" else "Piyasa fiyatı"
    }

    override fun getItemCount(): Int = rates.size

    fun submitList(newRates: List<GoldRate>) {
        rates = newRates
        notifyDataSetChanged()
    }
}
