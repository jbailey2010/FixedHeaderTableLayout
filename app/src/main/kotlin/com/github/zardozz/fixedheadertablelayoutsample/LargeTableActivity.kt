package com.github.zardozz.fixedheadertablelayoutsample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.jbailey2010.fixedheadertable.FixedHeaderTable

/**
 * Stress test: 5000 rows × 20 columns. With the recycling RecyclerView architecture this
 * should scroll smoothly with only the visible cells live in memory. The old
 * `FixedHeaderTableLayout` would have inflated 100,000 cell views up-front.
 */
class LargeTableActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_large_table)

        val rows = 5_000
        val cols = 20

        val data: List<List<String>> = buildList(rows + 1) {
            add(buildList(cols + 1) {
                add("Row")
                for (c in 1..cols) add("Stat $c")
            })
            for (r in 1..rows) {
                add(buildList(cols + 1) {
                    add("R$r")
                    for (c in 1..cols) add("%d.%02d".format(r * c % 100, (r + c) % 100))
                })
            }
        }

        val rowLabelWidth = 64.dp(this)
        val cellWidth = 72.dp(this)
        val widths = IntArray(cols + 1) { if (it == 0) rowLabelWidth else cellWidth }

        val adapter = StatsAdapter(
            data = data,
            widthFor = { col -> widths[col] },
            rowHeightPx = 44.dp(this),
            fixedRowCount = 1,
            fixedColumnCount = 1,
            interactive = false,
        )

        findViewById<FixedHeaderTable>(R.id.table).adapter = adapter
        findViewById<TextView>(R.id.info).text = "$rows rows × $cols columns — scroll freely"
    }
}
