package io.quartic.zeus

import io.quartic.common.application.ConfigurationBase
import io.quartic.zeus.model.DatasetName
import java.net.URL

data class ZeusConfiguration(
    val datasets: Map<DatasetName, DataProviderConfiguration>
) : ConfigurationBase()

data class DataProviderConfiguration(
    val prettyName: String,
    val url: URL,
    val indexedAttributes: Set<String> = emptySet()
)
