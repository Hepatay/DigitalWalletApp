package com.epatay.digitalwallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyItem
import java.util.Locale

class CurrencyAdapter(
    private var currencyList: List<CurrencyItem>
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    private var multiplier: Double = 1.0

    class CurrencyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRate: TextView = view.findViewById(R.id.tvCurrencyRate)
        val tvName: TextView = view.findViewById(R.id.tvCurrencyName)
        val ivFlag: ImageView = view.findViewById(R.id.ivFlag)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CurrencyViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_currency, parent, false)

        return CurrencyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CurrencyViewHolder,
        position: Int
    ) {
        val item = currencyList[position]

        val totalTry = item.rateValue * multiplier

        val amountString = if (multiplier % 1.0 == 0.0) {
            multiplier.toInt().toString()
        } else {
            multiplier.toString()
        }

        holder.tvRate.text = String.format(
            Locale.forLanguageTag("tr-TR"),
            "%s %s = %.2f ₺",
            amountString,
            item.code,
            totalTry
        )

        holder.tvName.text = item.name

        // Eksik olan satır buydu
        holder.ivFlag.setImageResource(item.flagIcon)
    }

    override fun getItemCount(): Int {
        return currencyList.size
    }

    fun updateData(newList: List<CurrencyItem>) {
        currencyList = newList
        notifyDataSetChanged()
    }

    fun updateMultiplier(newMultiplier: Double) {
        multiplier = if (newMultiplier.isFinite() && newMultiplier > 0.0) {
            newMultiplier
        } else {
            1.0
        }

        notifyDataSetChanged()
    }
}
