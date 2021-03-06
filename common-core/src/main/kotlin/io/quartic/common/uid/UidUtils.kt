package io.quartic.common.uid

import org.apache.commons.codec.binary.Hex
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun <T : Uid> sequenceGenerator(converter: (String) -> T) = object : UidGenerator<T> {
    val counter = AtomicInteger()
    override fun get() = converter(counter.incrementAndGet().toString())
}

fun <T : Uid> randomGenerator(converter: (String) -> T) = object : UidGenerator<T> {
    override fun get() = converter(UUID.randomUUID().toString().substring(0, 6))
}

private val random = SecureRandom()
fun <T : Uid> secureRandomGenerator(converter: (String) -> T) = object : UidGenerator<T> {
    override fun get(): T {
        val bytes = ByteArray(16)    // Length-128
        random.nextBytes(bytes)
        return converter(Hex.encodeHexString(bytes))
    }
}

