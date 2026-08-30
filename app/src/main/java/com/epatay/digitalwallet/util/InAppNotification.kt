package com.epatay.digitalwallet.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.epatay.digitalwallet.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

enum class NotificationType {
    SUCCESS,
    INFO,
    WARNING,
    ERROR
}

object InAppNotification {

    private var activeOverlayView: View? = null
    private var dismissHandler: Handler? = null
    private var dismissRunnable: Runnable? = null

    fun show(
        activity: Activity?,
        message: CharSequence,
        type: NotificationType = NotificationType.SUCCESS,
        durationMs: Long = 2300L,
        actionText: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return

        val decorView = activity.window?.decorView as? ViewGroup ?: return

        // Cancel and remove existing active notification
        dismissHandler?.removeCallbacksAndMessages(null)
        activeOverlayView?.let { oldView ->
            (oldView.parent as? ViewGroup)?.removeView(oldView)
        }
        activeOverlayView = null

        val overlay = LayoutInflater.from(activity).inflate(
            R.layout.layout_in_app_notification,
            decorView,
            false
        )
        activeOverlayView = overlay
        decorView.addView(overlay)

        val card = overlay.findViewById<MaterialCardView>(R.id.cardNotificationRoot)
        val ivIcon = overlay.findViewById<ImageView>(R.id.ivNotificationIcon)
        val tvMessage = overlay.findViewById<TextView>(R.id.tvNotificationMessage)
        val btnAction = overlay.findViewById<MaterialButton>(R.id.btnNotificationAction)

        tvMessage.text = message

        // Position under status bar dynamically
        val statusBarHeight = getStatusBarHeight(activity)
        val density = activity.resources.displayMetrics.density
        val params = card.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = statusBarHeight + (10 * density).toInt()
        card.layoutParams = params

        // Style by type
        when (type) {
            NotificationType.SUCCESS -> {
                ivIcon.setImageResource(R.drawable.ic_check_circle)
                ivIcon.setColorFilter(Color.parseColor("#00E676"))
            }
            NotificationType.INFO -> {
                ivIcon.setImageResource(R.drawable.ic_info_circle)
                ivIcon.setColorFilter(Color.parseColor("#29B6F6"))
            }
            NotificationType.WARNING -> {
                ivIcon.setImageResource(R.drawable.ic_warning_circle)
                ivIcon.setColorFilter(Color.parseColor("#FFB300"))
            }
            NotificationType.ERROR -> {
                ivIcon.setImageResource(R.drawable.ic_error_circle)
                ivIcon.setColorFilter(Color.parseColor("#FF5252"))
            }
        }

        // Action button (e.g. "Geri Al")
        if (!actionText.isNullOrBlank() && onAction != null) {
            btnAction.visibility = View.VISIBLE
            btnAction.text = actionText
            btnAction.setOnClickListener {
                dismissNow(overlay, card)
                onAction.invoke()
            }
        } else {
            btnAction.visibility = View.GONE
        }

        // Tap on card to dismiss
        card.setOnClickListener {
            dismissNow(overlay, card)
        }

        // Entrance animation
        val startY = -120f
        card.translationY = startY
        card.alpha = 0f
        card.scaleX = 0.94f
        card.scaleY = 0.94f

        val animY = ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, 0f)
        val animAlpha = ObjectAnimator.ofFloat(card, View.ALPHA, 1f)
        val animScaleX = ObjectAnimator.ofFloat(card, View.SCALE_X, 1f)
        val animScaleY = ObjectAnimator.ofFloat(card, View.SCALE_Y, 1f)

        AnimatorSet().apply {
            playTogether(animY, animAlpha, animScaleX, animScaleY)
            interpolator = OvershootInterpolator(1.15f)
            duration = 320
            start()
        }

        // Schedule auto dismiss
        val handler = Handler(Looper.getMainLooper())
        dismissHandler = handler
        val runnable = Runnable {
            dismissNow(overlay, card)
        }
        dismissRunnable = runnable
        handler.postDelayed(runnable, durationMs)
    }

    private fun dismissNow(overlay: View, card: View) {
        dismissHandler?.removeCallbacksAndMessages(null)
        if (overlay.parent == null) return

        val animY = ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, -100f)
        val animAlpha = ObjectAnimator.ofFloat(card, View.ALPHA, 0f)
        val animScaleX = ObjectAnimator.ofFloat(card, View.SCALE_X, 0.94f)
        val animScaleY = ObjectAnimator.ofFloat(card, View.SCALE_Y, 0.94f)

        AnimatorSet().apply {
            playTogether(animY, animAlpha, animScaleX, animScaleY)
            interpolator = AccelerateInterpolator()
            duration = 220
            start()
        }

        overlay.postDelayed({
            (overlay.parent as? ViewGroup)?.removeView(overlay)
            if (activeOverlayView === overlay) {
                activeOverlayView = null
            }
        }, 230)
    }

    private fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else (28 * context.resources.displayMetrics.density).toInt()
    }
}

// Extension helpers for Fragments
fun Fragment.showInAppSuccess(message: CharSequence, durationMs: Long = 2300L) {
    if (isAdded) InAppNotification.show(activity, message, NotificationType.SUCCESS, durationMs)
}

fun Fragment.showInAppError(message: CharSequence, durationMs: Long = 2800L) {
    if (isAdded) InAppNotification.show(activity, message, NotificationType.ERROR, durationMs)
}

fun Fragment.showInAppWarning(message: CharSequence, durationMs: Long = 2600L) {
    if (isAdded) InAppNotification.show(activity, message, NotificationType.WARNING, durationMs)
}

fun Fragment.showInAppInfo(message: CharSequence, durationMs: Long = 2300L) {
    if (isAdded) InAppNotification.show(activity, message, NotificationType.INFO, durationMs)
}

fun Fragment.showInAppAction(message: CharSequence, actionText: String, onAction: () -> Unit) {
    if (isAdded) InAppNotification.show(activity, message, NotificationType.INFO, 3500L, actionText, onAction)
}