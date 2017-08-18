package io.quartic.eval

import io.quartic.common.application.ConfigurationBase
import java.net.URI

data class EvalConfiguration(
    val registryUrl: URI,
    val qubeUrl: URI
) : ConfigurationBase()


