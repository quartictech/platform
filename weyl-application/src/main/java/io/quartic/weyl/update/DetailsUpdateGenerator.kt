package io.quartic.weyl.update

import io.quartic.weyl.core.model.AttributeName
import io.quartic.weyl.core.model.Feature

class DetailsUpdateGenerator : SelectionDrivenUpdateGenerator {
    data class Details(val schema: Set<String>, val records: List<Map<String, Any>>)

    override fun name() = "details"

    override fun generate(entities: Collection<Feature>): Details {
        val records = entities
                .map { e -> e.attributes.attributes()[DETAILS_ATTRIBUTE_NAME] }
                .filterNotNull()
                .filterIsInstance<List<Any>>()
                .flatMap { it }
                .filterIsInstance<Map<String, Any>>()

        val schema = records
                .map { it.keys }
                .flatMap { it }
                .toSet()

        return Details(schema, records)
    }

    companion object {
        val DETAILS_ATTRIBUTE_NAME = AttributeName("_details")   // TODO: move into static schema and plumb through somehow
    }
}