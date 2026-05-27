package com.github.zardozz.fixedheadertablelayoutsample

import android.content.Context

/** Convert an integer dp value to pixels in the current display metrics. */
fun Int.dp(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()
