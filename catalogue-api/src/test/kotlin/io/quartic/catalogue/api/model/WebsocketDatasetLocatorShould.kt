package io.quartic.catalogue.api.model

import io.quartic.catalogue.api.model.WebsocketDatasetLocator

class WebsocketDatasetLocatorShould : DatasetLocatorTests<WebsocketDatasetLocator>() {
    override fun locator() = WebsocketDatasetLocator("foo")

    override fun json() = "{\"type\": \"websocket\", \"url\": \"foo\"}"
}
