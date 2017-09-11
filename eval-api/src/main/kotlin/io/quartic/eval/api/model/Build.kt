package io.quartic.eval.api.model

import io.quartic.common.model.CustomerId
import java.util.*
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import java.time.Instant


@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    Type(Build::class, name = "build")
)
data class Build(
    val id: UUID,
    val buildNumber: Long,
    val branch: String,
    val customerId: CustomerId,
    val trigger: BuildTrigger,
    val status: String,
    val time: Instant
)
