package com.epatay.digitalwallet.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import com.epatay.digitalwallet.databinding.ItemExpenseBinding
import java.util.Locale

class TransactionAdapter(
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : ListAdapter<
    Transaction,
    TransactionAdapter.TransactionViewHolder
>(
    TransactionDiffCallback
) {

    constructor(
        transactionList: List<Transaction>,
        onEditClick: (Transaction) -> Unit,
        onDeleteClick: (Transaction) -> Unit
    ) : this(
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick
    ) {
        submitList(transactionList)
    }

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
        val currentItem = getItem(position)

        holder.binding.tvItemTitle.text =
            currentItem.title

        holder.binding.tvItemDate.text =
            "${currentItem.date} • ${currentItem.category}"

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

        holder.binding.btnTransactionMenu
            .setOnClickListener { anchor ->

                PopupMenu(
                    anchor.context,
                    anchor
                ).apply {
                    menu.add(
                        0,
                        MENU_EDIT,
                        0,
                        "Düzenle"
                    ).setIcon(
                        android.R.drawable.ic_menu_edit
                    )

                    menu.add(
                        0,
                        MENU_DELETE,
                        1,
                        "Sil"
                    ).setIcon(
                        android.R.drawable.ic_menu_delete
                    )

                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            MENU_EDIT -> {
                                onEditClick(currentItem)
                                true
                            }

                            MENU_DELETE -> {
                                onDeleteClick(currentItem)
                                true
                            }

                            else -> false
                        }
                    }

                    show()
                }
            }
    }

    fun updateData(newList: List<Transaction>) {
        submitList(newList)
    }

    private fun formatCurrency(value: Double): String {

        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%,.2f ₺",
            value
        )
    }

    private companion object {
        const val MENU_EDIT = 1
        const val MENU_DELETE = 2

        val TransactionDiffCallback =
            object : DiffUtil.ItemCallback<Transaction>() {

                override fun areItemsTheSame(
                    oldItem: Transaction,
                    newItem: Transaction
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: Transaction,
                    newItem: Transaction
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}
