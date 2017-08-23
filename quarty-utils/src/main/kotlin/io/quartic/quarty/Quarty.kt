package io.quartic.quarty

import io.quartic.common.client.Retrofittable
import okhttp3.ResponseBody
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.net.URI
import java.util.concurrent.CompletableFuture

@Retrofittable
interface Quarty {
    @Streaming
    @retrofit2.http.GET("pipeline")
    fun getPipeline(
        @Query("repo_url") repoUrl: URI,
        @Query("repo_commit") repoCommit: String
    ): CompletableFuture<ResponseBody>
}
