package io.quartic.catalogue.api.model

class WebsocketDatasetLocatorShould : DatasetLocatorTests<DatasetLocator.WebsocketDatasetLocator>() {
    override fun locator() = DatasetLocator.WebsocketDatasetLocator("foo")

    override fun json() = "{\"type\": \"websocket\", \"url\": \"foo\"}"
}
