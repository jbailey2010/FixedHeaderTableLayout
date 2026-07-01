package com.github.zardozz.fixedheadertablelayoutsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.jbailey2010.fixedheadertable.FixedHeaderTable

/**
 * Small interactive table: 12 rows × 8 columns of fake player stats with one fixed
 * header row and one fixed header column (the player name). Tapping a body cell
 * toggles a highlight — exercises the fine-grained `notifyCellChanged` path so the
 * scroll position must stay put on every tap.
 */
class BasicTableActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_table)

        val headers = listOf("Player", "Pos", "GP", "G", "A", "PIM", "+/-", "TOI")
        val players = listOf(
            "Crosby" to listOf("C", "82", "31", "62", "30", "+8", "20:12"),
            "Malkin" to listOf("C", "78", "27", "50", "44", "-3", "19:30"),
            "Letang" to listOf("D", "80", "10", "31", "55", "+12", "23:45"),
            "Rust"   to listOf("R", "76", "24", "28", "20", "+5", "18:02"),
            "Guentzel" to listOf("L", "81", "36", "37", "18", "+15", "18:48"),
            "Karlsson" to listOf("D", "74", "13", "37", "28", "-9", "24:11"),
            "Reilly" to listOf("C", "79", "14", "22", "12", "+1", "15:33"),
            "Acciari" to listOf("C", "82", "18", "11", "26", "+7", "13:21"),
            "O'Connor" to listOf("L", "70", "16", "22", "10", "+2", "14:55"),
            "Smith"  to listOf("R", "60", "8", "11", "18", "-4", "12:08"),
            "Eller"  to listOf("C", "72", "9", "13", "24", "-6", "13:42"),
        )

        val data: List<List<String>> = buildList {
            add(headers)
            for ((name, stats) in players) add(listOf(name) + stats)
        }

        val widths = intArrayOf(
            120.dp(this), 60.dp(this), 60.dp(this), 60.dp(this),
            60.dp(this), 70.dp(this), 60.dp(this), 80.dp(this),
        )

        val adapter = StatsAdapter(
            data = data,
            widthFor = { col -> widths[col] },
            rowHeightPx = 48.dp(this),
            fixedRowCount = 1,
            fixedColumnCount = 1,
            interactive = true,
        )

        findViewById<FixedHeaderTable>(R.id.table).adapter = adapter
    }
}
