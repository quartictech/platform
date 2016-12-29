package io.quartic.catalogue.api;

public class WebsocketDatasetLocatorShould extends DatasetLocatorTests<WebsocketDatasetLocator> {
    @Override
    protected WebsocketDatasetLocator locator() {
        return WebsocketDatasetLocatorImpl.of("foo");
    }

    @Override
    protected String json() {
        return "{\"type\": \"websocket\", \"url\": \"foo\"}";
    }
}
