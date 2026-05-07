package com.example.asset_charts

import ru.tinkoff.piapi.contract.v1.CandleInterval

data class Request(
    var figi: String = "",
    var interval: CandleInterval = CandleInterval.CANDLE_INTERVAL_DAY
)