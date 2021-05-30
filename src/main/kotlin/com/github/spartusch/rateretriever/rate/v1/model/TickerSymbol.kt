package com.github.spartusch.rateretriever.rate.v1.model

data class TickerSymbol(private val value: String) {
    override fun toString() = value
}
