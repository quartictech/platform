package io.quartic.catalogue.api.model

class GeoJsonDatasetLocatorShould : DatasetLocatorTests<DatasetLocator.GeoJsonDatasetLocator>() {
    override fun locator() = DatasetLocator.GeoJsonDatasetLocator("http://wat")

    override fun json() = "{\"type\": \"geojson\", \"url\": \"http://wat\"}"
}
