package io.quartic.catalogue.api

class WebsocketDatasetLocatorShould : DatasetLocatorTests<WebsocketDatasetLocator>() {
    override fun locator(): WebsocketDatasetLocator = WebsocketDatasetLocatorImpl.of("foo")

    override fun json() = "{\"type\": \"websocket\", \"url\": \"foo\"}"
}
