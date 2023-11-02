package com.xiaopo.flying.sticker.util

import android.util.Log


private const val TIME_TAG = ""

/**
 *# 日志打印拦截处理
 * ```
 * author :Vihanmy
 * date   :2023-06-27 16:30
 * desc   :
 * ```
 */
internal object LOGG {
    @Deprecated("使用debug代替", ReplaceWith("debug(tag, msg)"))
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    @Deprecated("使用error代替", ReplaceWith("error(tag, msg)"))
    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    fun debug(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun debug(tag: String, block: () -> String) {
        tryIt {
            Log.d(tag, block.invoke())
        }
    }

    fun warn(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    fun error(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    /**
     *# 便捷输出当前系统运行时间
     * ```
     * author :Vihanmy
     * date   :2023/7/6 11:02
     * desc   :
     * ```
     */
    fun printCurrentTime(tag: Any? = null) {
        e(TIME_TAG, "${tag?.toString()?.plus("-") ?: ""}currentTimeMillis: ${System.currentTimeMillis()}")
    }
}


/**
 *# 日志打印逻辑和其他逻辑隔离开
 * ```
 * author :Vihanmy
 * date   :2023/7/11 15:55
 * desc   :
 * ```
 */
internal inline fun runLogPrint(block: LOGG.() -> Unit) {
    tryIt {
        block.invoke(LOGG)
    }
}