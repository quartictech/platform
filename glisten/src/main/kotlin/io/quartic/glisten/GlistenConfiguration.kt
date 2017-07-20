package io.quartic.glisten

import io.dropwizard.Configuration
import io.quartic.glisten.model.Registration

data class GlistenConfiguration(
    val registrations: Map<String, Registration>
) : Configuration()


