package com.github.jbailey2010.fixedheadertable

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FixedHeaderTableTest {

    private class TinyAdapter : FixedHeaderTableAdapter() {
        override val rowCount = 4
        override val columnCount = 3
        override fun getColumnWidth(col: Int) = 100
        override fun getRowHeight(row: Int) = 80
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder =
            object : CellViewHolder(TextView(parent.context)) {}
        override fun onBindViewHolder(holder: CellViewHolder, row: Int, col: Int) {
            (holder.itemView as TextView).text = "R${row}C${col}"
        }
    }

    private fun measureAndLayout(v: View, w: Int = 600, h: Int = 400) {
        v.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        v.layout(0, 0, w, h)
    }

    @Test
    fun adapterStoresAndClears() {
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val table = FixedHeaderTable(activity)
                assertNull(table.adapter)

                val a = TinyAdapter()
                table.adapter = a
                assertSame(a, table.adapter)

                table.adapter = null
                assertNull(table.adapter)
            }
        }
    }

    @Test
    fun measureAndLayoutSucceedsWithAdapter() {
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val table = FixedHeaderTable(activity)
                val container = FrameLayout(activity)
                container.addView(
                    table,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
                table.adapter = TinyAdapter()

                measureAndLayout(container)
                assertTrue("table should have non-zero width after layout", table.width > 0)
                assertTrue("table should have non-zero height after layout", table.height > 0)
            }
        }
    }

    @Test
    fun sharedColumnWidthsNotifiesAndUpdates() {
        val shared = SharedColumnWidths(intArrayOf(50, 60, 70))
        assertEquals(60, shared.get(1))

        var fired: Pair<Int, Int>? = null
        shared.addListener { col, newWidth -> fired = col to newWidth }
        shared.set(1, 90)
        assertEquals(90, shared.get(1))
        assertEquals(1 to 90, fired)

        // Setting the same value is a no-op.
        fired = null
        shared.set(1, 90)
        assertNull(fired)
    }
}
