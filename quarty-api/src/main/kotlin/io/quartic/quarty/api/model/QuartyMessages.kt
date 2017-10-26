package io.quartic.quarty.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.quartic.quarty.api.model.QuartyRequest.*
import io.quartic.quarty.api.model.QuartyResponse.Complete.Error
import io.quartic.quarty.api.model.QuartyResponse.Complete.Result
import io.quartic.quarty.api.model.QuartyResponse.Log
import io.quartic.quarty.api.model.QuartyResponse.Progress
import java.net.URI

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(
    Type(Initialise::class, name = "initialise"),
    Type(Evaluate::class, name = "evaluate"),
    Type(Execute::class, name = "execute")
)
sealed class QuartyRequest {
    data class Initialise(val repoURL: URI, val repoCommit: String) : QuartyRequest()
    class Evaluate : QuartyRequest()
    data class Execute(val step: String, val namespace: String, val apiToken: String) : QuartyRequest()
}

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(
    Type(Progress::class, name = "progress"),
    Type(Log::class, name = "log"),
    Type(Result::class, name = "result"),
    Type(Error::class, name = "error")
)
sealed class QuartyResponse {
    data class Progress(val message: String) : QuartyResponse()
    data class Log(val stream: String, val line: String) : QuartyResponse()
    sealed class Complete : QuartyResponse() {
        data class Result(val result: Any?) : Complete()
        data class Error(val detail: Any) : Complete()
    }
}
