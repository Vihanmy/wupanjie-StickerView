package com.xiaopo.flying.sticker.util

import android.graphics.Matrix

private val floatArrayOf9: FloatArray by lazy { FloatArray(9) }

internal val Matrix.rotateAngle: Double
    get() {
        // 从矩阵中提取当前旋转角度
        getValues(floatArrayOf9)
        val scaleX = floatArrayOf9[Matrix.MSCALE_X]
        val skewY = floatArrayOf9[Matrix.MSKEW_Y]
        return Math.toDegrees(Math.atan2(skewY.toDouble(), scaleX.toDouble()))
    }