package com.epatay.digitalwallet.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.databinding.LayoutGenericInfoBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class GenericInfoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutGenericInfoBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutGenericInfoBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefKey = arguments?.getString(ARG_PREF_KEY)
        val title = arguments?.getString(ARG_TITLE).orEmpty()
        val description = arguments?.getString(ARG_DESCRIPTION).orEmpty()
        val iconResId = arguments?.getInt(ARG_ICON_RES, R.drawable.ic_nav_budget) ?: R.drawable.ic_nav_budget

        binding.tvInfoTitle.text = title
        binding.tvInfoDescription.text = description
        binding.ivInfoIcon.setImageResource(iconResId)

        binding.btnInfoConfirm.setOnClickListener {
            markSeen(prefKey)
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val prefKey = arguments?.getString(ARG_PREF_KEY)
        markSeen(prefKey)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun markSeen(prefKey: String?) {
        if (!prefKey.isNullOrBlank()) {
            val context = context ?: return
            val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(prefKey, true).apply()
        }
    }

    companion object {
        private const val ARG_PREF_KEY = "pref_key"
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_ICON_RES = "icon_res"

        const val KEY_HOME = "has_seen_home_sheet"
        const val KEY_MARKETS = "has_seen_markets_sheet"
        const val KEY_PORTFOLIO = "has_seen_portfolio_sheet"

        fun showIfFirstTime(
            fragmentManager: FragmentManager,
            context: Context,
            prefKey: String,
            title: String,
            description: String,
            iconResId: Int = R.drawable.ic_nav_budget
        ) {
            val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean(prefKey, false)) return

            if (fragmentManager.findFragmentByTag(prefKey) != null) return

            val sheet = GenericInfoBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PREF_KEY, prefKey)
                    putString(ARG_TITLE, title)
                    putString(ARG_DESCRIPTION, description)
                    putInt(ARG_ICON_RES, iconResId)
                }
            }
            sheet.show(fragmentManager, prefKey)
        }
    }
}
