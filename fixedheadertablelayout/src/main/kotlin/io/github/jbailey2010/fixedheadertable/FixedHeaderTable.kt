/*
 * MIT License
 *
 * Copyright (c) 2021 Andrew Beck
 * Copyright (c) 2026 Jeff Bailey (fork)
 */

package io.github.jbailey2010.fixedheadertable

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jbailey2010.fixedheadertable.internal.HorizontalScrollCoordinator
import io.github.jbailey2010.fixedheadertable.internal.TableRegionAdapter
import io.github.jbailey2010.fixedheadertable.internal.TableRowViewHolder
import io.github.jbailey2010.fixedheadertable.internal.VerticalScrollCoordinator

/**
 * A scrollable 2D table with a configurable number of pinned header rows (top) and pinned
 * header columns (left). Cells are recycled via `RecyclerView`, so very large grids (10k+
 * rows, 20+ wide columns) are tractable in memory and on layout time.
 *
 * Usage:
 *
 * ```kotlin
 * val table = findViewById<FixedHeaderTable>(R.id.table)
 * table.adapter = MyTableAdapter(stats)
 * ```
 *
 * Multi-table use (several tables with aligned columns): see [setRecycledViewPool] and
 * the upcoming `SharedColumnWidths` (Phase 1d).
 */
