package com.github.spartusch.rateretriever.domain.model

import org.javamoney.moneta.Money
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Locale
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount
import javax.money.NumberValue

data class Rate(private val value: MonetaryAmount) {

    constructor(amount: BigDecimal, currency: CurrencyUnit) : this(Money.of(amount, currency))

    val number: NumberValue get() = value.number
    val currency: CurrencyUnit get() = value.currency

    fun format(locale: Locale, fractionDigits: Int): String {
        val numberFormat = DecimalFormat.getInstance(locale)
        numberFormat.minimumFractionDigits = fractionDigits
        return numberFormat.format(value.number)
    }
}
