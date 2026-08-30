package com.epatay.digitalwallet.ui.tutorial

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class TutorialCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val targetViews = mutableListOf<View>()
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#A6000000") // Dim for fallback
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint().apply {
        color = Color.parseColor("#6600E676") // Soft Emerald Glow
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
    }
    private val glowStrokePaint = Paint().apply {
        color = Color.parseColor("#B300E676")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val clearPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }
    
    var useBlurFallback = false // Set to true on API < 31

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setTargets(vararg views: View?) {
        targetViews.clear()
        views.filterNotNull().forEach { targetViews.add(it) }
        postInvalidate()
    }

    fun setTarget(view: View?) {
        setTargets(view)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (useBlurFallback) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        }

        if (targetViews.isEmpty()) return

        val myLocation = IntArray(2)
        getLocationInWindow(myLocation)

        for (tv in targetViews) {
            if (tv.width <= 0 || tv.height <= 0 || !tv.isShown) continue

            val location = IntArray(2)
            tv.getLocationInWindow(location)

            val dx = (location[0] - myLocation[0]).toFloat()
            val dy = (location[1] - myLocation[1]).toFloat()

            val isFab = tv.width == tv.height && tv.width < 300
            val cornerRadius = if (isFab) tv.width / 2f else 32f

            val rect = RectF(dx, dy, dx + tv.width, dy + tv.height)
            val glowRect = RectF(dx - 6f, dy - 6f, dx + tv.width + 6f, dy + tv.height + 6f)

            // 1. Draw emerald glow behind target
            canvas.drawRoundRect(glowRect, cornerRadius + 4f, cornerRadius + 4f, glowPaint)

            // 2. Clear dim if fallback is active
            if (useBlurFallback) {
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, clearPaint)
            }

            // 3. Draw the crisp target view on top of the blurred background
            canvas.save()
            canvas.translate(dx, dy)
            tv.draw(canvas)
            canvas.restore()

            // 4. Draw a subtle glowing border around the target
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowStrokePaint)
        }
    }
}