class FixedHeaderTable @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cellPool: RecyclerView.RecycledViewPool = RecyclerView.RecycledViewPool()

    private val cornerRv = makeRegionRv()
    private val columnHeaderRv = makeRegionRv()
    private val rowHeaderRv = makeRegionRv()
    private val bodyRv = makeRegionRv()

    private val verticalCoordinator = VerticalScrollCoordinator()
    private val horizontalCoordinator = HorizontalScrollCoordinator()

    private val rowAttachListener = object : RecyclerView.OnChildAttachStateChangeListener {
        override fun onChildViewAttachedToWindow(view: View) {
            if (view is RecyclerView) horizontalCoordinator.register(view)
        }
        override fun onChildViewDetachedFromWindow(view: View) {
            if (view is RecyclerView) horizontalCoordinator.unregister(view)
        }
    }

    init {
        addView(cornerRv)
        addView(columnHeaderRv)
        addView(rowHeaderRv)
        addView(bodyRv)
        setRegionsVisible(false)

        verticalCoordinator.bind(bodyRv, rowHeaderRv)
        // Every inner cell-row RV that appears inside body or column-header registers
        // with the horizontal coordinator on attach and unregisters on detach.
        bodyRv.addOnChildAttachStateChangeListener(rowAttachListener)
        columnHeaderRv.addOnChildAttachStateChangeListener(rowAttachListener)
    }

    private fun makeRegionRv(): RecyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        itemAnimator = null
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    private fun setRegionsVisible(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        cornerRv.visibility = v
        columnHeaderRv.visibility = v
        rowHeaderRv.visibility = v
        bodyRv.visibility = v
    }

    private var _adapter: FixedHeaderTableAdapter? = null

    var adapter: FixedHeaderTableAdapter?
        get() = _adapter
        set(value) {
            if (value === _adapter) return

            // Snapshot vertical scroll before the new region adapters replace the
            // old ones — RecyclerView zeros its internal position on setAdapter, so
            // we have to restore vertical explicitly. Horizontal is preserved
            // automatically via HorizontalScrollCoordinator.sharedOffset, which is
            // a table-level value the region-level adapter swap doesn't touch;
            // newly-attached row RVs snap to it when they register.
            val hadAdapter = _adapter != null
            val carryV = if (hadAdapter) bodyRv.computeVerticalScrollOffset() else 0

            _adapter?.unregisterObserver(adapterObserver)
            _adapter = value
            value?.registerObserver(adapterObserver)

            if (value == null) {
                // Clearing — wipe horizontal state so subsequent fresh sets start at 0.
                horizontalCoordinator.reset()
            }

            rebuild()

            if (hadAdapter && value != null && carryV > 0) {
                pendingVerticalOffset = carryV
            }
            applyPendingScrollWhenReady()
        }

    // Scroll offsets stashed by onRestoreInstanceState until adapter + layout exist.
    private var pendingVerticalOffset: Int = 0
    private var pendingHorizontalOffset: Int = 0

    /**
     * Replace the internal cell pool. Multiple [FixedHeaderTable] instances sharing the
     * same pool will recycle cell views across each other — useful when several
     * column-aligned tables (e.g. period-by-period game stats) sit on one screen.
     */
    fun setRecycledViewPool(pool: RecyclerView.RecycledViewPool) {
        if (pool === cellPool) return
        cellPool = pool
        if (_adapter != null) rebuild()
    }

    private val adapterObserver = object : FixedHeaderTableAdapter.Observer {
        override fun onDataSetChanged() { rebuild() }

        override fun onCellChanged(row: Int, col: Int) {
            val a = _adapter ?: return
            val fr = a.fixedRowCount
            val fc = a.fixedColumnCount
            val rv: RecyclerView
            val rowInRegion: Int
            val colInRegion: Int
            when {
                row < fr && col < fc -> { rv = cornerRv; rowInRegion = row; colInRegion = col }
                row < fr -> { rv = columnHeaderRv; rowInRegion = row; colInRegion = col - fc }
                col < fc -> { rv = rowHeaderRv; rowInRegion = row - fr; colInRegion = col }
                else -> { rv = bodyRv; rowInRegion = row - fr; colInRegion = col - fc }
            }
            val holder = rv.findViewHolderForAdapterPosition(rowInRegion) as? TableRowViewHolder
            holder?.rebindCell(colInRegion)
        }

        override fun onRowChanged(row: Int) {
            val a = _adapter ?: return
            val fr = a.fixedRowCount
            val fc = a.fixedColumnCount
            val inHeader = row < fr
            val rowInRegion = if (inHeader) row else row - fr
            // Row updates touch both the row-header region and the body/col-header region.
            val regionA = if (inHeader) cornerRv else rowHeaderRv
            val regionB = if (inHeader) columnHeaderRv else bodyRv
            (regionA.findViewHolderForAdapterPosition(rowInRegion) as? TableRowViewHolder)?.rebindAllCells()
            (regionB.findViewHolderForAdapterPosition(rowInRegion) as? TableRowViewHolder)?.rebindAllCells()
            // Column widths might have changed too if row height changed, but height is per-row;
            // a width change is column-scope (onColumnChanged) so we don't relayout the row here.
        }

        override fun onColumnChanged(col: Int) {
            val a = _adapter ?: return
            val fc = a.fixedColumnCount
            val inHeaderCol = col < fc
            val colInRegion = if (inHeaderCol) col else col - fc
            val regionA = if (inHeaderCol) cornerRv else columnHeaderRv
            val regionB = if (inHeaderCol) rowHeaderRv else bodyRv
            for (rv in arrayOf(regionA, regionB)) {
                for (i in 0 until rv.childCount) {
                    val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? TableRowViewHolder
                    holder?.rebindCell(colInRegion)
                }
            }
        }
    }

    private fun rebuild() {
        val a = _adapter
        if (a == null) {
            cornerRv.adapter = null
            columnHeaderRv.adapter = null
            rowHeaderRv.adapter = null
            bodyRv.adapter = null
            setRegionsVisible(false)
            requestLayout()
            return
        }

        val fr = a.fixedRowCount
        val fc = a.fixedColumnCount
        // Deliberately not resetting horizontalCoordinator here: the adapter setter
        // does it explicitly when it wants a fresh start (adapter → null). For
        // same-shape swaps, sharedOffset persists and newly-attached inner row RVs
        // snap to it in HorizontalScrollCoordinator.register().

        cornerRv.adapter = TableRegionAdapter(a, 0, fr, 0, fc, cellPool)
        columnHeaderRv.adapter = TableRegionAdapter(a, 0, fr, fc, a.columnCount, cellPool)
        rowHeaderRv.adapter = TableRegionAdapter(a, fr, a.rowCount, 0, fc, cellPool)
        bodyRv.adapter = TableRegionAdapter(a, fr, a.rowCount, fc, a.columnCount, cellPool)

        setRegionsVisible(true)
        requestLayout()
    }

    private fun cornerWidth(): Int {
        val a = _adapter ?: return 0
        var w = 0
        for (c in 0 until a.fixedColumnCount) w += a.getColumnWidth(c)
        return w
    }

    private fun cornerHeight(): Int {
        val a = _adapter ?: return 0
        var h = 0
        for (r in 0 until a.fixedRowCount) h += a.getRowHeight(r)
        return h
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

        if (_adapter == null) return

        val cw = cornerWidth()
        val ch = cornerHeight()
        val remW = (width - cw).coerceAtLeast(0)
        val remH = (height - ch).coerceAtLeast(0)

        cornerRv.measure(exactly(cw), exactly(ch))
        columnHeaderRv.measure(exactly(remW), exactly(ch))
        rowHeaderRv.measure(exactly(cw), exactly(remH))
        bodyRv.measure(exactly(remW), exactly(remH))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (_adapter == null) return

        val width = r - l
        val height = b - t
        val cw = cornerWidth()
        val ch = cornerHeight()

        cornerRv.layout(0, 0, cw, ch)
        columnHeaderRv.layout(cw, 0, width, ch)
        rowHeaderRv.layout(0, ch, cw, height)
        bodyRv.layout(cw, ch, width, height)
    }

    private fun exactly(size: Int) = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)

    // --- saved state ------------------------------------------------------------------
    //
    // The view must have an android:id for the framework to track its instance state
    // across config changes. Without an ID, scroll position resets on rotation — that's
    // an Android-level constraint, not a library one.

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(
            superState,
            verticalOffset = bodyRv.computeVerticalScrollOffset(),
            horizontalOffset = horizontalCoordinator.currentOffset,
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        pendingVerticalOffset = state.verticalOffset
        pendingHorizontalOffset = state.horizontalOffset
        applyPendingScrollWhenReady()
    }

    private fun applyPendingScrollWhenReady() {
        if (_adapter == null) return
        if (pendingVerticalOffset == 0 && pendingHorizontalOffset == 0) return

        val vTarget = pendingVerticalOffset
        val hTarget = pendingHorizontalOffset
        pendingVerticalOffset = 0
        pendingHorizontalOffset = 0

        // The new TableRegionAdapters were just assigned in rebuild(); their internal
        // RVs lay out on the next frame. Post so the body's children (and their inner
        // row RVs) are attached and registered with the horizontal coordinator.
        bodyRv.post {
            if (vTarget != 0) {
                val current = bodyRv.computeVerticalScrollOffset()
                bodyRv.scrollBy(0, vTarget - current)
            }
            if (hTarget != 0) {
                horizontalCoordinator.scrollToOffset(hTarget)
            }
        }
    }

    private class SavedState : BaseSavedState {
        val verticalOffset: Int
        val horizontalOffset: Int

        constructor(superState: Parcelable?, verticalOffset: Int, horizontalOffset: Int) :
            super(superState) {
            this.verticalOffset = verticalOffset
            this.horizontalOffset = horizontalOffset
        }

        constructor(source: Parcel) : super(source) {
            verticalOffset = source.readInt()
            horizontalOffset = source.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(verticalOffset)
            out.writeInt(horizontalOffset)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}
