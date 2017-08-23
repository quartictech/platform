package io.quartic.eval.apis

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.client.ClientBuilder
import io.quartic.common.client.Retrofittable
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.model.Step
import okhttp3.ResponseBody
import retrofit2.http.Query
import java.net.URI
import java.util.concurrent.CompletableFuture

class QuartyClient(clientBuilder: ClientBuilder, url: String) {
    val quarty = clientBuilder.retrofit<Quarty>(url)
    val LOG by logger()

    sealed class QuartyResult {
        data class Success(val log: String, val dag: List<Step>) : QuartyResult()
        data class Failure(val log: String) : QuartyResult()
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = QuartyMessage.Progress::class, name = "progress"),
        JsonSubTypes.Type(value = QuartyMessage.Log::class, name = "log"),
        JsonSubTypes.Type(value = QuartyMessage.Result::class, name = "result"),
        JsonSubTypes.Type(value = QuartyMessage.Error::class, name = "error")
    )
    sealed class QuartyMessage {
        data class Progress(val message: String) : QuartyMessage()
        data class Log(val stream: String, val line: String) : QuartyMessage()
        data class Result(val result: List<Step>) : QuartyMessage()
        data class Error(val detail: Any) : QuartyMessage()
    }

    @Retrofittable
    interface Quarty {
        @retrofit2.http.GET("pipeline")
        fun getDag(
            @Query("repo_url") repoUrl: URI,
            @Query("repo_commit") repoCommit: String
        ): CompletableFuture<ResponseBody>
    }

    fun getDag(repoUrl: URI, repoCommit: String): CompletableFuture<out QuartyResult> =
        quarty.getDag(repoUrl, repoCommit)
            .thenApply { response ->
                val messages = response.string().lines()
                    .filter { ! it.isEmpty() }
                    .map { OBJECT_MAPPER.readValue<QuartyMessage>(it) }

                val result: QuartyMessage.Result? = messages.map { message ->
                    when(message) {
                        is QuartyMessage.Result -> message
                        else -> null
                    }
                }.filterNotNull().firstOrNull()

                if (result != null) {
                    QuartyResult.Success("great", result.result)
                } else {
                    QuartyResult.Failure("noob")
                }
            }
}

