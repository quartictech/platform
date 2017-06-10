package io.quartic.weyl.core.source

import com.nhaarman.mockito_kotlin.mock
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetNamespace
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatasetAuthoriserShould {

    private val authoriser = DatasetAuthoriser(mapOf(DatasetNamespace("foo") to listOf("tagA", "tagB")))

    @Test
    fun disallow_if_namespace_not_in_rules() {
        assertFalse(authoriser.isAllowed(DatasetCoordinates("bar", "123"), mock<DatasetConfig>()))
    }

    @Test
    fun allow_if_namespace_in_rules_with_no_tag_constraints() {
        val authoriser = DatasetAuthoriser(mapOf(DatasetNamespace("foo") to emptyList()))

        assertTrue(authoriser.isAllowed(DatasetCoordinates("foo", "123"), mock<DatasetConfig>()))
    }

    @Test
    fun disallow_if_no_tags_but_namespace_rule_has_tag_constraints() {
        assertFalse(authoriser.isAllowed(DatasetCoordinates("foo", "123"), mock<DatasetConfig>()))
    }

    @Test
    fun disallow_if_tags_not_a_set_but_namespace_rule_has_tag_constraints() {
        assertFalse(authoriser.isAllowed(DatasetCoordinates("foo", "123"),
                DatasetConfig(mock(), mock(), mapOf("tags" to 56))
        ))
    }

    @Test
    fun disallow_if_no_tag_matches_tag_limitations() {
        assertFalse(authoriser.isAllowed(DatasetCoordinates("foo", "123"),
                DatasetConfig(mock(), mock(), mapOf("tags" to setOf("tagC", "tagD")))
        ))
    }

    @Test
    fun allow_if_at_least_one_tag_matches_tag_limitations() {
        assertTrue(authoriser.isAllowed(DatasetCoordinates("foo", "123"),
                DatasetConfig(mock(), mock(), mapOf("tags" to setOf("tagC", "tagB")))
        ))
    }
}