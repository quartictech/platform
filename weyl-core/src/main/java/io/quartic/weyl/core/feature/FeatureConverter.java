package io.quartic.weyl.core.feature;

import io.quartic.common.geojson.FeatureCollection;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static io.quartic.weyl.core.geojson.UtilsKt.fromJts;
import static io.quartic.weyl.core.geojson.UtilsKt.toJts;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatortoWgs84;
import static io.quartic.weyl.core.utils.GeometryTransformer.wgs84toWebMercator;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class FeatureConverter {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureConverter.class);

    private final AttributesFactory attributesFactory;
    private final GeometryTransformer toModel;
    private final GeometryTransformer fromModel;

    public FeatureConverter(AttributesFactory attributesFactory) {
        this(attributesFactory, wgs84toWebMercator(), webMercatortoWgs84());
    }

    public FeatureConverter(AttributesFactory attributesFactory, GeometryTransformer toModel, GeometryTransformer fromModel) {
        this.attributesFactory = attributesFactory;
        this.toModel = toModel;
        this.fromModel = fromModel;
    }

    public Collection<NakedFeature> toModel(FeatureCollection featureCollection) {
        return featuresToModel(featureCollection.getFeatures());
    }

    public Collection<NakedFeature> featuresToModel(Collection<io.quartic.common.geojson.Feature> features) {
        return features.stream()
                .filter(f -> f.getGeometry() != null)
                .map(this::toModel)
                .collect(toList());
    }

    private NakedFeature toModel(io.quartic.common.geojson.Feature f) {
        // HACK: we can assume that we've simply filtered out features with null geometries for now
        return NakedFeatureImpl.of(
                Optional.ofNullable(f.getId()),
                toModel.transform(toJts(f.getGeometry())),
                convertToModelAttributes(f.getProperties())
        );
    }

    private Attributes convertToModelAttributes(Map<String, Object> rawAttributes) {
        final AttributesFactory.AttributesBuilder builder = attributesFactory.builder();
        rawAttributes.forEach((k, v) -> builder.put(k, convertAttributeValue(k, v)));
        return builder.build();
    }

    private static Object convertAttributeValue(String key, Object value) {
        // TODO: Move this up into generic code behind the importers
        if (value instanceof Map) {
            try {
                return objectMapper().convertValue(value, ComplexAttribute.class);
            }
            catch (IllegalArgumentException e) {
                LOG.warn("Couldn't convert attribute {}. Exception: {}", key, e);
                return value;
            }
        }
        return value;
    }

    /**
     * Frontend GeoJSON strips out all attributes except those needed for filtering or colouring, and adds _entityId.
     */
    public FeatureCollection toFrontendGeojson(Collection<Feature> features) {
        return new FeatureCollection(features.stream().map(this::toFrontendGeojson).collect(toList()));
    }

    /**
     * Frontend GeoJSON strips out all attributes except those needed for filtering or colouring, and adds _entityId.
     */
    public io.quartic.common.geojson.Feature toFrontendGeojson(Feature f) {
        return new io.quartic.common.geojson.Feature(
                null,
                fromJts(fromModel.transform(f.geometry())),
                getRawAttributesForFrontend(f)
        );
    }

    public io.quartic.common.geojson.Feature toGeojson(Feature f) {
        return new io.quartic.common.geojson.Feature(
                null,
                fromJts(fromModel.transform(f.geometry())),
                getRawAttributes(f, t -> true)
        );
    }

    public static Map<String, Object> getRawAttributesForFrontend(Feature feature) {
        final Map<String, Object> raw = getRawAttributes(feature, v -> !(v instanceof ComplexAttribute));
        raw.put("_entityId", feature.entityId().getUid());
        return raw;
    }

    private static Map<String, Object> getRawAttributes(Feature feature, Predicate<Object> predicate) {
        return feature.attributes().attributes()
                .entrySet()
                .stream()
                .filter(e -> (e.getValue() != null) && predicate.test(e.getValue()))
                .collect(toMap(e -> e.getKey().name(), Entry::getValue));
    }
}
