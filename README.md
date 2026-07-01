<div align="center">
    <h2>FixedHeaderTableLayout</h2>
    <p>A recycling 2D table view for Android with pinned header rows and columns.</p>
</div>

This is a Kotlin + RecyclerView rewrite of the original
[Zardozz/FixedHeaderTableLayout](https://github.com/Zardozz/FixedHeaderTableLayout)
library. The original library inflated every cell up front and ran an O(rows × columns)
measure pass, which made very large tables (sports stats, financial grids, etc.) slow to
draw and prone to OOM crashes.

This fork's `1.0.0` is a clean re-implementation that:

- Recycles cell views via `RecyclerView` — memory is `O(visible_cells)` regardless of
  total table size. 10,000-row grids are fine.
- Takes column widths from the adapter rather than measuring every cell — no quadratic
  measure pass.
- Exposes a small Kotlin adapter API modelled on `RecyclerView.Adapter`.
- Drops the pinch-zoom feature from `0.x`. Matrix transforms and recycling don't combine
  cleanly. If you need pinch-to-zoom on a table, use a `ScaleGestureDetector` and a
  scale-aware container around this view.

## Status

`1.0.0` — first release of the fork. The API is intentionally small. Breaking changes
from `0.x` are total; see [Migrating from 0.x](#migrating-from-0x) below.

## Installation

```groovy
dependencies {
    implementation 'io.github.jbailey2010:fixedheadertablelayout:1.0.0'
}
```

Minimum SDK: 21. Kotlin not required for consumers; the API is `@JvmOverloads`-annotated
where it matters, but a Kotlin codebase will read more naturally.

## Basic usage

XML:

```xml
<io.github.jbailey2010.fixedheadertable.FixedHeaderTable
    android:id="@+id/table"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Adapter:

```kotlin
class PlayerStatsAdapter(private val rows: List<List<String>>) : FixedHeaderTableAdapter() {
    override val rowCount get() = rows.size
    override val columnCount get() = rows.first().size

    // 1 pinned header row at top; 1 pinned column (player name) at left.
    override val fixedRowCount = 1
    override val fixedColumnCount = 1

    override fun getColumnWidth(col: Int) = if (col == 0) 240 else 144   // px
    override fun getRowHeight(row: Int) = 96                              // px

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cell, parent, false)
        return TextCellHolder(view)
    }

    override fun onBindViewHolder(holder: CellViewHolder, row: Int, col: Int) {
        (holder as TextCellHolder).textView.text = rows[row][col]
    }
}

class TextCellHolder(view: View) : CellViewHolder(view) {
    val textView: TextView = view.findViewById(R.id.cell_text)
}
```

Activity:

```kotlin
findViewById<FixedHeaderTable>(R.id.table).adapter = PlayerStatsAdapter(stats)
```

## Multi-table use (column-aligned siblings)

Multiple `FixedHeaderTable` instances can share column widths via `SharedColumnWidths` and
share cell-view recycling via a single `RecyclerView.RecycledViewPool`:

```kotlin
val widths = SharedColumnWidths(intArrayOf(240, 96, 96, 96, 96))   // px
val pool = RecyclerView.RecycledViewPool()

fun makeAdapter(data: List<List<String>>) = object : FixedHeaderTableAdapter() {
    init { widths.addListener { col, _ -> notifyColumnChanged(col) } }
    override val rowCount get() = data.size
    override val columnCount get() = data.first().size
    override fun getColumnWidth(col: Int) = widths.get(col)
    override fun getRowHeight(row: Int) = 80
    // ... onCreateViewHolder / onBindViewHolder
}

table1.setRecycledViewPool(pool)
table1.adapter = makeAdapter(period1Stats)
table2.setRecycledViewPool(pool)
table2.adapter = makeAdapter(period2Stats)

// Update column 1 everywhere by calling shared.set(...).
widths.set(1, 120)
```

## Notifying changes

```kotlin
adapter.notifyDataSetChanged()       // wholesale rebuild
adapter.notifyCellChanged(row, col)  // single cell; preserves scroll
adapter.notifyRowChanged(row)        // entire row
adapter.notifyColumnChanged(col)     // entire column
```

`notifyCellChanged` is cheap and is the right call for toggle/highlight interactions —
unlike `notifyDataSetChanged` it does not reset the scroll position.

## Migrating from `0.x`

Total breaking change. There is no compatibility shim — the API has been replaced.

Conceptual mapping:

| `0.x`                                                | `1.0.0`                                                                |
| ---------------------------------------------------- | ---------------------------------------------------------------------- |
| `FixedHeaderTableLayout` (FrameLayout)               | `FixedHeaderTable` (FrameLayout)                                       |
| `addViews(main, colHeader, rowHeader, corner)`       | `adapter = MyAdapter(...)`                                             |
| Manually-built `FixedHeaderSubTableLayout` per region | Single adapter declares `fixedRowCount` / `fixedColumnCount`            |
| Manually-inflated cells in `FixedHeaderTableRow`     | `onCreateViewHolder` / `onBindViewHolder`                              |
| Implicit column widths via measure                   | Explicit per-column widths via `getColumnWidth(col)`                   |
| `fhtl_min_scale` / `fhtl_max_scale` pinch zoom       | **Removed.** No replacement in-library.                                |
| `FixedHeaderSubTableLayout` for column-aligned tables | `SharedColumnWidths` + shared `RecycledViewPool`                       |

In practice migration is: write a `FixedHeaderTableAdapter` that returns your data,
delete the four-quadrant view-building code, drop any references to pinch zoom.

## Limitations

- Column widths are declared, not measured. If you need auto-fit, compute widths from
  your data before constructing the adapter (e.g. by measuring the header row only).
- The body's row view holders keep their inner cell RVs alive across vertical recycling,
  which bounds memory at `(visible_rows + cache) × visible_cols` cell views. For very
  wide tables this is still vastly better than the original library's `rows × cols`.

## Credits

Original library by Andrew Beck ([Zardozz](https://github.com/Zardozz)), MIT licensed.
This fork by Jeff Bailey, same license.

## License

```
MIT License

Copyright (c) 2021 Andrew Beck
Copyright (c) 2026 Jeff Bailey

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
