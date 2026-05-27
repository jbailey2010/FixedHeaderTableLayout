/*
 * MIT License
 *
 * Copyright (c) 2021 Andrew Beck
 * Copyright (c) 2026 Jeff Bailey (fork)
 */

package com.github.jbailey2010.fixedheadertable

import android.view.ViewGroup

/**
 * Adapter contract for [FixedHeaderTable].
 *
 * The table is modelled as a 2D grid of cells. The top [fixedRowCount] rows stay pinned
 * at the top when the user scrolls vertically; the left [fixedColumnCount] columns stay
 * pinned at the left when the user scrolls horizontally. The intersection (top-left
 * corner) is doubly pinned.
 *
 * Column widths and row heights are **declared** by the adapter rather than measured.
 * This is intentional: measuring every cell to derive widths is O(rows × columns) and
 * defeats the recycling that makes large tables viable. If you need auto-fit behavior,
 * compute widths once from your data model and return them from [getColumnWidth].
 */
abstract class FixedHeaderTableAdapter {

    /** Total number of rows, including the [fixedRowCount] pinned header rows. */
    abstract val rowCount: Int

    /** Total number of columns, including the [fixedColumnCount] pinned header columns. */
    abstract val columnCount: Int

    /** Number of top rows that stay pinned during vertical scrolling. Default 1. */
    open val fixedRowCount: Int = 1

    /** Number of left columns that stay pinned during horizontal scrolling. Default 1. */
    open val fixedColumnCount: Int = 1

    /** Width in pixels for [col]. Called once per visible column; cache as needed. */
    abstract fun getColumnWidth(col: Int): Int

    /** Height in pixels for [row]. Called once per visible row; cache as needed. */
    abstract fun getRowHeight(row: Int): Int

    /**
     * Distinguishes cell layouts. Cells with the same view type can be recycled
     * across positions. Default is `0` (all cells share one layout).
     */
    open fun getCellViewType(row: Int, col: Int): Int = 0

    abstract fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder

    abstract fun onBindViewHolder(holder: CellViewHolder, row: Int, col: Int)

    // --- change notification ---

    private val observers = mutableListOf<Observer>()

    internal fun registerObserver(observer: Observer) {
        if (observer !in observers) observers += observer
    }

    internal fun unregisterObserver(observer: Observer) {
        observers -= observer
    }

    /** Full rebind. Use sparingly — prefer the targeted variants below. */
    fun notifyDataSetChanged() {
        observers.forEach { it.onDataSetChanged() }
    }

    /** Single cell changed. */
    fun notifyCellChanged(row: Int, col: Int) {
        observers.forEach { it.onCellChanged(row, col) }
    }

    /** Every cell in [row] changed. */
    fun notifyRowChanged(row: Int) {
        observers.forEach { it.onRowChanged(row) }
    }

    /** Every cell in [col] changed. */
    fun notifyColumnChanged(col: Int) {
        observers.forEach { it.onColumnChanged(col) }
    }

    internal interface Observer {
        fun onDataSetChanged()
        fun onCellChanged(row: Int, col: Int)
        fun onRowChanged(row: Int)
        fun onColumnChanged(col: Int)
    }
}
