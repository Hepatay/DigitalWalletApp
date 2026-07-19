package com.epatay.digitalwallet.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import com.epatay.digitalwallet.databinding.ItemExpenseBinding
import java.util.Locale

class TransactionAdapter(
    private var transactionList: List<Transaction>,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(
        val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransactionViewHolder {

        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TransactionViewHolder,
        position: Int
    ) {
        val currentItem = transactionList[position]

        holder.binding.tvItemTitle.text =
            currentItem.title

        holder.binding.tvItemDate.text =
            currentItem.date

        if (currentItem.type == TransactionType.INCOME) {

            holder.binding.tvItemAmount.text =
                "+ ${formatCurrency(currentItem.amount)}"

            holder.binding.tvItemAmount.setTextColor(
                Color.parseColor("#4CAF50")
            )

            holder.binding.ivCategoryIcon.setImageResource(
                android.R.drawable.ic_input_add
            )

            holder.binding.ivCategoryIcon.setColorFilter(
                Color.parseColor("#4CAF50")
            )

        } else {

            holder.binding.tvItemAmount.text =
                "- ${formatCurrency(currentItem.amount)}"

            holder.binding.tvItemAmount.setTextColor(
                Color.parseColor("#F44336")
            )

            val iconRes =
                when (currentItem.category) {

                    "Eğitim" ->
                        R.drawable.ic_school

                    "Gıda" ->
                        R.drawable.ic_food

                    "Ulaşım" ->
                        R.drawable.ic_bus

                    "Fatura" ->
                        R.drawable.ic_bill

                    "Eğlence" ->
                        R.drawable.ic_fun

                    else ->
                        R.drawable.ic_other
                }

            holder.binding.ivCategoryIcon.setImageResource(
                iconRes
            )

            holder.binding.ivCategoryIcon.setColorFilter(
                Color.parseColor("#F44336")
            )
        }

        holder.binding.btnEditTransaction
            .setOnClickListener {

                onEditClick(currentItem)
            }

        holder.binding.btnDeleteTransaction
            .setOnClickListener {

                onDeleteClick(currentItem)
            }
    }

    override fun getItemCount(): Int {
        return transactionList.size
    }

    fun updateData(newList: List<Transaction>) {
        transactionList = newList
        notifyDataSetChanged()
    }

    private fun formatCurrency(value: Double): String {

        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%,.2f ₺",
            value
        )
    }
}