package io.quartic.management.conversion;

import com.google.common.collect.*;
import io.quartic.geojson.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class CsvConverter implements GeoJsonConverter {
    private static final Logger LOG = LoggerFactory.getLogger(CsvConverter.class);

    private Optional<Feature> parseFeature(CSVRecord record, String latStr, String lonStr, Set<String> columns)
            throws NumberFormatException {
        if (latStr != null && lonStr != null) {
            Double lat = Double.parseDouble(latStr);
            Double lon = Double.parseDouble(lonStr);

            Point point = PointImpl.of(ImmutableList.of(lon, lat));

            Map<String, Object> properties = Maps.newHashMap();
            for (String key : columns) {
                properties.put(key, record.get(key));
            }

            return Optional.of(FeatureImpl.of(Optional.empty(), Optional.of(point), properties));
        }

        return Optional.empty();
    }

    private Collection<Feature> parseFeatures(Iterable<CSVRecord> records, String latColumn,
                                              String lonColumn, Set<String> columns) {
        List<Optional<Feature>> features = StreamSupport.stream(records.spliterator(), false)
                .map(record -> {
                    String latStr = record.get(latColumn);
                    String lonStr = record.get(lonColumn);
                    try {
                        return parseFeature(record, latStr, lonStr, columns);
                    } catch (NumberFormatException e) {
                        LOG.warn("exception converting lat: " + e);
                        return Optional.<Feature>empty();
                    }
                })
                .collect(toList());

        LOG.info("{} of {} features successfully converted",
                features.stream().filter(Optional::isPresent).count(),
                features.size());

        return features.stream()
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(toList());
    }

    @Override
    public FeatureCollection convert(InputStream data) throws IOException {
        CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new InputStreamReader(data));

        Map<String, Integer> firstRow = csvParser.getHeaderMap();

        Optional<String> latColumn = firstRow.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(key -> key.startsWith("lat") || key.contains("latitude"))
                .findFirst();

        Optional<String> lonColumn = firstRow.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(key -> key.startsWith("lon") || key.contains("longitude"))
                .findFirst();


        if (latColumn.isPresent() && lonColumn.isPresent()) {
            String latField = latColumn.get();
            String lonField = lonColumn.get();
            Set<String> keys = Sets.newHashSet(firstRow.keySet());
            keys.removeAll(ImmutableSet.of(latField, lonField));

            return FeatureCollectionImpl.of(parseFeatures(csvParser, latField, lonField, keys));
        }
        else {
            throw new RuntimeException("lat & lon field can't be found in keys: " + firstRow.keySet());
        }
    }
}
