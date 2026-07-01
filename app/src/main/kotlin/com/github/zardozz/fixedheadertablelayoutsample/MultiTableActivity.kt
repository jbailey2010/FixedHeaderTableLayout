package com.github.zardozz.fixedheadertablelayoutsample

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import io.github.jbailey2010.fixedheadertable.FixedHeaderTable
import io.github.jbailey2010.fixedheadertable.SharedColumnWidths

/**
 * Two `FixedHeaderTable`s stacked vertically that share both a `SharedColumnWidths`
 * (so column widths stay aligned) and a `RecyclerView.RecycledViewPool` (so cell views
 * recycle across both tables). Tapping the button widens column 1 in the shared widths;
 * both tables update simultaneously.
 */
class MultiTableActivity : AppCompatActivity() {

    private lateinit var shared: SharedColumnWidths
    private lateinit var topAdapter: StatsAdapter
    private lateinit var bottomAdapter: StatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_table)

        val headers = listOf("Player", "G", "A", "P", "S")
        val period1 = listOf(
            listOf("Crosby", "1", "2", "3", "6"),
            listOf("Malkin", "0", "1", "1", "4"),
            listOf("Rust",   "1", "0", "1", "3"),
            listOf("Letang", "0", "1", "1", "2"),
            listOf("Guentzel", "1", "1", "2", "5"),
        )
        val period2 = listOf(
            listOf("Crosby", "0", "1", "1", "3"),
            listOf("Malkin", "2", "0", "2", "5"),
            listOf("Rust",   "0", "1", "1", "2"),
            listOf("Letang", "1", "0", "1", "3"),
            listOf("Guentzel", "0", "2", "2", "4"),
        )

        shared = SharedColumnWidths(
            intArrayOf(120.dp(this), 56.dp(this), 56.dp(this), 56.dp(this), 56.dp(this)),
        )
        val sharedPool = RecyclerView.RecycledViewPool()

        topAdapter = makeAdapter(buildList { add(headers); addAll(period1) })
        bottomAdapter = makeAdapter(buildList { add(headers); addAll(period2) })

        // One listener pushes width changes to both adapters.
        shared.addListener { col, _ ->
            topAdapter.notifyColumnChanged(col)
            bottomAdapter.notifyColumnChanged(col)
        }

        with(findViewById<FixedHeaderTable>(R.id.table_top)) {
            setRecycledViewPool(sharedPool)
            adapter = topAdapter
        }
        with(findViewById<FixedHeaderTable>(R.id.table_bottom)) {
            setRecycledViewPool(sharedPool)
            adapter = bottomAdapter
        }

        findViewById<Button>(R.id.btn_widen).setOnClickListener {
            shared.set(1, shared.get(1) + 8.dp(this))
        }
    }

    private fun makeAdapter(data: List<List<String>>) = StatsAdapter(
        data = data,
        widthFor = { col -> shared.get(col) },
        rowHeightPx = 44.dp(this),
        fixedRowCount = 1,
        fixedColumnCount = 1,
        interactive = false,
    )
}
