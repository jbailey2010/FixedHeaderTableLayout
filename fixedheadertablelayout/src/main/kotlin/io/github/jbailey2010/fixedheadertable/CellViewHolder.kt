/*
 * MIT License
 *
 * Copyright (c) 2021 Andrew Beck
 * Copyright (c) 2026 Jeff Bailey (fork)
 */

package io.github.jbailey2010.fixedheadertable

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Base class for cell holders. Extends `RecyclerView.ViewHolder` so internal recycling
 * machinery can use it directly. Subclasses are expected to cache references to their
 * cell's child views (e.g. TextView, ImageView) and apply data in
 * [FixedHeaderTableAdapter.onBindViewHolder].
 *
 * The current bound coordinates are exposed via [row] and [column]. Both are
 * [NO_POSITION] when the holder is unbound (recycled and waiting for re-bind).
 */
abstract class CellViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    /** Row index this holder is currently bound to, or [NO_POSITION]. */
    var row: Int = NO_POSITION
        internal set

    /** Column index this holder is currently bound to, or [NO_POSITION]. */
    var column: Int = NO_POSITION
        internal set

    companion object {
        const val NO_POSITION: Int = -1
    }
}
