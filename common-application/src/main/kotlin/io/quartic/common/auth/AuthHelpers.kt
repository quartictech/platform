package io.quartic.common.auth

import com.google.common.net.HostAndPort

fun extractSubdomain(hostHeader: String) = HostAndPort.fromString(hostHeader).host.split(".")[0]
