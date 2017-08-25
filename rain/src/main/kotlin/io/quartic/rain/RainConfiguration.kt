package io.quartic.rain

import io.quartic.common.application.ConfigurationBase
import java.net.URI

data class RainConfiguration(
    val howlWatchUrl: URI,
    val howlUrl: URI
) : ConfigurationBase()
