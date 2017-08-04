package io.quartic.glisten

import io.quartic.common.application.ConfigurationBase

data class GlistenConfiguration(
    val secretToken: String
) : ConfigurationBase()


