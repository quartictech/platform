package io.quartic.common.test

import java.util.concurrent.CompletableFuture

fun <R> exceptionalFuture(e: Exception = RuntimeException("Sad")) = CompletableFuture<R>().apply { completeExceptionally(e) }
