package io.quartic.zeus.csv

import com.fasterxml.jackson.core.JsonGenerator.Feature
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder
import com.fasterxml.jackson.dataformat.csv.CsvSchema.ColumnType

class CsvGenerator {
    fun generateFor(data: Collection<Map<String, Any?>>): String {
        if (data.isEmpty()) {
            return "\n"
        }

        // Assume that first entry is representative
        val schema = with(Builder()) {
            data.first().keys.forEach { k -> addColumn(k, ColumnType.NUMBER_OR_STRING) }
            build()
        }.withHeader()

        val cleanedUp = removeNesting(data)

        return CsvMapper()
                .configure(Feature.IGNORE_UNKNOWN, true)
                .writerFor(cleanedUp.javaClass)
                .with(schema)
                .writeValueAsString(cleanedUp)  // TODO - switch to streaming implementation
    }

    // TODO - replace this naive in-memory approach with lazy sequence or something
    private fun removeNesting(data: Collection<Map<String, Any?>>)
            = data.map { item -> item.filterValues { v -> v !is Map<*,*> } }
}