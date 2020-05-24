package com.github.spartusch.rateretriever.rate.v1.provider

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.net.MalformedURLException
import java.net.URI
import java.util.Locale

class StringExtensionsTest {

    // toBigDecimal

    @ParameterizedTest
    @CsvSource(
            "de: 123456789,1234",
            "it: 123.456.789,1234",
            "en: 123,456,789.1234",
            delimiter = ':'
    )
    fun toBigDecimal_parsesDifferentLocales(locale: Locale, amount: String) {
        val convertedAmount = amount.toBigDecimal(locale)
        assertThat(convertedAmount).isEqualByComparingTo("123456789.1234")
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid!", "@1"])
    @EmptySource
    fun toBigDecimal_throwsExceptionIfInputIsInvalid(input: String) {
        ThrowableAssert.catchThrowable { input.toBigDecimal(Locale.GERMANY) }
    }

    // toURI

    @Test
    fun toURI_isEqualToUriConstructor() {
        val uri = "http://www.github.com".toURI()
        assertThat(uri).isEqualTo(URI("http://www.github.com"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid", "http://does!not@work"])
    @EmptySource
    fun toURI_throwsExceptionIfStringIsInvalid(invalidUri: String) {
        ThrowableAssert.catchThrowableOfType({ invalidUri.toURI() }, MalformedURLException::class.java)
    }

}
