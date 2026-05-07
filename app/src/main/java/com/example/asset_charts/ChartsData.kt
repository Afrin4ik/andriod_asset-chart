package com.example.asset_charts

import ru.tinkoff.piapi.contract.v1.CandleInterval

object ChartsData {
    val FIGIs = mapOf(
        "sber" to "BBG004730N88",
        "ibm" to "BBG000BLNNH6",
        "appl" to "BBG000B9XRY4",
        "gazp" to "BBG004730RP0",
        "sibn" to "BBG004S684M6",
        "tatn" to "BBG004RVFFC0",
        "rosn" to "BBG004731354",
        "nvtk" to "BBG00475KKY8",
        "lkoh" to "BBG004731032",
        "rnft" to "BBG00F9XX7H4",
        "plzl" to "BBG000R607Y3",
        "alrs" to "BBG004S68B31",
        "fesh" to "BBG000QF1Q17"
    )

    val TimeFrame = mapOf(
        "1h" to CandleInterval.CANDLE_INTERVAL_HOUR,
        "4h" to CandleInterval.CANDLE_INTERVAL_4_HOUR,
        "1d" to CandleInterval.CANDLE_INTERVAL_DAY,
        "1w" to CandleInterval.CANDLE_INTERVAL_WEEK,
        "1M" to CandleInterval.CANDLE_INTERVAL_MONTH
    )
}
