package io.quartic.catalogue.api;

public class GeoJsonDatasetLocatorShould extends DatasetLocatorTests<GeoJsonDatasetLocator> {
    @Override
    protected GeoJsonDatasetLocator locator() {
        return GeoJsonDatasetLocatorImpl.of("http://wat");
    }

    @Override
    protected String json() {
        return "{\"type\": \"geojson\", \"path\": \"http://wat\"}";
    }
}
