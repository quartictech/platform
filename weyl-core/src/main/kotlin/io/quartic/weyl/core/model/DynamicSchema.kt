package io.quartic.weyl.core.model

import java.util.Collections.emptyMap

val EMPTY_SCHEMA = DynamicSchema(emptyMap())

data class DynamicSchema(val attributes: Map<AttributeName, Attribute>)