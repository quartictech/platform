package io.quartic.mgmt.auth

import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.catalogue.api.model.DatasetNamespace.Companion.NAMESPACE_ANY
import io.quartic.common.auth.User
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NamespaceAuthoriserShould {
    @Test
    fun allow_namespace_if_present() {
        val authoriser = NamespaceAuthoriser(mapOf(User("oliver") to setOf(DatasetNamespace("foo"))))

        assertTrue(authoriser.authorisedFor(User("oliver"), DatasetNamespace("foo")))
    }

    @Test
    fun disallow_namespace_if_not_present() {
        val authoriser = NamespaceAuthoriser(mapOf(User("oliver") to setOf(DatasetNamespace("foo"))))

        assertFalse(authoriser.authorisedFor(User("oliver"), DatasetNamespace("bar")))
    }

    @Test
    fun allow_any_if_wildcard_present() {
        val authoriser = NamespaceAuthoriser(mapOf(User("oliver") to setOf(NAMESPACE_ANY)))

        assertTrue(authoriser.authorisedFor(User("oliver"), DatasetNamespace("foo")))
    }

    @Test
    fun disallow_if_user_not_present() {
        val authoriser = NamespaceAuthoriser(mapOf(User("oliver") to setOf(DatasetNamespace("foo"))))

        assertFalse(authoriser.authorisedFor(User("alex"), DatasetNamespace("foo")))
    }
}
