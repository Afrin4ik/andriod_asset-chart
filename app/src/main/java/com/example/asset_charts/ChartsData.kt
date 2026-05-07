package com.example.asset_charts

// Здесь находятся данные о доступных активах и таймфрэймов

import ru.tinkoff.piapi.contract.v1.CandleInterval

object ChartsData { // Синглтон (object)

    // Доступные активы
    val FIGIs = mapOf(
        "sber" to "BBG004730N88", // сбер
        "ibm" to "BBG000BLNNH6", // IBM
        "appl" to "BBG000B9XRY4", // apple
        "gazp" to "BBG004730RP0", // газпром
        "sibn" to "BBG004S684M6", // газпром нефть
        "tatn" to "BBG004RVFFC0", // татнефть
        "rosn" to "BBG004731354", // роснефть
        "nvtk" to "BBG00475KKY8", // новатэк
        "lkoh" to "BBG004731032", // лукойл
        "rnft" to "BBG00F9XX7H4", // русснефть
        "plzl" to "BBG000R607Y3", // полюс золото
        "alrs" to "BBG004S68B31", // АЛРОСА
        "fesh" to "BBG000QF1Q17" // ДВМП
    )

    // Доступные таймфрэймы
    val TimeFrame = mapOf(
        "1h" to CandleInterval.CANDLE_INTERVAL_HOUR,
        "4h" to CandleInterval.CANDLE_INTERVAL_4_HOUR,
        "1d" to CandleInterval.CANDLE_INTERVAL_DAY,
        "1w" to CandleInterval.CANDLE_INTERVAL_WEEK,
        "1M" to CandleInterval.CANDLE_INTERVAL_MONTH
    )
}
