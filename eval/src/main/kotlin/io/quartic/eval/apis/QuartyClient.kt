package io.quartic.eval.apis

import io.quartic.common.client.Retrofittable
import io.quartic.qube.api.model.Dag
import java.net.URI
import java.util.concurrent.CompletableFuture

@Retrofittable
interface QuartyClient {
    sealed class QuartyResult {
        data class Success(val log: String, val dag: Dag) : QuartyResult()
        data class Failure(val log: String) : QuartyResult()
    }


    fun getDag(cloneUrl: URI, ref: String): CompletableFuture<out QuartyResult>
}
