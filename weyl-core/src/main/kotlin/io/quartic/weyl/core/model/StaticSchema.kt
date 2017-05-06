package io.quartic.weyl.core.model

// TODO: remove default values once everything in Kotlin
data class StaticSchema @JvmOverloads constructor(
    val titleAttribute: AttributeName? = null,
    val primaryAttribute: AttributeName? = null,
    val imageAttribute: AttributeName? = null,
    val blessedAttributes: List<AttributeName> = emptyList(),       // Order is important here
    val categoricalAttributes: Set<AttributeName> = emptySet(),
    val attributeTypes: Map<AttributeName, AttributeType> = emptyMap()
)
