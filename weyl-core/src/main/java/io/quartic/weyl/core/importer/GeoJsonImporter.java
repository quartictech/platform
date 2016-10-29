package io.quartic.weyl.core.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.jester.api.GeoJsonDatasetSource;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeoJsonImporter implements Importer {
    private static final Logger LOG = LoggerFactory.getLogger(GeoJsonImporter.class);
    private final FeatureStore featureStore;
    private final GeometryTransformer geometryTransformer;
    private final ObjectMapper objectMapper;
    private final URL url;

    public static GeoJsonImporter create(GeoJsonDatasetSource source, FeatureStore featureStore, ObjectMapper objectMapper) {
        try {
            return new GeoJsonImporter(new URL(source.url()), featureStore, GeometryTransformer.wgs84toWebMercator(), objectMapper);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Source URL malformed", e);
        }
    }

    public GeoJsonImporter(URL url, FeatureStore featureStore,
                            GeometryTransformer geometryTransformer, ObjectMapper objectMapper) {
        this.url = url;
        this.featureStore = featureStore;
        this.geometryTransformer = geometryTransformer;
        this.objectMapper = objectMapper;
    }

    @Override
    public Collection<io.quartic.weyl.core.model.Feature> get() throws IOException {
        final FeatureCollection featureCollection = objectMapper.readValue(url, FeatureCollection.class);

        return featureCollection.features().stream().map(this::toJts)
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toList());
    }

    private Optional<io.quartic.weyl.core.model.Feature> toJts(Feature f) {
        // TODO: We are ignoring null geometries here (as well as in the live pipeline). We should figure out something better.
        return f.geometry().map(rawGeometry -> {
            Geometry transformedGeometry = geometryTransformer.transform(Utils.toJts(rawGeometry));
            return ImmutableFeature.builder()
                    .externalId(f.id().orElse(null))
                    .uid(featureStore.getFeatureIdGenerator().get())
                    .geometry(transformedGeometry)
                    .metadata(convertMetadata(f.properties()))
                    .build();
        });
    }

    private Object convertMetadataValue(Object value) {
        // TODO: Move this up into generic code behind the importers
        if (value instanceof Map) {
            try {
                return objectMapper.convertValue(value, ComplexAttribute.class);
            }
            catch (IllegalArgumentException e) {
                LOG.warn("unrecognised complex attribute type: " + value);
                return value;
            }
        }
        return value;
    }

    private Map<String, Object> convertMetadata(Map<String, Object> rawMetadata) {
        return rawMetadata.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> convertMetadataValue(entry.getValue())));
    }
}
