package io.quartic.glisten

import io.quartic.common.application.ConfigurationBase
import java.net.URL

data class GlistenConfiguration(
    val bildUrl: URL,
    val secretToken: String
) : ConfigurationBase()


