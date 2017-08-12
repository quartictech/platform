package io.quartic.home.conversion

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import io.quartic.common.geojson.Feature
import io.quartic.common.geojson.GeoJsonGenerator
import io.quartic.common.geojson.Point
import io.quartic.common.logging.logger
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.Double.parseDouble
import java.util.concurrent.atomic.AtomicLong
import kotlin.streams.asStream

class CsvConverter : GeoJsonConverter {
    private val LOG by logger()

    override fun convert(data: InputStream, outputStream: OutputStream) {
        val csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(InputStreamReader(data))

        val firstRow = csvParser.headerMap

        val latColumn = firstRow.keys
                .filter { key -> key.toLowerCase().startsWith("lat") || key.toLowerCase().contains("latitude") }
                .firstOrNull()

        val lonColumn = firstRow.keys
                .filter { key -> key.toLowerCase().startsWith("lon") || key.toLowerCase().contains("longitude") }
                .firstOrNull()

        if (latColumn != null && lonColumn != null) {
            val keys = Sets.newHashSet(firstRow.keys)
            keys.removeAll(ImmutableSet.of(latColumn, lonColumn))

            val totalFeatureCount = AtomicLong()

            val features = parseFeatures(csvParser, latColumn, lonColumn, keys)
                    .onEach { totalFeatureCount.incrementAndGet() }
                    .filter { it != null }
                    .map { it!! }
            val convertedFeatureCount = GeoJsonGenerator(outputStream).writeFeatures(features.asStream())

            LOG.info("$convertedFeatureCount of $totalFeatureCount features successfully converted")
        } else {
            throw RuntimeException("lat & lon field can't be found in keys: ${firstRow.keys}")
        }
    }

    private fun parseFeatures(
            records: Iterable<CSVRecord>,
            latColumn: String,
            lonColumn: String,
            columns: Set<String>
    ): Sequence<Feature?> {
        return records
                .asSequence()
                .map { record ->
                    try {
                        parseFeature(record, record[latColumn], record[lonColumn], columns)
                    } catch (e: NumberFormatException) {
                        LOG.warn("Exception converting lat & lon", e)
                        null
                    }
                }
    }

    @Throws(NumberFormatException::class)
    private fun parseFeature(record: CSVRecord, latStr: String?, lonStr: String?, columns: Set<String>): Feature? {
        return if (latStr != null && lonStr != null) {
            Feature(
                    null,
                    Point(listOf(parseDouble(lonStr), parseDouble(latStr))),
                    columns.associateBy({it}, {record[it]})
            )
        } else null
    }
}
