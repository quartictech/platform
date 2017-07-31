package io.quartic.mgmt

import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.application.ConfigurationBase
import io.quartic.mgmt.auth.Multimap
import io.quartic.common.auth.User

data class GithubConfiguration(
   val clientId: String,
   val clientSecret: String,
   val allowedOrganisations: Set<String>,
   val trampolineUrl: String,
   val useSecureCookies: Boolean,
   val scopes: List<String>,
   val redirectHost: String
)

class MgmtConfiguration : ConfigurationBase() {
    var catalogueUrl: String? = null
    var howlUrl: String? = null
    var authorisedNamespaces: Multimap<User, DatasetNamespace> = emptyMap()
    var github: GithubConfiguration? = null
    val tokenTimeToLiveMinutes: Int = 60
}
