package io.quartic.quarty

import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.net.URI
import java.util.concurrent.CompletableFuture

@Retrofittable
interface Quarty {
    @Streaming
    @GET("init")
    fun initAsync(
        @Query("repo_url") repoUrl: URI,
        @Query("repo_commit") repoCommit: String
    ): CompletableFuture<ResponseBody>

    @Streaming
    @GET("evaluate")
    fun evaluateAsync(): CompletableFuture<ResponseBody>

    @Streaming
    @POST("execute")
    fun executeAsync(
        @Query("step") step: String,
        @Query("namespace") namespace: String
    ): CompletableFuture<ResponseBody>
}
