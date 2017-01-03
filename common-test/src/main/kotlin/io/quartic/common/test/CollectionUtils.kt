package io.quartic.common.test

// TODO: can eliminate this stuff once everything's in Kotlin

@SafeVarargs
fun <K, V> map(vararg pairs: Pair<K, V>) = hashMapOf(*pairs)

fun <K, V> entry(key: K, value: V) = Pair(key, value)
