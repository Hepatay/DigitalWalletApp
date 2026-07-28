package com.epatay.digitalwallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyItem
import java.text.NumberFormat
import java.util.Locale

class CurrencyAdapter(
    private var currencyList: List<CurrencyItem>
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    private val turkishLocale =
        Locale.forLanguageTag("tr-TR")

    private val rateFormatter =
        NumberFormat
            .getNumberInstance(turkishLocale)
            .apply {
                minimumFractionDigits = 4
                maximumFractionDigits = 4
            }

    class CurrencyViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView =
            view.findViewById(R.id.tvCurrencyTitle)
        val tvUnit: TextView =
            view.findViewById(R.id.tvCurrencyUnit)
        val tvBuying: TextView =
            view.findViewById(R.id.tvForexBuying)
        val tvSelling: TextView =
            view.findViewById(R.id.tvForexSelling)
        val ivFlag: ImageView =
            view.findViewById(R.id.ivFlag)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CurrencyViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_currency,
                    parent,
                    false
                )

        return CurrencyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CurrencyViewHolder,
        position: Int
    ) {
        val item =
            currencyList[position]

        holder.tvTitle.text =
            "${item.code} - ${item.name}"

        holder.tvUnit.text =
            if (item.unit > 1) {
                holder.itemView.context.getString(
                    R.string.currency_unit_multiple,
                    item.unit
                )
            } else {
                holder.itemView.context.getString(R.string.currency_unit_single)
            }

        holder.tvBuying.text =
            holder.itemView.context.getString(
                R.string.currency_buying,
                formatRate(item.forexBuying)
            )

        holder.tvSelling.text =
            holder.itemView.context.getString(
                R.string.currency_selling,
                formatRate(item.forexSelling)
            )

        holder.ivFlag.setImageResource(item.flagResId)
    }

    override fun getItemCount(): Int {
        return currencyList.size
    }

    fun updateData(
        newList: List<CurrencyItem>
    ) {
        currencyList = newList
        notifyDataSetChanged()
    }

    private fun formatRate(
        value: Double?
    ): String {
        return value
            ?.let { rate ->
                "${rateFormatter.format(rate)} TL"
            }
            ?: "-"
    }
}
