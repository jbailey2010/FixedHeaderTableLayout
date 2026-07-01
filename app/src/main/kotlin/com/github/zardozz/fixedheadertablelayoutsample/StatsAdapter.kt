package com.github.zardozz.fixedheadertablelayoutsample

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.jbailey2010.fixedheadertable.CellViewHolder
import com.github.jbailey2010.fixedheadertable.FixedHeaderTableAdapter

/**
 * Generic 2D-text adapter used by all three sample activities.
 *
 * @param data    rows of pre-formatted strings (includes header row + label column)
 * @param widthFor function returning the pixel width for a given column index. Pulled
 *                each bind so callers can back it by a `SharedColumnWidths` or any
 *                other dynamic source.
 */
class StatsAdapter(
    private val data: List<List<String>>,
    private val widthFor: (Int) -> Int,
    private val rowHeightPx: Int,
    override val fixedRowCount: Int = 1,
    override val fixedColumnCount: Int = 1,
    private val interactive: Boolean = false,
) : FixedHeaderTableAdapter() {

    override val rowCount: Int get() = data.size
    override val columnCount: Int get() = data.firstOrNull()?.size ?: 0

    override fun getColumnWidth(col: Int): Int = widthFor(col)
    override fun getRowHeight(row: Int): Int = rowHeightPx

    private val selected = mutableSetOf<Long>()
    private fun key(r: Int, c: Int): Long =
        (r.toLong() shl 32) or (c.toLong() and 0xFFFFFFFFL)

    fun toggleSelection(row: Int, col: Int) {
        val k = key(row, col)
        if (!selected.remove(k)) selected.add(k)
        notifyCellChanged(row, col)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cell_text, parent, false)
        val holder = TextCellHolder(view)
        if (interactive) {
            view.setOnClickListener {
                val r = holder.row
                val c = holder.column
                if (r != CellViewHolder.NO_POSITION && c != CellViewHolder.NO_POSITION) {
                    toggleSelection(r, c)
                }
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: CellViewHolder, row: Int, col: Int) {
        val cell = holder as TextCellHolder
        cell.textView.text = data[row].getOrElse(col) { "" }

        val isHeader = row < fixedRowCount || col < fixedColumnCount
        cell.textView.setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)

        val highlighted = key(row, col) in selected
        cell.textView.setBackgroundResource(
            if (highlighted) R.drawable.selected_border else R.drawable.list_border,
        )
    }
}
