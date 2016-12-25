package io.quartic.common.websocket

import javax.websocket.Endpoint
import javax.websocket.server.ServerEndpointConfig
import javax.websocket.server.ServerEndpointConfig.Configurator

fun createEndpointConfig(path: String, endpoint: Endpoint) = ServerEndpointConfig.Builder
        .create(endpoint.javaClass, path)
        .configurator(object : Configurator() {
            @Suppress("UNCHECKED_CAST")
            override fun <T> getEndpointInstance(endpointClass: Class<T>) = endpoint as T
        })
        .build()
