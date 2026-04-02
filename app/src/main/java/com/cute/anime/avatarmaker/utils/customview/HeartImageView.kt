package com.cute.anime.avatarmaker.utils.customview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.cute.anime.avatarmaker.R

class HeartImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var maskBitmap: Bitmap? = null
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val maskDrawable = ContextCompat.getDrawable(context, R.drawable.heart_mask)!!

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        maskBitmap?.recycle()
        maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            val c = Canvas(it)
            maskDrawable.setBounds(0, 0, w, h)
            maskDrawable.draw(c)
        }
    }
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        maskBitmap?.let { canvas.drawBitmap(it, 0f, 0f, maskPaint) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        maskBitmap?.recycle()
        maskBitmap = null
    }
}