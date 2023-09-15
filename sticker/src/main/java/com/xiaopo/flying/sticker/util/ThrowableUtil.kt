package com.xiaopo.flying.sticker.util


/**
 *# 便捷异常捕捉
 * ```
 * author :Vihanmy
 * date   :2023-05-19 13:46
 * desc   :
 * ```
 */
inline fun tryIt(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}