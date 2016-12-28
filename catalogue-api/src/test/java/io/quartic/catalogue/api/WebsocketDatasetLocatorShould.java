package io.quartic.catalogue.api;

/**
 * Created by alex on 28/12/16.
 */
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
