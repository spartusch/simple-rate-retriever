package com.github.spartusch.rateretriever.rate.v1.model

import kotlin.reflect.full.cast
import kotlin.reflect.full.isSubclassOf

open class BoxedValue<T : Any> (protected val value: T) {
    override fun toString() = value.toString()

    override fun equals(
        other: Any?
    ) = (other != null && this::class.isSubclassOf(other::class) && value == BoxedValue::class.cast(other).value) ||
        (other != null && value::class.isSubclassOf(other::class) && value == other)

    override fun hashCode() = value.hashCode()
}
