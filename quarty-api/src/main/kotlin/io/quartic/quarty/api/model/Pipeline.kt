package io.quartic.quarty.api.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.quartic.quarty.api.model.Node.Raw
import io.quartic.quarty.api.model.Node.Step
import io.quartic.quarty.api.model.Source.S3

data class Pipeline(
    val steps: List<Node>
)

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(
    Type(Step::class, name = "step"),
    Type(Raw::class, name = "raw")
)
sealed class Node {
    abstract val id: String
    abstract val info: LexicalInfo
    abstract val inputs: List<Dataset>
    abstract val outputs: List<Dataset>

    data class Step(
        override val id: String,
        override val info: LexicalInfo,
        override val inputs: List<Dataset>,
        override val outputs: List<Dataset>
    ) : Node()

    data class Raw(
        override val id: String,
        override val info: LexicalInfo,
        override val outputs: List<Dataset>,
        val source: Source
    ) : Node() {
        override val inputs = emptyList<Dataset>()
    }
}

data class LexicalInfo(
    val name: String,
    val description: String?,
    val file: String,
    @JsonProperty("line_range")
    val lineRange: List<Int>
)

data class Dataset(
    val namespace: String?,
    @JsonProperty("dataset_id")
    val datasetId: String
) {
    @get:JsonIgnore
    val fullyQualifiedName get() = "${namespace ?: ""}::${datasetId}"
}

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(
    Type(S3::class, name = "s3")
)
sealed class Source {
    data class S3(
        val bucket: String,
        val key: String
    ) : Source()
}
