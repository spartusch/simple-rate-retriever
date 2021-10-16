package com.github.spartusch.rateretriever.domain.model

data class TickerSymbol(private val value: String) {
    override fun toString() = value
}
