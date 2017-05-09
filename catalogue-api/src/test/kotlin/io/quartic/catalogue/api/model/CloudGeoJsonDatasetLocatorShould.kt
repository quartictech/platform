package io.quartic.catalogue.api.model

class CloudGeoJsonDatasetLocatorShould : DatasetLocatorTests<DatasetLocator.CloudGeoJsonDatasetLocator>() {
    override fun locator() = DatasetLocator.CloudGeoJsonDatasetLocator("http://wat", false)

    override fun json() = "{\"type\": \"cloud-geojson\", \"path\": \"http://wat\"}"
}
