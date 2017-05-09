package io.quartic.weyl

import io.dropwizard.setup.Environment
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetLocator
import io.quartic.common.client.userAgentFor
import io.quartic.common.websocket.WebsocketClientSessionFactory
import io.quartic.common.websocket.WebsocketListener
import io.quartic.weyl.core.attributes.AttributesFactory
import io.quartic.weyl.core.feature.FeatureConverter
import io.quartic.weyl.core.source.GeoJsonSource
import io.quartic.weyl.core.source.PostgresSource
import io.quartic.weyl.core.source.Source
import io.quartic.weyl.core.source.WebsocketSource

data class WeylSourceFactory(
        val configuration: WeylConfiguration,
        val environment: Environment,
        val websocketFactory: WebsocketClientSessionFactory) {

    companion object {
        val MIME_TYPE = "application/geojson"
    }

    fun geojsonSource(config: DatasetConfig, url: String) = GeoJsonSource(
            config.metadata.name,
            url,
            userAgentFor(WeylApplication::class.java),
            FeatureConverter(AttributesFactory())
    )

    fun websocketSource(config: DatasetConfig, listenerFactory: WebsocketListener.Factory,
                        indexable: Boolean) =
            WebsocketSource(
                    config.metadata.name,
                    FeatureConverter(AttributesFactory()),
                    environment.metrics(),
                    listenerFactory,
                    indexable
            )

    fun liveOrStaticGeojson(dataset: DatasetConfig, locatorPath: String, streaming: Boolean) =
            if (streaming) {
                websocketSource(dataset,
                        WebsocketListener.Factory(configuration.rainWsUrlRoot + locatorPath, websocketFactory),
                        true)
            } else {
                geojsonSource(dataset, configuration.howlStorageUrl + locatorPath)
            }

    fun createSource(dataset: DatasetConfig): Source? {
        val locator = dataset.locator
        return when (locator) {
            is DatasetLocator.PostgresDatasetLocator -> PostgresSource(
                    dataset.metadata.name,
                    locator,
                    AttributesFactory())
            is DatasetLocator.GeoJsonDatasetLocator -> geojsonSource(dataset, locator.url)
            is DatasetLocator.WebsocketDatasetLocator -> websocketSource(dataset,
                    WebsocketListener.Factory(locator.url, websocketFactory), false)
            is DatasetLocator.CloudGeoJsonDatasetLocator ->
                // TODO: can remove the geojsonSource variant once we've regularised the Rain path
                liveOrStaticGeojson(dataset, locator.path, locator.streaming)
            is DatasetLocator.CloudDatasetLocator ->
                if (locator.mimeType == MIME_TYPE) {
                    liveOrStaticGeojson(dataset, locator.path, locator.streaming)
                }
                else null
            else -> null
        }
    }
}