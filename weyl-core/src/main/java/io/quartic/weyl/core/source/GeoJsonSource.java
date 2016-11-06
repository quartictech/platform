package io.quartic.weyl.core.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.catalogue.api.GeoJsonDatasetLocator;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

@Value.Immutable
public abstract class GeoJsonSource implements Source {
    public static ImmutableGeoJsonSource.Builder builder() {
        return ImmutableGeoJsonSource.builder();
    }

    private static final Logger LOG = LoggerFactory.getLogger(GeoJsonSource.class);

    protected abstract String name();
    protected abstract GeoJsonDatasetLocator locator();
    protected abstract FeatureStore featureStore();
    protected abstract ObjectMapper objectMapper();
    @Value.Default
    protected GeometryTransformer geometryTransformer() {
        return GeometryTransformer.wgs84toWebMercator();
    }
    @Value.Derived
    protected URL url() {
        try {
            return new URL(locator().url());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Source URL malformed", e);
        }
    }

    @Override
    public Observable<SourceUpdate> observable() {
        return Observable.create(sub -> {
            try {
                sub.onNext(SourceUpdate.of(importAllFeatures(), emptyList()));
                sub.onCompleted();
            } catch (IOException e) {
                sub.onError(e);
            }
        });
    }

    @Override
    public boolean indexable() {
        return true;
    }

    @Override
    public LayerViewType viewType() {
        return LayerViewType.MOST_RECENT;
    }

    private Collection<io.quartic.weyl.core.model.Feature> importAllFeatures() throws IOException {
        final FeatureCollection featureCollection = objectMapper().readValue(url(), FeatureCollection.class);

        return featureCollection.features().stream().map(this::toJts)
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toList());
    }

    private Optional<io.quartic.weyl.core.model.Feature> toJts(Feature f) {
        // TODO: We are ignoring null geometries here (as well as in the live pipeline). We should figure out something better.
        return f.geometry().map(rawGeometry -> {
            Geometry transformedGeometry = geometryTransformer().transform(Utils.toJts(rawGeometry));
            return ImmutableFeature.builder()
                    .externalId(f.id().orElse(null))
                    .uid(featureStore().getFeatureIdGenerator().get())
                    .geometry(transformedGeometry)
                    .metadata(convertMetadata(f.properties()))
                    .build();
        });
    }

    private Object convertMetadataValue(Object value) {
        // TODO: Move this up into generic code behind the importers
        if (value instanceof Map) {
            try {
                return objectMapper().convertValue(value, ComplexAttribute.class);
            }
            catch (IllegalArgumentException e) {
                LOG.warn("[{}] Unrecognised complex attribute type: {}", name(), value);
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
