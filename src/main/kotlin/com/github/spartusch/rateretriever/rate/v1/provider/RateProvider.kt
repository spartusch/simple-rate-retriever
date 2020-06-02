package com.github.spartusch.rateretriever.rate.v1.provider

import java.math.BigDecimal

interface RateProvider {
    fun getProviderId(): String
    fun getCurrentRate(symbol: String, currencyCode: String): BigDecimal?
    fun isCurrencyCodeSupported(currencyCode: String): Boolean
}
