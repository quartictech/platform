package io.quartic.common.auth

import com.google.common.net.HostAndPort

fun getIssuer(hostHeader: String) = HostAndPort.fromString(hostHeader).host.split(".").get(0)
