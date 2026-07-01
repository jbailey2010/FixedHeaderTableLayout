/*
 * MIT License
 *
 * Copyright (c) 2021 Andrew Beck
 * Copyright (c) 2026 Jeff Bailey (fork)
 */

package com.github.jbailey2010.fixedheadertable

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import kotlin.math.ceil

/**
 * Compute per-column pixel widths by measuring the text of the header row plus up to
 * [sampleSize] body rows. Cost is O(columns × sample) — never O(rows × columns) —
 * so it's cheap enough to run once when the data changes.
 *
 * Pass the same 2D grid you'd render in the table, top row first. `cells[0]` is
 * treated as the header/label row; subsequent rows contribute if within the sample.
 * Rows don't have to be uniform length; missing cells are simply skipped.
 *
 * The measured text width is padded with [horizontalPaddingPx] (matching the
 * left+right padding you set on your cell views) and clamped to
 * `[minWidthDp, maxWidthDp]`. When [useBoldMetrics] is true, measurement uses
 * `Typeface.DEFAULT_BOLD` — a safe over-estimate so bold header text never clips
 * even if body cells use a lighter face.
 *
 * Typical use, inside the adapter setup:
 * ```kotlin
 * val cells = buildList {
 *     add(listOf("") + columnHeaders)                 // corner + header row
 *     for (row in bodyRows) add(listOf(row.label) + row.cells)
 * }
 * val widths = autoFitColumnWidths(context, cells)
 * table.adapter = MyAdapter(data, widths)
 * ```
 */
fun autoFitColumnWidths(
    context: Context,
    cells: List<List<String>>,
    sampleSize: Int = 20,
    textSizeSp: Float = 12f,
    horizontalPaddingPx: Int = 30,
    minWidthDp: Int = 40,
    maxWidthDp: Int = 200,
    safetyMarginDp: Int = 6,
    useBoldMetrics: Boolean = true,
): IntArray {
    if (cells.isEmpty()) return IntArray(0)
    val columnCount = cells.maxOf { it.size }
    if (columnCount == 0) return IntArray(0)

    val metrics = context.resources.displayMetrics
    val paint = Paint().apply {
        // TypedValue.applyDimension respects the user's font scale (including the
        // non-linear scaling introduced in Android 14) via the DisplayMetrics — the
        // pre-Android-14 shortcut of multiplying by `scaledDensity` is deprecated.
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSizeSp, metrics)
        if (useBoldMetrics) typeface = Typeface.DEFAULT_BOLD
    }
    val minPx = (minWidthDp * metrics.density).toInt()
    val maxPx = (maxWidthDp * metrics.density).toInt()
    // Safety margin covers the gap between Paint.measureText (glyph advance) and
    // TextView's line-break decision (which accounts for kerning, anti-aliasing,
    // hinting, and a hair of trailing bearing). Without it, exact-fit widths can
    // wrap the last character to a new line.
    val safetyPx = (safetyMarginDp * metrics.density).toInt()
    val rowsToScan = minOf(sampleSize, cells.size)

    return IntArray(columnCount) { col ->
        var widest = 0
        for (r in 0 until rowsToScan) {
            val row = cells[r]
            if (col >= row.size) continue
            val w = ceil(paint.measureText(row[col])).toInt()
            if (w > widest) widest = w
        }
        (widest + horizontalPaddingPx + safetyPx).coerceIn(minPx, maxPx)
    }
}
