package com.github.spartusch.rateretriever.utils

import com.github.spartusch.rateretriever.domain.model.Rate
import org.javamoney.moneta.Money
import java.math.BigDecimal
import javax.money.CurrencyUnit
import javax.money.Monetary

fun rate(value: String, currency: CurrencyUnit) =
    Rate(Money.of(BigDecimal(value), currency))

fun rate(value: String, currency: String) =
    Rate(Money.of(BigDecimal(value), Monetary.getCurrency(currency)))
