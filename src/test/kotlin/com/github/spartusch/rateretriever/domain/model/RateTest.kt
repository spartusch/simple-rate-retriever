package com.github.spartusch.rateretriever.domain.model

import com.github.spartusch.rateretriever.utils.rate
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Locale

class RateTest {
    @Test
    fun format_germanLocale() {
        val locale = Locale.forLanguageTag("de-DE")
        val cut = rate("1012.34", "EUR")

        val result = cut.format(locale, 6)

        Assertions.assertThat(result).isEqualTo("1.012,340000")
    }

    @Test
    fun format_usLocale() {
        val locale = Locale.forLanguageTag("en-US")
        val cut = rate("1012.34", "USD")

        val result = cut.format(locale, 6)

        Assertions.assertThat(result).isEqualTo("1,012.340000")
    }

    @Test
    fun format_fractionDigitsAreRoundedAtTheLastDigit() {
        val locale = Locale.forLanguageTag("de-DE")
        val cut = rate("12.3456789", "EUR")

        val result = cut.format(locale, 4)

        Assertions.assertThat(result).isEqualTo("12,3457")
    }
}
