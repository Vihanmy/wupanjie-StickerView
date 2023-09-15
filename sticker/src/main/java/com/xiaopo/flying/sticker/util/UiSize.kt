package com.xiaopo.flying.sticker.util

import android.content.res.Resources
import android.util.TypedValue


/**
 *# UI尺寸便捷单位转换
 * ```
 * author :Vihanmy
 * date   :2023-05-15 13:47
 * desc   :
 * ```
 */

//【】px
val Float.px
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)


//【】dp
val Float.dp
    //get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)
    get() = (this * (Resources.getSystem().displayMetrics.density) + 0.5f)

val Int.dp
    //get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics)
    get() = (this * (Resources.getSystem().displayMetrics.density) + 0.5f)

//【】sp
val Float.sp
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, Resources.getSystem().displayMetrics)

val Int.sp
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), Resources.getSystem().displayMetrics)
