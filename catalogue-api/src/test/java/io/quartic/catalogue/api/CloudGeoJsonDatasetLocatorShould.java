package io.quartic.catalogue.api;

public class CloudGeoJsonDatasetLocatorShould extends DatasetLocatorTests<CloudGeoJsonDatasetLocator> {
    @Override
    protected CloudGeoJsonDatasetLocator locator() {
        return CloudGeoJsonDatasetLocatorImpl.of("http://wat");
    }

    @Override
    protected String json() {
        return "{\"type\": \"cloud-geojson\", \"path\": \"http://wat\"}";
    }
}
