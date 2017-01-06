package io.quartic.common.pingpong

class PingPongResource : PingPongService {
    override fun ping() = Pong(javaClass.`package`.implementationVersion ?: "unknown")
}
