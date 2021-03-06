package com.github.spartusch.rateretriever.rate.v1.provider

import java.math.BigDecimal
import java.net.URI
import java.text.NumberFormat
import java.util.Locale

internal fun String.toBigDecimal(locale: Locale): BigDecimal {
    val numberFormat = NumberFormat.getNumberInstance(locale)
    return BigDecimal(numberFormat.parse(this).toString())
}

internal fun String.toURI() = URI(this)
