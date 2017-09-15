package io.quartic.common.websocket

import javax.websocket.Endpoint
import javax.websocket.server.ServerEndpointConfig

fun serverEndpointConfig(path: String, endpoint: Endpoint): ServerEndpointConfig = ServerEndpointConfig.Builder
        .create(endpoint.javaClass, path)
        .configurator(object : ServerEndpointConfig.Configurator() {
            @Suppress("UNCHECKED_CAST")
            override fun <T> getEndpointInstance(endpointClass: Class<T>) = endpoint as T
        })
        .build()
