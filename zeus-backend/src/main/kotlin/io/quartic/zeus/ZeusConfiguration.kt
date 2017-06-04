package io.quartic.zeus

import io.dropwizard.Configuration
import io.quartic.zeus.model.DatasetName
import java.net.URL

data class ZeusConfiguration(
        val datasets: Map<DatasetName, DataProviderConfiguration>
) : Configuration()

data class DataProviderConfiguration(
        val prettyName: String,
        val url: URL,
        val indexedAttributes: Set<String> = emptySet()
)