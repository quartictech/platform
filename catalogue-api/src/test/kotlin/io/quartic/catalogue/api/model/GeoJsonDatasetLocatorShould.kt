package io.quartic.catalogue.api.model

class GeoJsonDatasetLocatorShould : DatasetLocatorTests<GeoJsonDatasetLocator>() {
    override fun locator() = GeoJsonDatasetLocator("http://wat")

    override fun json() = "{\"type\": \"geojson\", \"url\": \"http://wat\"}"
}
