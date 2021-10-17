package com.github.spartusch.rateretriever.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class TickerSymbolTest {
    private companion object {
        @JvmStatic
        fun otherObjectsProvider() = listOf(
            Arguments.of(TickerSymbol("FOO"), true),
            Arguments.of(TickerSymbol("foo"), true),
            Arguments.of(TickerSymbol("FoO"), true),
            Arguments.of(TickerSymbol("F-O-O"), false),
            Arguments.of(TickerSymbol("BAR"), false),
            Arguments.of(TickerSymbol(""), false),
            Arguments.of(null, false),
            Arguments.of(42, false),
        )
    }

    private val cut = TickerSymbol("FOO")

    @Test
    fun testToString() {
        assertThat(cut.toString())
            .isEqualTo("FOO")
    }

    @ParameterizedTest
    @MethodSource("otherObjectsProvider")
    fun testEquals(otherObject: Any?, expectedMatch: Boolean) {
        assertThat(cut == otherObject)
            .isEqualTo(expectedMatch)
    }

    @ParameterizedTest
    @MethodSource("otherObjectsProvider")
    fun testHashCode(otherObject: Any?, expectedMatch: Boolean) {
        assertThat(cut.hashCode() == otherObject?.hashCode())
            .isEqualTo(expectedMatch)
    }
}
