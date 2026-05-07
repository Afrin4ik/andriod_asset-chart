package com.example.asset_charts

import android.graphics.Color
import android.graphics.Paint
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.renderer.scatter.CircleShapeRenderer

private fun CombinedChart.setupChart() {
    description.isEnabled = false
    setTouchEnabled(true)
    isDragEnabled = true
    setScaleEnabled(true)
    setPinchZoom(true)

    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        granularity = 1f
        labelCount = 6
    }

    axisRight.isEnabled = false
    legend.isEnabled = true
}

private fun createLineDataSet(
    values: List<Double>, // список чисел, которые пойдут на ось Y
    label: String, // подпись для этой линии (в легенде)
    color: Int // цвет линии (в формате ARGB)
): LineDataSet {
    val entries = values.mapIndexedNotNull { index, value ->
        if (value.isNaN()) null else Entry(index.toFloat(), value.toFloat())
    }
    return LineDataSet(entries, label).apply {
        setColor(color)
        lineWidth = 1.5f
        setDrawCircles(false)
        setDrawValues(false)
    }
}

private fun calculateSignals(tenkan: List<Double>, kijun: List<Double>, candles: List<Candle>): Pair<List<Entry>, List<Entry>> {
    val buyEntries = mutableListOf<Entry>()
    val sellEntries = mutableListOf<Entry>()

    for (i in 1 until tenkan.size) {
        val prevT = tenkan[i-1]
        val prevK = kijun[i-1]
        val currT = tenkan[i]
        val currK = kijun[i]

        if (currT > currK && prevT <= prevK) {
            buyEntries.add(Entry(i.toFloat(), candles[i].low.toFloat()))
        }

        if (currT < currK && prevT >= prevK) {
            sellEntries.add(Entry(i.toFloat(), candles[i].high.toFloat()))
        }
    }
    return Pair(buyEntries, sellEntries)
}

fun drawCandles(
    candles: List<Candle>,
    chart: CombinedChart
) {
    chart.setupChart()

    val candleEntries = candles.mapIndexed { index, candle ->
        CandleEntry(
            index.toFloat(),
            candle.high.toFloat(),
            candle.low.toFloat(),
            candle.open.toFloat(),
            candle.close.toFloat()
        )
    }
    val candleDataSet = CandleDataSet(candleEntries, "Candles").apply {
        setColors(Color.BLACK, Color.GRAY)
        shadowColor = Color.DKGRAY
        shadowWidth = 0.8f
        decreasingPaintStyle = Paint.Style.FILL
        increasingPaintStyle = Paint.Style.FILL
        neutralColor = Color.BLUE
        decreasingColor = Color.RED
        increasingColor = Color.rgb(122, 242, 84)
    }

    val ichimoku = calculateIchimoku(candles)

    val tenkanDataSet = createLineDataSet(ichimoku.tenkanSen, "Tenkan-sen", Color.RED)
    val kijunDataSet = createLineDataSet(ichimoku.kijunSen, "Kijun-sen", Color.BLUE)
    val senkouADataSet = createLineDataSet(ichimoku.senkouSpanA, "Senkou A", Color.YELLOW)
    val senkouBDataSet = createLineDataSet(ichimoku.senkouSpanB, "Senkou B", Color.MAGENTA)
    val chikouDataSet = createLineDataSet(ichimoku.chikouSpan, "Chikou", Color.GREEN)

    val kumoEntries = ichimoku.senkouSpanA.zip(ichimoku.senkouSpanB).mapIndexed { index, (a, b) ->
        Entry(index.toFloat(), a.toFloat(), b.toFloat())
    }
    val kumoDataSet = LineDataSet(kumoEntries, "Kumo").apply {
        setDrawCircles(false)
        setDrawValues(false)
        setDrawFilled(true)
        fillColor = Color.argb(50, 0, 100, 80)
        mode = LineDataSet.Mode.LINEAR
        lineWidth = 0f
        fillFormatter = IFillFormatter { dataSet, _ -> dataSet.getEntryForIndex(dataSet.entryCount - 1).y }
    }

    val (buyEntries, sellEntries) = calculateSignals(ichimoku.tenkanSen, ichimoku.kijunSen, candles)
    val buyDataSet = ScatterDataSet(buyEntries, "Buy").apply {
        setColors(Color.GREEN)
        scatterShapeSize = 12f
        shapeRenderer = CircleShapeRenderer()
    }
    val sellDataSet = ScatterDataSet(sellEntries, "Sell").apply {
        setColors(Color.RED)
        scatterShapeSize = 12f
        shapeRenderer = CircleShapeRenderer()
    }

    val combinedData = CombinedData().apply {
        setData(CandleData(candleDataSet))
        setData(LineData(
            tenkanDataSet,
            kijunDataSet,
            senkouADataSet,
            senkouBDataSet,
            chikouDataSet,
            kumoDataSet
        ))
        setData(ScatterData(buyDataSet, sellDataSet))
    }

    chart.data = combinedData
    chart.invalidate()
}