package io.quartic.catalogue.api.model

import io.quartic.catalogue.api.model.CloudGeoJsonDatasetLocator

class CloudGeoJsonDatasetLocatorShould : DatasetLocatorTests<CloudGeoJsonDatasetLocator>() {
    override fun locator() = CloudGeoJsonDatasetLocator("http://wat", false)

    override fun json() = "{\"type\": \"cloud-geojson\", \"path\": \"http://wat\"}"
}
