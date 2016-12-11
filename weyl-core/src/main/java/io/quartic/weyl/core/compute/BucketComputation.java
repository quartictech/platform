package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeImpl;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.AttributeSchemaImpl;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import org.immutables.value.Value;
import org.slf4j.Logger;
import rx.Observable;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.newHashMap;
import static io.quartic.weyl.core.compute.SpatialJoiner.SpatialPredicate.CONTAINS;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static rx.Observable.just;

@SweetStyle
@Value.Immutable
public abstract class BucketComputation implements LayerPopulator {
    private static final Logger LOG = getLogger(BucketComputation.class);

    protected abstract LayerId layerId();
    protected abstract BucketSpec bucketSpec();

    @Value.Default
    protected SpatialJoiner joiner() {
        return new SpatialJoiner();
    }

    private final AttributesFactory attributesFactory = new AttributesFactory();

    @Override
    public List<LayerId> dependencies() {
        return ImmutableList.of(bucketSpec().features(), bucketSpec().buckets());
    }

    @Override
    public LayerSpec spec(List<Layer> dependencies) {
        final Layer featureLayer = dependencies.get(0);
        final Layer bucketLayer = dependencies.get(1);

        final String featureName = featureLayer.spec().metadata().name();
        final String bucketName = bucketLayer.spec().metadata().name();

        return LayerSpecImpl.of(
                layerId(),
                LayerMetadataImpl.of(
                        String.format("%s (bucketed)", featureName),
                        String.format("%s bucketed by %s aggregating by %s", featureName, bucketName, bucketSpec().aggregation()),
                        Optional.empty(),
                        Optional.empty()
                ),
                IDENTITY_VIEW,
                schemaFrom(bucketLayer, featureName),
                true
        );
    }

    private AttributeSchema schemaFrom(Layer bucketLayer, String rawAttributeName) {
        final AttributeSchema originalSchema = bucketLayer.spec().schema();

        Map<AttributeName, Attribute> attributeMap = newHashMap(originalSchema.attributes());
        Attribute newAttribute = AttributeImpl.builder()
                .type(AttributeType.NUMERIC)
                .build();
        final AttributeName attributeName = AttributeNameImpl.of(rawAttributeName);
        attributeMap.put(attributeName, newAttribute);

        return AttributeSchemaImpl
                .copyOf(originalSchema)
                .withAttributes(attributeMap)
                .withBlessedAttributes(concat(singletonList(attributeName), originalSchema.blessedAttributes()))
                .withPrimaryAttribute(attributeName);
    }

    @Override
    public Observable<LayerUpdate> updates(List<Layer> dependencies) {
        final Layer featureLayer = dependencies.get(0);
        final Layer bucketLayer = dependencies.get(1);

        final ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        try {
            return just(LayerUpdateImpl.of(
                    forkJoinPool.submit(() -> {
                        // The order here is that CONTAINS applies from left -> right and
                        // the spatial index on the right layer is the one that is queried
                        Map<Feature, List<Tuple>> groups = joiner()
                                .innerJoin(bucketLayer, featureLayer, CONTAINS)
                                .collect(groupingBy(Tuple::left));

                        return groups.entrySet().parallelStream()
                                .map(entry -> featureForBucket(featureLayer.spec().metadata().name(), entry))
                                .collect(toList());
                    }).get()
            ));
        } catch (ExecutionException | InterruptedException e) {
            // TODO: this is naughty, we shouldn't be wrapping an IE in a RE
            throw new RuntimeException("Error during computation", e);
        }
    }

    private NakedFeature featureForBucket(String attributeName, Entry<Feature, List<Tuple>> entry) {
        Feature bucket = entry.getKey();
        Double value = bucketSpec().aggregation().aggregate(
                bucket,
                entry.getValue().stream().map(Tuple::right).collect(toList()));

        if (bucketSpec().normalizeToArea()) {
            if (bucket.geometry().getArea() > 0) {
                value /= bucket.geometry().getArea();
            }
        }

        final AttributesFactory.AttributesBuilder builder = attributesFactory.builder(bucket.attributes());
        builder.put(attributeName, value);
        return NakedFeatureImpl.of(
                Optional.of(bucket.entityId().uid()),
                bucket.geometry(),
                builder.build()
        );
    }
}
