package io.quartic.bild

import io.dropwizard.Configuration
import io.fabric8.kubernetes.api.model.Job

data class BildConfiguration(
    val template: Job
) : Configuration()

