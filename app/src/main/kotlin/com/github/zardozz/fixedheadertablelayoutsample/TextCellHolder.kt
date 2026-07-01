package com.github.zardozz.fixedheadertablelayoutsample

import android.view.View
import android.widget.TextView
import io.github.jbailey2010.fixedheadertable.CellViewHolder

class TextCellHolder(view: View) : CellViewHolder(view) {
    val textView: TextView = view.findViewById(R.id.cell_text)
}
