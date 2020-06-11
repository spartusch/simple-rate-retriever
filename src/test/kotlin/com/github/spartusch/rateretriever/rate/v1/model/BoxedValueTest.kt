package com.github.spartusch.rateretriever.rate.v1.model

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

private class BoxedInt(i: Int) : BoxedValue<Int>(i)

private open class BoxedString(s: String) : BoxedValue<String>(s)
private class DerivedBoxedString(s: String) : BoxedString(s)
private class OtherBoxedString(s: String) : BoxedValue<String>(s)

class BoxedValueTest {

    @Test
    fun equals_twoBoxedStringsAreEqual() {
        val s1 = BoxedString("1")
        val s2 = BoxedString("1")
        assertThat(s1).isEqualTo(s2)
        assertThat(s2).isEqualTo(s1)
    }

    @Test
    fun equals_boxedValuesAreNotEqualToDerivedValues() {
        val s1 = BoxedString("1")
        val s2 = DerivedBoxedString("1")
        assertThat(s1).isNotEqualTo(s2) // a BoxedString is not a DerivedBoxedString
    }

    @Test
    fun equals_derivedValuesAreEqualToSuperValues() {
        val s1 = BoxedString("1")
        val s2 = DerivedBoxedString("1")
        assertThat(s2).isEqualTo(s1) // a DerivedBoxedString is a BoxedString
    }

    @Test
    fun equals_unrelatedBoxValueTypesAreNotEqualEvenIfValueInsideIsEqual() {
        val s1 = BoxedString("1")
        val s2 = OtherBoxedString("1")
        assertThat(s1).isNotEqualTo(s2)
        assertThat(s2).isNotEqualTo(s1)
    }

    @Test
    fun equals_unrelatedBoxValueTypesAreNotEqual() {
        val s = BoxedString("1")
        val i = BoxedInt(1)
        assertThat(s).isNotEqualTo(i)
        assertThat(i).isNotEqualTo(s)
    }

    @Test
    fun equals_twoBoxedIntAreEqual() {
        val i1 = BoxedInt(1)
        val i2 = BoxedInt(1)
        assertThat(i1).isEqualTo(i2)
        assertThat(i2).isEqualTo(i1)
    }

    @Test
    fun equals_boxedValuesAreNotEqualIfValuesInsideAreNotEqual() {
        val i1 = BoxedInt(1)
        val i2 = BoxedInt(2)
        assertThat(i1).isNotEqualTo(i2)
        assertThat(i2).isNotEqualTo(i1)
    }

    @Test
    fun equals_boxedValuesAreEqualToTheBoxedValuesInside() {
        assertThat(BoxedInt(1)).isEqualTo(1)
        assertThat(BoxedString("1")).isEqualTo("1")
    }

    @Test
    fun equals_doesNotEqualNull() {
        assertThat(BoxedInt(1)).isNotEqualTo(null)
    }

    @Test
    fun equals_equalsItself() {
        val s = BoxedString("1")
        assertThat(s).isEqualTo(s)
    }

    @Test
    fun equals_doesNotEqualOtherTypes() {
        val i = BoxedInt(1)
        assertThat(i).isNotEqualTo("1")
    }

    @Test
    fun toString_returnsValueInsideAsString() {
        val s = BoxedString("1")
        val i = BoxedInt(1)
        assertThat(s.toString()).isEqualTo("1")
        assertThat(i.toString()).isEqualTo("1")
    }

    @Test
    fun hashCode_isHashCodeOfValueInside() {
        val s = BoxedString("1")
        val i = BoxedInt(1)
        assertThat(s.hashCode()).isEqualTo("1".hashCode())
        assertThat(i.hashCode()).isEqualTo(1.hashCode())
    }
}
