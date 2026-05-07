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

// Настройка внешнего вида графика
private fun CombinedChart.setupChart() {
    description.isEnabled = false // Убираем описание внизу графика
    setTouchEnabled(true) // Разрешаем взаимодействие (тач, зум, скролл)
    isDragEnabled = true // Разрешаем прокрутку графика
    setScaleEnabled(true) // Разрешаем масштабировать
    setPinchZoom(true) // Разрешаем "щипок" двумя пальцами

    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM // Ось X внизу графика
        granularity = 1f // Шаг между метками
        labelCount = 6 // Кол-во подписей по оси X
    }

    axisRight.isEnabled = false // Убираем правую ось Y (оставляем только левую)
    legend.isEnabled = true // Показываем легенду (подписи к линиям)
}

private fun createLineDataSet(
    values: List<Double>, // список чисел, которые пойдут на ось Y
    label: String, // подпись для этой линии (в легенде)
    color: Int // цвет линии (в формате ARGB)
): LineDataSet {
    val entries = values.mapIndexedNotNull { index, value -> // Преобразуем каждый элемент списка в точку на графике
        if (value.isNaN()) null else Entry(index.toFloat(), value.toFloat())
    } // Теперь entries — это List<Entry>, где Entry(x: Float, y: Float)
    return LineDataSet(entries, label).apply { // Настраивается стиль
        setColor(color) // задаём цвет линии
        lineWidth = 1.5f // толщина линии в пикселях
        setDrawCircles(false) // не рисовать точки‑круги на каждом значении
        setDrawValues(false) // не рисовать подписи значений рядом с точками
    }
}

// Ищем сигналы на покупку и продажу. Здесь реализуется простейшая логика сигналов Ишимоку
private fun calculateSignals(tenkan: List<Double>, kijun: List<Double>, candles: List<Candle>): Pair<List<Entry>, List<Entry>> {
    val buyEntries = mutableListOf<Entry>()
    val sellEntries = mutableListOf<Entry>()

    for (i in 1 until tenkan.size) {
        val prevT = tenkan[i-1]
        val prevK = kijun[i-1]
        val currT = tenkan[i]
        val currK = kijun[i]

        // Если линия Tenkan пересекает Kijun снизу вверх — это сигнал на покупку
        if (currT > currK && prevT <= prevK) {
            buyEntries.add(Entry(i.toFloat(), candles[i].low.toFloat()))
        }

        // Если сверху вниз — сигнал на продажу
        if (currT < currK && prevT >= prevK) {
            sellEntries.add(Entry(i.toFloat(), candles[i].high.toFloat()))
        }
    }
    return Pair(buyEntries, sellEntries)
}

// Главная функция отрисовки свечей
fun drawCandles(
    candles: List<Candle>, // список свечей
    chart: CombinedChart // график, на который нужно всё отрисовать
) {
//    val chart = findViewById<CombinedChart>(R.id.chart) // - закоментил тк были конфликты

    // Configure chart
    chart.setupChart()

    // Строим свечной график
    val candleEntries = candles.mapIndexed { index, candle ->
        CandleEntry(
            index.toFloat(),
            candle.high.toFloat(),
            candle.low.toFloat(),
            candle.open.toFloat(),
            candle.close.toFloat()
        )
    }
    val candleDataSet = CandleDataSet(candleEntries, "Candles").apply { // Настраивается стиль
        setColors(Color.BLACK, Color.GRAY)
        shadowColor = Color.DKGRAY
        shadowWidth = 0.8f
        decreasingPaintStyle = Paint.Style.FILL
        increasingPaintStyle = Paint.Style.FILL
        neutralColor = Color.BLUE
        decreasingColor = Color.RED
        increasingColor = Color.rgb(122, 242, 84)
    }

    // Вычисляем Ишимоку
    val ichimoku = calculateIchimoku(candles)

    // Рисуем линии Ишимоку
    val tenkanDataSet = createLineDataSet(ichimoku.tenkanSen, "Tenkan-sen", Color.RED)
    val kijunDataSet = createLineDataSet(ichimoku.kijunSen, "Kijun-sen", Color.BLUE)
    val senkouADataSet = createLineDataSet(ichimoku.senkouSpanA, "Senkou A", Color.YELLOW)
    val senkouBDataSet = createLineDataSet(ichimoku.senkouSpanB, "Senkou B", Color.MAGENTA)
    val chikouDataSet = createLineDataSet(ichimoku.chikouSpan, "Chikou", Color.GREEN)

    // Рисуем облако Kumo
    val kumoEntries = ichimoku.senkouSpanA.zip(ichimoku.senkouSpanB).mapIndexed { index, (a, b) ->
        Entry(index.toFloat(), a.toFloat(), b.toFloat())
    }
    val kumoDataSet = LineDataSet(kumoEntries, "Kumo").apply { // Настраивается стиль
        setDrawCircles(false) // Отключает отображение кружков в точках данных
        setDrawValues(false) // Скрывает числовые значения у точек
        setDrawFilled(true) // Включает заливку под линией
        fillColor = Color.argb(50, 0, 100, 80) // Цвет заливки (полупрозрачный зеленоватый)
        mode = LineDataSet.Mode.LINEAR // Режим интерполяции между точками
        lineWidth = 0f // Полностью скрывает саму линию (остается только заливка)
        fillFormatter = IFillFormatter { dataSet, _ -> dataSet.getEntryForIndex(dataSet.entryCount - 1).y } // Форматтер заливки. Определяет, до какого уровня заполнять область - в данном случае до последней точки набора данных
    }

    // Сигналы
    val (buyEntries, sellEntries) = calculateSignals(ichimoku.tenkanSen, ichimoku.kijunSen, candles)
    val buyDataSet = ScatterDataSet(buyEntries, "Buy").apply { // Настраивается стиль
        setColors(Color.GREEN)  // Устанавливает зеленый цвет для точек покупки
        scatterShapeSize = 12f  // Размер точек (12 пикселей)
        shapeRenderer = CircleShapeRenderer()  // Рендерит точки как кружки
    }
    val sellDataSet = ScatterDataSet(sellEntries, "Sell").apply { // Настраивается стиль
        setColors(Color.RED)
        scatterShapeSize = 12f
        shapeRenderer = CircleShapeRenderer()
    }

    // Объединяем всё в combinedData
    val combinedData = CombinedData().apply {
        setData(CandleData(candleDataSet)) // Свечи
        setData(LineData( // Линии
            tenkanDataSet,
            kijunDataSet,
            senkouADataSet,
            senkouBDataSet,
            chikouDataSet,
            kumoDataSet
        ))
        setData(ScatterData(buyDataSet, sellDataSet)) // Точки
    }

    chart.data = combinedData // передаём данные в компонент графика
    chart.invalidate() // перерисовывает себя
}