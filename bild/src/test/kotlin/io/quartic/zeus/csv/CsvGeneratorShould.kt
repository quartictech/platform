package io.quartic.zeus.csv

import io.quartic.bild.csv.CsvGenerator
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class CsvGeneratorShould {
    @Test
    fun output_basic_csv() {
        val data = listOf(
                mapOf("a" to "yeah", "b" to 3),
                mapOf("a" to "no", "b" to 47)
        )

        assertThat(CsvGenerator().generateFor(data), equalTo("""
            a,b
            yeah,3
            no,47
        """.nicely()))
    }

    @Test
    fun handle_null_values() {
        val data = listOf(
                mapOf("a" to "yeah", "b" to 3),
                mapOf("a" to "no", "b" to null)
        )

        assertThat(CsvGenerator().generateFor(data), equalTo("""
            a,b
            yeah,3
            no,
        """.nicely()))
    }

    @Test
    fun treat_missing_values_as_null() {
        val data = listOf(
                mapOf("a" to "yeah", "b" to 3),
                mapOf("a" to "no")
        )

        assertThat(CsvGenerator().generateFor(data), equalTo("""
            a,b
            yeah,3
            no,
        """.nicely()))
    }

    @Test
    fun skip_unrecognised_keys() {
        val data = listOf(
                mapOf("a" to "yeah", "b" to 3),
                mapOf("a" to "no", "b" to 47, "c" to "who am I?")
        )

        assertThat(CsvGenerator().generateFor(data), equalTo("""
            a,b
            yeah,3
            no,47
        """.nicely()))
    }

    @Test
    fun munge_lists_into_string() {
        val data = listOf(
                mapOf("a" to "yeah", "b" to listOf("x", "y", "a space"))
        )

        assertThat(CsvGenerator().generateFor(data), equalTo("""
            a,b
            yeah,"x;y;a space"
        """.nicely()))
    }

    @Test
    fun treat_map_values_as_null() {
        val data = listOf(
                mapOf("a" to "yeah", "b" to mapOf("x" to 42, "y" to "a space"))
        )

        assertThat(CsvGenerator().generateFor(data), equalTo("""
            a,b
            yeah,
        """.nicely()))
    }

    @Test
    fun produce_empty_csv_if_no_data() {
        assertThat(CsvGenerator().generateFor(emptyList()), equalTo("\n"))
    }

    private fun String.nicely() = trimIndent() + "\n"
}

