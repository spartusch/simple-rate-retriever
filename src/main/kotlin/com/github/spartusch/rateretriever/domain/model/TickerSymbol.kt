package com.github.spartusch.rateretriever.domain.model

import java.util.Locale

data class TickerSymbol(private val value: String) {
    override fun toString() = value

    private fun normalizedValue() = value.uppercase(Locale.getDefault())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return this.normalizedValue() == (other as TickerSymbol).normalizedValue()
    }

    override fun hashCode() = normalizedValue().hashCode()
}
