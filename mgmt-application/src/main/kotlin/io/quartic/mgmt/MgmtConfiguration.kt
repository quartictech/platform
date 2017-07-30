package io.quartic.mgmt

import io.dropwizard.Configuration
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.application.ConfigurationBase
import io.quartic.mgmt.auth.Multimap
import io.quartic.common.auth.User

class MgmtConfiguration : ConfigurationBase() {
    var catalogueUrl: String? = null
    var howlUrl: String? = null
    var authorisedNamespaces: Multimap<User, DatasetNamespace> = emptyMap()
}
