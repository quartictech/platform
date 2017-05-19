package io.quartic.zeus.provider

import io.quartic.zeus.model.ItemId
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

private typealias Data = Map<ItemId, Map<String, Any>>

class SelectiveMatcherShould {

    @Test
    fun return_only_items_that_match() {
        val data = mapOf(
                ItemId("123") to mapOf("a" to "x"),
                ItemId("456") to mapOf("a" to "y"),
                ItemId("789") to mapOf("a" to "x")
        )

        val matcher = SelectiveMatcher(setOf("a"), data)

        assertThat(matcher(setOf("x")), equalTo(mapOf(
                ItemId("123") to mapOf("a" to "x"),
                ItemId("789") to mapOf("a" to "x")
        ) as Data))
    }

    @Test
    fun return_items_that_match_any_of_multiple_terms() {
        val data = mapOf(
                ItemId("123") to mapOf("a" to "x"),
                ItemId("456") to mapOf("a" to "y"),
                ItemId("789") to mapOf("a" to "x")
        )

        val matcher = SelectiveMatcher(setOf("a"), data)

        assertThat(matcher(setOf("x", "y")), equalTo(mapOf(
                ItemId("123") to mapOf("a" to "x"),
                ItemId("456") to mapOf("a" to "y"),
                ItemId("789") to mapOf("a" to "x")
        ) as Data))
    }

    @Test
    fun return_items_that_match_on_any_indexed_attribute() {
        val data = mapOf(
                ItemId("123") to mapOf("a" to "x"),
                ItemId("789") to mapOf("b" to "x")
        )

        val matcher = SelectiveMatcher(setOf("a", "b"), data)

        assertThat(matcher(setOf("x")), equalTo(mapOf(
                ItemId("123") to mapOf("a" to "x"),
                ItemId("789") to mapOf("b" to "x")
        ) as Data))
    }

    @Test
    fun not_return_items_that_match_on_unindexed_attributes() {
        val data = mapOf(
                ItemId("123") to mapOf("a" to "x"),
                ItemId("789") to mapOf("c" to "x")
        )

        val matcher = SelectiveMatcher(setOf("a"), data)

        assertThat(matcher(setOf("x")), equalTo(mapOf(
                ItemId("123") to mapOf("a" to "x")
        ) as Data))
    }

    @Test
    fun return_items_that_match_on_stringified_values() {
        val data = mapOf(
                ItemId("123") to mapOf("a" to 42)
        )

        val matcher = SelectiveMatcher(setOf("a"), data)

        assertThat(matcher(setOf("42")), equalTo(mapOf(
                ItemId("123") to mapOf("a" to 42)
        ) as Data))
    }

    @Test
    fun return_items_that_match_partially() {
        val data = mapOf(
                ItemId("123") to mapOf("a" to "tube"),
                ItemId("456") to mapOf("a" to "uber"),
                ItemId("789") to mapOf("a" to "lube")
        )

        val matcher = SelectiveMatcher(setOf("a"), data)

        assertThat(matcher(setOf("ub")), equalTo(data as Data))
    }

    @Test
    fun return_items_that_match_even_with_different_case() {
        val data = mapOf(
                ItemId("123") to mapOf("a" to "TuBe")
        )

        val matcher = SelectiveMatcher(setOf("a"), data)

        assertThat(matcher(setOf("tUbE")), equalTo(data as Data))
    }
}