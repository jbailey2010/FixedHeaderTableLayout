/*
 * MIT License
 *
 * Copyright (c) 2021 Andrew Beck
 * Copyright (c) 2026 Jeff Bailey (fork)
 */

package com.github.jbailey2010.fixedheadertable

/**
 * A mutable, listenable list of column widths intended to be shared across multiple
 * [FixedHeaderTableAdapter] instances so several `FixedHeaderTable`s on screen stay
 * column-aligned.
 *
 * Typical wiring in an adapter:
 *
 * ```kotlin
 * class MyAdapter(private val widths: SharedColumnWidths) : FixedHeaderTableAdapter() {
 *     init {
 *         widths.addListener { col, _ -> notifyColumnChanged(col) }
 *     }
 *     override fun getColumnWidth(col: Int) = widths.get(col)
 *     // ...
 * }
 * ```
 *
 * For cell-view recycling across the same set of tables, also call
 * `FixedHeaderTable.setRecycledViewPool(pool)` on each instance with a shared
 * `RecyclerView.RecycledViewPool`.
 */
class SharedColumnWidths(initial: IntArray) {

    private val widths: IntArray = initial.copyOf()
    private val listeners = mutableListOf<Listener>()

    val size: Int get() = widths.size

    fun get(col: Int): Int = widths[col]

    /** Update one column's width. No-op if the value is unchanged. */
    fun set(col: Int, width: Int) {
        if (widths[col] == width) return
        widths[col] = width
        // Snapshot the listener list so listeners removing themselves during dispatch is safe.
        listeners.toList().forEach { it.onColumnWidthChanged(col, width) }
    }

    /** Update every column at once. Width count must match [size]. */
    fun setAll(newWidths: IntArray) {
        require(newWidths.size == widths.size) {
            "Width count (${newWidths.size}) does not match shared size ($size)"
        }
        for (i in newWidths.indices) set(i, newWidths[i])
    }

    fun addListener(listener: Listener) {
        if (listener !in listeners) listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    fun interface Listener {
        fun onColumnWidthChanged(col: Int, newWidth: Int)
    }
}
