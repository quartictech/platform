package io.quartic.mgmt.auth

import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.catalogue.api.model.DatasetNamespace.Companion.NAMESPACE_ANY
import io.quartic.common.auth.User

class NamespaceAuthoriser(private val mappings: Multimap<User, DatasetNamespace>) {

    fun authorisedFor(user: User, namespace: DatasetNamespace): Boolean {
        val namespaces = mappings[user] ?: return false
        return namespaces.contains(namespace) || namespaces.contains(NAMESPACE_ANY)
    }
}