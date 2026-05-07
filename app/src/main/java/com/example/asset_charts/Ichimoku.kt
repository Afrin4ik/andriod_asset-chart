package com.example.asset_charts

import kotlin.math.max

data class IchimokuData(
    val tenkanSen: List<Double>,
    val kijunSen: List<Double>,
    val senkouSpanA: List<Double>,
    val senkouSpanB: List<Double>,
    val chikouSpan: List<Double>
)

fun calculateIchimoku(candles: List<Candle>): IchimokuData {
    val tenkanSen = calculateLine(candles, 9)
    val kijunSen = calculateLine(candles, 26)

    val senkouSpanA = (tenkanSen zip kijunSen).map { (t, k) -> (t + k) / 2 }
        .shiftForward(26)

    val senkouSpanB = calculateLine(candles, 52)
        .shiftForward(26)

    val chikouSpan = candles.map { it.close }.shiftBackward(26)

    return IchimokuData(
        tenkanSen,
        kijunSen,
        senkouSpanA,
        senkouSpanB,
        chikouSpan
    )
}

private fun calculateLine(candles: List<Candle>, period: Int): List<Double> {
    return candles.indices.map { i ->
        val start = max(0, i - period + 1)
        val subList = candles.subList(start, i + 1)
        val max = subList.maxOf { it.high }
        val min = subList.minOf { it.low }
        (max + min) / 2.0
    }
}

private fun <T> List<T>.shiftForward(shift: Int): List<T> {
    return if (size <= shift) emptyList()
    else dropLast(shift) + List(shift) { last() }
}

private fun <T> List<T>.shiftBackward(shift: Int): List<T> {
    if (isEmpty()) {
        return emptyList()
    }
    return List(shift) { first() } + dropLast(shift)
}