package com.epatay.digitalwallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.Expense
import com.epatay.digitalwallet.databinding.ItemExpenseBinding

class ExpenseAdapter : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    // Tıklanma olayını MainActivity'e gönderecek arayüz
    var onItemClick: ((Expense) -> Unit)? = null

    private var expenseList = emptyList<Expense>()

    class ExpenseViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        // 1. Önce o anki satırın verisini çekiyoruz (EN ÜSTE ALINDI)
        val currentItem = expenseList[position]

        // 2. Kategoriye göre ikon ve renk belirle (expense yerine currentItem kullanıldı)
        val iconRes: Int
        val bgColor: String

        when (currentItem.category) {
            "Gıda" -> {
                iconRes = R.drawable.ic_food
                bgColor = "#FF9800" // Turuncu
            }
            "Ulaşım" -> {
                iconRes = R.drawable.ic_bus
                bgColor = "#2196F3" // Mavi
            }
            "Fatura" -> {
                iconRes = R.drawable.ic_bill
                bgColor = "#F44336" // Kırmızı
            }
            "Eğitim" -> {
                iconRes = R.drawable.ic_school
                bgColor = "#9C27B0" // Mor
            }
            "Eğlence" -> {
                iconRes = android.R.drawable.star_on
                bgColor = "#E91E63" // Pembe
            }
            else -> {
                iconRes = android.R.drawable.ic_menu_info_details
                bgColor = "#607D8B" // Gri (Diğer)
            }
        }

        // 3. Belirlediğimiz değerleri tasarıma uygula (Başına holder. eklendi)
        holder.binding.ivCategoryIcon.setImageResource(iconRes)
        holder.binding.cardIconBackground.setCardBackgroundColor(android.graphics.Color.parseColor(bgColor))
        holder.binding.ivCategoryIcon.setColorFilter(android.graphics.Color.WHITE)

        // 4. Metinleri doldur
        holder.binding.tvItemTitle.text = currentItem.title
        holder.binding.tvItemDate.text = currentItem.date
        holder.binding.tvItemAmount.text = "₺${currentItem.amount}"

        // Satıra tıklandığında tetiklenir
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return expenseList.size
    }

    fun setData(expenses: List<Expense>) {
        this.expenseList = expenses
        notifyDataSetChanged()
    }

    // Kaydırılan pozisyondaki harcamayı geri döndüren fonksiyon
    fun getExpenseAt(position: Int): Expense {
        return expenseList[position]
    }

}