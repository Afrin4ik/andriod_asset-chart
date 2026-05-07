package com.example.asset_charts

import ru.tinkoff.piapi.contract.v1.HistoricCandle
import ru.tinkoff.piapi.core.utils.MapperUtils

data class Candle(
    var open: Double = 0.0,
    var close: Double = 0.0,
    var low: Double = 0.0,
    var high: Double = 0.0,
    var volume: Long = 0L,
    var time: Long = 0L
)
{
    constructor(hc: HistoricCandle) : this(
        MapperUtils.quotationToBigDecimal(hc.open).toDouble(),
        MapperUtils.quotationToBigDecimal(hc.close).toDouble(),
        MapperUtils.quotationToBigDecimal(hc.low).toDouble(),
        MapperUtils.quotationToBigDecimal(hc.high).toDouble(),
        hc.volume,
        hc.time.seconds
    )
}