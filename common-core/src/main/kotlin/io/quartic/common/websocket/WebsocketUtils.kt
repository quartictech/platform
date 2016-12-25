package io.quartic.common.websocket

import com.google.common.collect.Lists.newArrayList
import com.google.common.net.HttpHeaders.USER_AGENT
import io.quartic.common.client.Utils.userAgentFor
import javax.websocket.ClientEndpointConfig
import javax.websocket.Endpoint
import javax.websocket.server.ServerEndpointConfig

fun serverEndpointConfig(path: String, endpoint: Endpoint) = ServerEndpointConfig.Builder
        .create(endpoint.javaClass, path)
        .configurator(object : ServerEndpointConfig.Configurator() {
            @Suppress("UNCHECKED_CAST")
            override fun <T> getEndpointInstance(endpointClass: Class<T>) = endpoint as T
        })
        .build()

fun clientEndpointConfig(owner: Class<*>) = ClientEndpointConfig.Builder
        .create()
        .configurator(object : ClientEndpointConfig.Configurator() {
            override fun beforeRequest(headers: MutableMap<String, List<String>>?) {
                headers!!.put(USER_AGENT, newArrayList(userAgentFor(owner)))
            }
        })
        .build()
