/*
 * MIT License
 *
 * Copyright (c) 2021 Andrew Beck
 * Copyright (c) 2026 Jeff Bailey (fork)
 */

package io.github.jbailey2010.fixedheadertable.internal

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jbailey2010.fixedheadertable.CellViewHolder
import io.github.jbailey2010.fixedheadertable.FixedHeaderTableAdapter

/**
 * Drives one of the four table regions (corner / column-header / row-header / body).
 *
 * The outer adapter has one item per row in the region. Each row is itself a horizontal
 * `RecyclerView` whose [RowCellAdapter] binds the cells for the region's column range.
 * All four region RVs share a single [RecyclerView.RecycledViewPool] so cell views move
 * freely between rows and between regions.
 */
internal class TableRegionAdapter(
    private val tableAdapter: FixedHeaderTableAdapter,
    private val rowStart: Int,
    private val rowEnd: Int,
    private val colStart: Int,
    private val colEnd: Int,
    private val cellPool: RecyclerView.RecycledViewPool,
) : RecyclerView.Adapter<TableRowViewHolder>() {

    override fun getItemCount(): Int = rowEnd - rowStart

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableRowViewHolder {
        val rv = RecyclerView(parent.context).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setRecycledViewPool(cellPool)
            itemAnimator = null
            overScrollMode = View.OVER_SCROLL_NEVER
            // Height is set per-row in TableRowViewHolder.bind(); start with 0.
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                0,
            )
        }
        return TableRowViewHolder(rv, tableAdapter, colStart, colEnd)
    }

    override fun onBindViewHolder(holder: TableRowViewHolder, position: Int) {
        holder.bind(rowStart + position)
    }
}

internal class TableRowViewHolder(
    private val rowRv: RecyclerView,
    private val tableAdapter: FixedHeaderTableAdapter,
    private val colStart: Int,
    private val colEnd: Int,
) : RecyclerView.ViewHolder(rowRv) {

    private var cellAdapter: RowCellAdapter? = null

    /** Rebind a single cell at [positionInRow] (relative to this region's column range). */
    fun rebindCell(positionInRow: Int) {
        cellAdapter?.notifyItemChanged(positionInRow)
    }

    /** Rebind every cell in this row. */
    fun rebindAllCells() {
        cellAdapter?.notifyItemRangeChanged(0, cellAdapter!!.itemCount)
    }

    fun bind(absRow: Int) {
        val rowHeight = tableAdapter.getRowHeight(absRow)
        val lp = rowRv.layoutParams
        if (lp.height != rowHeight) {
            lp.height = rowHeight
            rowRv.layoutParams = lp
        }

        val existing = cellAdapter
        if (existing == null) {
            val created = RowCellAdapter(absRow, tableAdapter, colStart, colEnd)
            cellAdapter = created
            rowRv.adapter = created
        } else {
            existing.rebindRow(absRow)
        }
    }
}

/**
 * Inner adapter: one item per cell in the row's column range. Asks the table adapter
 * to create and bind each cell, applying the column-declared width on bind.
 */
internal class RowCellAdapter(
    private var rowIndex: Int,
    private val tableAdapter: FixedHeaderTableAdapter,
    private val colStart: Int,
    private val colEnd: Int,
) : RecyclerView.Adapter<CellViewHolder>() {

    fun rebindRow(row: Int) {
        rowIndex = row
        notifyItemRangeChanged(0, itemCount)
    }

    override fun getItemCount(): Int = colEnd - colStart

    override fun getItemViewType(position: Int): Int =
        tableAdapter.getCellViewType(rowIndex, colStart + position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val holder = tableAdapter.onCreateViewHolder(parent, viewType)
        // RecyclerView requires its own LayoutParams type on children. If the adapter
        // gave us something else (or nothing), wrap to a fresh RV LayoutParams. Width
        // is set per-cell on bind; height fills the row.
        val current = holder.itemView.layoutParams
        if (current !is RecyclerView.LayoutParams) {
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.MATCH_PARENT,
            )
        }
        return holder
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val col = colStart + position
        holder.row = rowIndex
        holder.column = col

        val width = tableAdapter.getColumnWidth(col)
        val lp = holder.itemView.layoutParams
        if (lp.width != width) {
            lp.width = width
            holder.itemView.layoutParams = lp
        }

        tableAdapter.onBindViewHolder(holder, rowIndex, col)
    }
}
