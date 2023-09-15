package com.xiaopo.flying.sticker.sticker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import com.xiaopo.flying.sticker.R
import com.xiaopo.flying.sticker.sticker.protocol.Sticker
import com.xiaopo.flying.sticker.util.dp

class SampleSticker(private val context: Context) : Sticker() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 30f
        color = Color.RED
        style = Paint.Style.FILL_AND_STROKE
    }

    /////////////////////////////////////////////////////////
    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.concat(matrix)

        //【】绘制转换后的图形
        val stickerBounds = boundPoints
        val rect = Rect(
            /* left = */ stickerBounds[0].toInt(),
            /* top = */ stickerBounds[1].toInt(),
            /* right = */ stickerBounds[6].toInt(),
            /* bottom = */ stickerBounds[7].toInt(),
        )

        canvas.drawRect(rect, paint.backgroundStyle())
        canvas.drawCircle(0f, 0f, 5.dp, paint.circleStyle())
        canvas.restore()


        //【】绘制原点
        canvas.drawCircle(0f, 0f, 5.dp, paint.circleStyle())

        //【】绘制转换图形的外接矩形
        canvas.drawRect(mappedBound, paint.boundsRectStyle())

        //【】绘制转换图形左上右下的实际展示坐标
        val mappedPoints = mappedBoundPoints
        val path = Path().apply {
            //顺时针连接
            moveTo(mappedPoints[0], mappedPoints[1])
            lineTo(mappedPoints[2], mappedPoints[3])
            lineTo(mappedPoints[6], mappedPoints[7])
            lineTo(mappedPoints[4], mappedPoints[5])
            close()
        }
        canvas.drawPath(path, paint.realBoundsRectStyle())

    }

    override fun getWidth(): Int {
        return 200.dp.toInt()
    }

    override fun getHeight(): Int {
        return 150.dp.toInt()
    }

    override fun setDrawable(drawable: Drawable) = apply {

    }

    override fun getDrawable(): Drawable {
        return ContextCompat.getDrawable(context, R.drawable.sticker_transparent_background);
    }

    override fun setAlpha(alpha: Int) = apply {

    }

    /////////////////////////////////////////////////////////
    private fun Paint.circleStyle() = apply {
        color = Color.BLUE
        style = Paint.Style.FILL_AND_STROKE
    }

    private fun Paint.backgroundStyle() = apply {
        color = Color.RED
        style = Paint.Style.FILL_AND_STROKE
    }

    private fun Paint.boundsRectStyle() = apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 1.dp
    }

    private fun Paint.realBoundsRectStyle() = apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 1.dp
    }
}