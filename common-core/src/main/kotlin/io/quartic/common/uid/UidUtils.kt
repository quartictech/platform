package io.quartic.common.uid

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun <T : Uid> sequenceGenerator(converter: (String) -> T) = object : UidGenerator<T> {
    val counter = AtomicInteger()
    override fun get() = converter(counter.incrementAndGet().toString())
}

fun <T : Uid> randomGenerator(len: Int, converter: (String) -> T) = object : UidGenerator<T> {
    override fun get() = converter(UUID.randomUUID().toString().substring(0, len))
}

fun <T : Uid> randomGenerator(converter: (String) -> T) = randomGenerator(6, converter)

