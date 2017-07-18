package sample.usingcamera2api.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import java.security.KeyStore

/**
 * Created by maueki on 17/07/18.
 */

class ResultOverlay : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?,
                attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int,
                defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val paint = Paint()
        paint.color = Color.argb(255,255,0,0)

        canvas!!.drawRect(Rect(0, 0, 100, 100), paint)
    }
}