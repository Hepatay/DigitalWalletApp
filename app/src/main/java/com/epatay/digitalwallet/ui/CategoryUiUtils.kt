package com.epatay.digitalwallet.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.epatay.digitalwallet.R

object CategoryUiUtils {

    val POPULAR_EXPENSE_CATEGORIES = listOf(
        "Market",
        "Yiyecek ve İçecek",
        "Fatura ve Abonelikler",
        "Ulaşım",
        "Alışveriş",
        "Ev",
        "Araç",
        "Kişisel",
        "Sağlık",
        "Eğlence",
        "Eğitim",
        "Spor ve Hobi",
        "Seyahat",
        "İş",
        "Diğer"
    )

    fun getCategoryIcon(category: String?): Int {
        return when (category) {
            "Market" -> R.drawable.ic_cart
            "Yiyecek ve İçecek", "Gıda" -> R.drawable.ic_food
            "Fatura ve Abonelikler", "Fatura" -> R.drawable.ic_bill
            "Ulaşım" -> R.drawable.ic_bus
            "Alışveriş" -> R.drawable.ic_shopping_bag
            "Ev" -> R.drawable.ic_home
            "Araç" -> R.drawable.ic_car
            "Kişisel" -> R.drawable.ic_person
            "Sağlık" -> R.drawable.ic_health
            "Eğlence" -> R.drawable.ic_fun
            "Eğitim" -> R.drawable.ic_school
            "Spor ve Hobi" -> R.drawable.ic_fitness
            "Seyahat" -> R.drawable.ic_flight
            "İş" -> R.drawable.ic_work
            "Birikim" -> R.drawable.ic_savings
            else -> R.drawable.ic_other
        }
    }

    fun getCategoryColor(category: String?): Int {
        return when (category) {
            "Market" -> Color.parseColor("#4CAF50")
            "Yiyecek ve İçecek", "Gıda" -> Color.parseColor("#FF5722")
            "Fatura ve Abonelikler", "Fatura" -> Color.parseColor("#9C27B0")
            "Ulaşım" -> Color.parseColor("#2196F3")
            "Alışveriş" -> Color.parseColor("#E91E63")
            "Ev" -> Color.parseColor("#795548")
            "Araç" -> Color.parseColor("#607D8B")
            "Kişisel" -> Color.parseColor("#00BCD4")
            "Sağlık" -> Color.parseColor("#F44336")
            "Eğlence" -> Color.parseColor("#673AB7")
            "Eğitim" -> Color.parseColor("#FF9800")
            "Spor ve Hobi" -> Color.parseColor("#009688")
            "Seyahat" -> Color.parseColor("#3F51B5")
            "İş" -> Color.parseColor("#455A64")
            "Birikim" -> Color.parseColor("#2E7D32")
            else -> Color.parseColor("#9E9E9E")
        }
    }

    fun createCategoryDropdownAdapter(
        context: Context,
        categories: List<String>
    ): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            context,
            R.layout.item_category_dropdown,
            R.id.tvCategoryDropdownName,
            categories
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return createCustomView(position, convertView, parent)
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return createCustomView(position, convertView, parent)
            }

            private fun createCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(
                    R.layout.item_category_dropdown,
                    parent,
                    false
                )
                val item = getItem(position).orEmpty()
                val ivIcon = view.findViewById<ImageView>(R.id.ivCategoryDropdownIcon)
                val tvName = view.findViewById<TextView>(R.id.tvCategoryDropdownName)

                tvName.text = item
                ivIcon.setImageResource(getCategoryIcon(item))
                ivIcon.setColorFilter(getCategoryColor(item))

                return view
            }
        }
    }
}
