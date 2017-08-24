package io.quartic.qube.api.model

data class ContainerSpec(
   val image: String,
   val command: List<String>,
   // TODO: This may need to be optional
   val port: Int
)
