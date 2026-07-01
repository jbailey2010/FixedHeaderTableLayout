/*
 * MIT License
 *
 * Copyright (c) 2021 Andrew Beck
 * Copyright (c) 2026 Jeff Bailey (fork)
 */

package com.github.jbailey2010.fixedheadertable.internal

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Bidirectional vertical scroll sync between the body and row-header outer RVs.
 *
 * Both regions use vertical `LinearLayoutManager`s with the same row range and row
 * heights, so identical `scrollBy(0, dy)` delivers visually identical positions.
 *
 * Bidirectional means the user can drag on either region to scroll both — handy when
 * the body is full of small cells and the row header has more grabbable surface.
 */
internal class VerticalScrollCoordinator {

    private var propagating = false

    fun bind(a: RecyclerView, b: RecyclerView) {
        a.addOnScrollListener(Forward(b))
        b.addOnScrollListener(Forward(a))
    }

    private inner class Forward(private val target: RecyclerView) : RecyclerView.OnScrollListener() {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            if (dy == 0 || propagating) return
            propagating = true
            target.scrollBy(0, dy)
            propagating = false
        }
    }
}

/**
 * Coordinates horizontal scrolling across a dynamic set of inner `RecyclerView`s — the
 * row-level cell RVs inside the body and column-header regions.
 *
 * Rows are recycled as the user scrolls vertically, so the set of "currently attached"
 * inner RVs changes constantly. The outer regions hook child attach/detach events to
 * [register] and [unregister] their row RVs here.
 *
 * The coordinator tracks a single [sharedOffset] in pixels. When any registered RV
 * reports a scroll delta, the delta is added to the offset and forwarded to every other
 * registered RV. When a new RV is registered, it is snapped to the current offset so
 * newly-scrolled-into-view rows align with the rest of the table.
 */
internal class HorizontalScrollCoordinator {

    private val registered = mutableSetOf<RecyclerView>()
    private var sharedOffset: Int = 0
    private var propagating: Boolean = false

    private val sharedListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            if (dx == 0 || propagating) return
            sharedOffset += dx
            propagating = true
            for (other in registered) {
                if (other === rv) continue
                other.scrollBy(dx, 0)
            }
            propagating = false
        }
    }

    fun register(rv: RecyclerView) {
        if (registered.add(rv)) {
            rv.addOnScrollListener(sharedListener)
            applyCurrentOffset(rv)
        }
    }

    fun unregister(rv: RecyclerView) {
        if (registered.remove(rv)) {
            rv.removeOnScrollListener(sharedListener)
        }
    }

    /** Current shared horizontal offset in pixels. */
    val currentOffset: Int get() = sharedOffset

    /**
     * Scroll every registered RV to [target] pixels from origin, and update the shared
     * offset. Used to restore saved scroll state after configuration changes.
     */
    fun scrollToOffset(target: Int) {
        val delta = target - sharedOffset
        if (delta == 0) return
        sharedOffset = target
        propagating = true
        for (rv in registered) rv.scrollBy(delta, 0)
        propagating = false
    }

    /**
     * Resets the shared offset to 0 and scrolls every registered RV back to its origin.
     * Call when the table adapter changes — the new dataset has fresh column geometry,
     * so the previous offset is meaningless.
     */
    fun reset() {
        sharedOffset = 0
        propagating = true
        for (rv in registered) {
            val cur = rv.computeHorizontalScrollOffset()
            if (cur != 0) rv.scrollBy(-cur, 0)
        }
        propagating = false
    }

    private fun applyCurrentOffset(rv: RecyclerView) {
        val sync = {
            val target = sharedOffset
            val cur = rv.computeHorizontalScrollOffset()
            if (cur != target) {
                propagating = true
                rv.scrollBy(target - cur, 0)
                propagating = false
            }
        }
        if (rv.isLaidOut && rv.childCount > 0) {
            sync()
        } else {
            // A freshly-attached row RV hasn't been given its cell adapter yet at
            // register time (bind() runs after attach). Waiting on a plain post can
            // fire before cells are laid out, making scrollBy a no-op. Hook into
            // the actual layout pass so we sync exactly once the row has cells.
            rv.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View, l: Int, t: Int, r: Int, b: Int,
                    oldL: Int, oldT: Int, oldR: Int, oldB: Int,
                ) {
                    val recycler = v as RecyclerView
                    if (recycler.width > 0 && recycler.childCount > 0) {
                        recycler.removeOnLayoutChangeListener(this)
                        sync()
                    }
                }
            })
        }
    }
}
