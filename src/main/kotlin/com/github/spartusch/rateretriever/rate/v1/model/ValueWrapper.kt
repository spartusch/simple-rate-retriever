package com.github.spartusch.rateretriever.rate.v1.model

import java.util.function.Function

interface ValueWrapper<T> {
    val value: T
    fun <R> map(f: Function<T, R>) = f.apply(value)
}
