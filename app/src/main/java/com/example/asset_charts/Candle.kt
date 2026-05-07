package com.example.asset_charts

import ru.tinkoff.piapi.contract.v1.HistoricCandle
import ru.tinkoff.piapi.core.utils.MapperUtils

// Этот класс служит адаптером между форматом данных Tinkoff Invest API и более простым внутренним представлением свечи в приложении
// Он упрощает работу с данными, преобразуя сложные структуры API в простые числовые значения

data class Candle(
    var open: Double = 0.0,
    var close: Double = 0.0,
    var low: Double = 0.0,
    var high: Double = 0.0,
    var volume: Long = 0L,
    var time: Long = 0L
)
{
    constructor(hc: HistoricCandle) : this( // Преобразует данные из формата Tinkoff API в простые числовые значения
        MapperUtils.quotationToBigDecimal(hc.open).toDouble(), //  для конвертации специального формата цен API в BigDecimal, а затем в Double
        MapperUtils.quotationToBigDecimal(hc.close).toDouble(),
        MapperUtils.quotationToBigDecimal(hc.low).toDouble(),
        MapperUtils.quotationToBigDecimal(hc.high).toDouble(),
        hc.volume, // берет значение напрямую
        hc.time.seconds // берет секунды из временной метки
    )
}