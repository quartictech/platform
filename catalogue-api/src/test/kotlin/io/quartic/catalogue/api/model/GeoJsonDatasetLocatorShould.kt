package io.quartic.catalogue.api.model

import io.quartic.catalogue.api.model.GeoJsonDatasetLocator

class GeoJsonDatasetLocatorShould : DatasetLocatorTests<GeoJsonDatasetLocator>() {
    override fun locator() = GeoJsonDatasetLocator("http://wat")

    override fun json() = "{\"type\": \"geojson\", \"url\": \"http://wat\"}"
}
