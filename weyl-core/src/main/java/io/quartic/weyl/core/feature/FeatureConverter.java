package io.quartic.weyl.core.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.geojson.*;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.weyl.core.geojson.Utils.fromJts;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatortoWgs84;
import static io.quartic.weyl.core.utils.GeometryTransformer.wgs84toWebMercator;
import static java.util.stream.Collectors.toList;

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

    public Collection<NakedFeature> toModel(io.quartic.geojson.FeatureCollection featureCollection) {
        return featureCollection.features().stream()
                .filter(f -> f.geometry().isPresent())
                .map(this::toModel)
                .collect(toList());
    }

    private NakedFeature toModel(io.quartic.geojson.Feature f) {
        // HACK: we can assume that we've simply filtered out features with null geometries for now
        return NakedFeatureImpl.of(
                f.id(),
                toModel.transform(Utils.toJts(f.geometry().get())),
                convertToModelAttributes(f.properties())
        );
    }

    private Attributes convertToModelAttributes(Map<String, Object> rawAttributes) {
        final AttributesFactory.AttributesBuilder builder = attributesFactory.builder();
        rawAttributes.forEach((k, v) -> builder.put(k, convertAttributeValue(OBJECT_MAPPER, k, v)));
        return builder.build();
    }

    private static Object convertAttributeValue(ObjectMapper objectMapper, String key, Object value) {
        // TODO: Move this up into generic code behind the importers
        if (value instanceof Map) {
            try {
                return objectMapper.convertValue(value, ComplexAttribute.class);
            }
            catch (IllegalArgumentException e) {
                LOG.warn("Couldn't convert attribute {}. Exception: {}", key, e);
                return value;
            }
        }
        return value;
    }

    public io.quartic.geojson.FeatureCollection toGeojson(Collection<Feature> features) {
        return FeatureCollectionImpl.of(
                features.stream()
                        .map(this::toGeojson)
                        .collect(toList())
        );
    }

    private io.quartic.geojson.Feature toGeojson(Feature f) {
        return io.quartic.geojson.FeatureImpl.of(
                Optional.empty(),
                Optional.of(fromJts(fromModel.transform(f.geometry()))),
                getRawProperties(f)
        );
    }

    public static Map<String, Object> getRawProperties(Feature feature) {
        final Map<String, Object> output = newHashMap();
        feature.attributes().attributes().entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof ComplexAttribute) && (entry.getValue() != null))
                .forEach(entry -> output.put(entry.getKey().name(), entry.getValue()));
        output.put("_entityId", feature.entityId().uid());
        return output;
    }
}
