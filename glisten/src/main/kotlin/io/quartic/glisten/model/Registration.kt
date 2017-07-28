package io.quartic.glisten.model

// TODO: use @JsonTypeInfo for GH/GL/BB distinction, rather than just ignoring type field
data class Registration(
    val type: String,
    val installationId: Int
)
