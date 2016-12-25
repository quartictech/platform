package io.quartic.common.uid

import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

// TODO: these can be reified once all Java refs are eliminated

fun <T : Uid> sequenceGenerator(converter: Function<String, T>) = object : UidGenerator<T> {
    val counter = AtomicInteger()
    override fun get() = converter.apply(counter.incrementAndGet().toString())
}

fun <T : Uid> randomGenerator(converter: Function<String, T>) = object : UidGenerator<T> {
    override fun get() = converter.apply(UUID.randomUUID().toString().substring(0, 6))
}