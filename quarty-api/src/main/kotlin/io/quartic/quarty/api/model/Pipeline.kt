package io.quartic.quarty.api.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.quartic.quarty.api.model.Pipeline.Node.Raw
import io.quartic.quarty.api.model.Pipeline.Node.Step
import io.quartic.quarty.api.model.Pipeline.Source.Bucket

data class Pipeline(
    val nodes: List<Node>
) {
    @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
    @JsonSubTypes(
        Type(Step::class, name = "step"),
        Type(Raw::class, name = "raw")
    )
    sealed class Node {
        abstract val id: String
        abstract val info: LexicalInfo
        abstract val inputs: List<Dataset>
        abstract val output: Dataset

        data class Step(
            override val id: String,
            override val info: LexicalInfo,
            override val inputs: List<Dataset>,
            override val output: Dataset
        ) : Node()

        data class Raw(
            override val id: String,
            override val info: LexicalInfo,
            val source: Source,
            override val output: Dataset
        ) : Node() {
            @JsonIgnore
            override val inputs = emptyList<Dataset>()
        }
    }

    data class LexicalInfo(
        val name: String,
        val description: String?,
        val file: String,
        val lineRange: List<Int>
    )

    data class Dataset(
        val namespace: String?,
        val datasetId: String
    )

    @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
    @JsonSubTypes(
        Type(Bucket::class, name = "bucket")
    )
    sealed class Source {
        data class Bucket(
            val key: String,
            val name: String? = null   // null means default bucket
        ) : Source()
    }
}

