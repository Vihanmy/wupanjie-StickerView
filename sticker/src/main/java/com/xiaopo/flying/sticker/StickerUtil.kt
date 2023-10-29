package com.xiaopo.flying.sticker

import android.graphics.PointF
import android.view.MotionEvent
import java.lang.Math.atan2


object StickerUtil {

    /**
     * 计算两点连线和x轴形成的夹角
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    fun calculateRotation(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x1 - x2).toDouble()
        val y = (y1 - y2).toDouble()
        val radians = atan2(y, x)
        return Math.toDegrees(radians).toFloat()
    }

    /**
     * 当 MotionEvent 有双指的时候, 计算双指的角度
     *
     * @param event
     * @return
     */
    fun calculateRotation(event: MotionEvent?): Float {
        return if (event == null || event.pointerCount < 2) {
            0f
        } else calculateRotation(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
    }

    /**
     * 计算双子缩放中点
     *
     * @param event
     * @param midPoint
     * @return
     */
    fun calculateMidPoint(event: MotionEvent?, midPoint: PointF): PointF {
        if (event == null || event.pointerCount < 2) {
            midPoint[0f] = 0f
            return midPoint
        }
        val x = (event.getX(0) + event.getX(1)) / 2
        val y = (event.getY(0) + event.getY(1)) / 2
        midPoint[x] = y
        return midPoint
    }

    /**
     * 计算双指距离
     */
    fun calculateDistance(event: MotionEvent?): Float {
        return if (event == null || event.pointerCount < 2) {
            0f
        } else calculateDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
    }

    /**
     * 击计算两点的距离
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x1 - x2).toDouble()
        val y = (y1 - y2).toDouble()
        return Math.sqrt(x * x + y * y).toFloat()
    }

}