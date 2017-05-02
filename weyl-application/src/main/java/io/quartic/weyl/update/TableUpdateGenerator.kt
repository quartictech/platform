package io.quartic.weyl.update

import io.quartic.weyl.core.model.AttributeName
import io.quartic.weyl.core.model.Feature

class TableUpdateGenerator : SelectionDrivenUpdateGenerator {
    data class Table(val schema: Set<String>, val records: List<Map<String, Any>>)

    override fun name() = "table"

    override fun generate(entities: Collection<Feature>): Table {
        val records = entities
                .map { e -> e.attributes().attributes()[TABULAR_ATTRIBUTE_NAME] }
                .filterNotNull()
                .filterIsInstance<List<Any>>()
                .flatMap { it }
                .filterIsInstance<Map<String, Any>>()

        val schema = records
                .map { it.keys }
                .flatMap { it }
                .toSet()

        return Table(schema, records)
    }

    companion object {
        val TABULAR_ATTRIBUTE_NAME = AttributeName.fromString("_table")   // TODO: move into static schema and plumb through somehow
    }
}