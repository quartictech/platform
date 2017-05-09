package io.quartic.weyl.core.feature;

import io.quartic.common.geojson.FeatureCollection;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.DynamicSchema;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static io.quartic.weyl.core.geojson.UtilsKt.fromJts;
import static io.quartic.weyl.core.geojson.UtilsKt.toJts;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatortoWgs84;
import static io.quartic.weyl.core.utils.GeometryTransformer.wgs84toWebMercator;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class FeatureConverter {
    public interface AttributeManipulator {
        boolean test(AttributeName name, Object value);
        void postProcess(Feature feature, Map<String, Object> attributes);
    }

    public static final AttributeManipulator MINIMAL_MANIPULATOR = new AttributeManipulator() {
        @Override
        public boolean test(AttributeName name, Object value) {
            return false;
        }

        @Override
        public void postProcess(Feature feature, Map<String, Object> attributes) {}
    };

    public static final AttributeManipulator DEFAULT_MANIPULATOR = new AttributeManipulator() {
        @Override
        public boolean test(AttributeName name, Object value) {
            return true;
        }

        @Override
        public void postProcess(Feature feature, Map<String, Object> attributes) {}
    };

    public static AttributeManipulator frontendManipulatorFor(DynamicSchema schema) {
        return new AttributeManipulator() {
            @Override
            public boolean test(AttributeName name, Object value) {
                final Attribute attribute = schema.getAttributes().get(name);
                if (attribute == null) {
                    throw new RuntimeException("Couldn't find attribute '" + name.getName() + "' in the schema");
                }

                return attribute.getType() == AttributeType.NUMERIC ||
                        attribute.getType() == AttributeType.TIMESTAMP ||
                        ofNullable(attribute.getCategories()).map(s -> !s.isEmpty()).orElse(false);
            }

            @Override
            public void postProcess(Feature feature, Map<String, Object> attributes) {
                attributes.put("_entityId", feature.getEntityId().getUid());
            }
        };
    }

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
        return new NakedFeature(
                f.getId(),
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

    public FeatureCollection toGeojson(AttributeManipulator manipulator, Collection<Feature> features) {
        return new FeatureCollection(
                features.stream()
                        .map(f -> toGeojson(manipulator, f))
                        .collect(toList())
        );
    }

    public io.quartic.common.geojson.Feature toGeojson(AttributeManipulator manipulator, Feature feature) {
        return new io.quartic.common.geojson.Feature(
                null,
                fromJts(fromModel.transform(feature.getGeometry())),
                getRawAttributes(manipulator, feature)
        );
    }

    public static Map<String, Object> getRawAttributes(AttributeManipulator manipulator, Feature feature) {
        final Map<String, Object> raw = feature.getAttributes().getAttributes()
                .entrySet()
                .stream()
                .filter(e -> (e.getValue() != null) && manipulator.test(e.getKey(), e.getValue()))
                .collect(toMap(e -> e.getKey().getName(), Entry::getValue));
        manipulator.postProcess(feature, raw);
        return raw;
    }
}
