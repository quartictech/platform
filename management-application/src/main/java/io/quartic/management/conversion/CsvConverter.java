package io.quartic.management.conversion;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.quartic.common.serdes.ObjectMappers;
import io.quartic.geojson.Feature;
import io.quartic.geojson.Point;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class CsvConverter implements GeoJsonConverter {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CsvConverter.class);

    private Optional<Feature> parseFeature(CSVRecord record, String latStr, String lonStr, Set<String> columns)
            throws NumberFormatException {
        if (latStr != null && lonStr != null) {
            Double lat = Double.parseDouble(latStr);
            Double lon = Double.parseDouble(lonStr);

            Point point = new Point(ImmutableList.of(lon, lat));

            Map<String, Object> properties = Maps.newHashMap();
            for (String key : columns) {
                properties.put(key, record.get(key));
            }

            return Optional.of(new Feature(null, point, properties));
        }

        return Optional.empty();
    }

    private Stream<Feature> parseFeatures(Iterable<CSVRecord> records, String latColumn,
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
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
    }

    @Override
    public void convert(InputStream data, OutputStream outputStream) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream);
        jsonGenerator.setCodec(ObjectMappers.OBJECT_MAPPER);
        CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new InputStreamReader(data));

        Map<String, Integer> firstRow = csvParser.getHeaderMap();

        Optional<String> latColumn = firstRow.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(key -> key.toLowerCase().startsWith("lat") || key.toLowerCase().contains("latitude"))
                .findFirst();

        Optional<String> lonColumn = firstRow.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(key -> key.toLowerCase().startsWith("lon") || key.toLowerCase().contains("longitude"))
                .findFirst();


        if (latColumn.isPresent() && lonColumn.isPresent()) {
            String latField = latColumn.get();
            String lonField = lonColumn.get();
            Set<String> keys = Sets.newHashSet(firstRow.keySet());
            keys.removeAll(ImmutableSet.of(latField, lonField));

            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("type", "FeatureCollection");
            jsonGenerator.writeArrayFieldStart("features");

            parseFeatures(csvParser, latField, lonField, keys)
                    .forEach(feature -> {
                        try {
                            jsonGenerator.writeObject(feature);
                        } catch (IOException e) {
                            throw new RuntimeException("exception while writing json: " + e);
                        }
                    });
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
        else {
            throw new RuntimeException("lat & lon field can't be found in keys: " + firstRow.keySet());
        }
    }
}
