package io.quartic.catalogue.api;

public class CloudGeoJsonDatasetLocatorShould extends DatasetLocatorTests<CloudGeoJsonDatasetLocator> {
    @Override
    protected CloudGeoJsonDatasetLocator locator() {
        return CloudGeoJsonDatasetLocatorImpl.of("http://wat", false);
    }

    @Override
    protected String json() {
        return "{\"type\": \"cloud-geojson\", \"path\": \"http://wat\"}";
    }
}
