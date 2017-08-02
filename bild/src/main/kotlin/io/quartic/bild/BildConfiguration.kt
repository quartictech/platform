package io.quartic.bild

import io.fabric8.kubernetes.api.model.Job
import io.quartic.common.application.ConfigurationBase

data class BildConfiguration(
    val template: Job
) : ConfigurationBase()

