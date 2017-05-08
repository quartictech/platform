package io.quartic.catalogue.api.model

class WebsocketDatasetLocatorShould : DatasetLocatorTests<WebsocketDatasetLocator>() {
    override fun locator() = WebsocketDatasetLocator("foo")

    override fun json() = "{\"type\": \"websocket\", \"url\": \"foo\"}"
}
