package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.api.LayerUpdateType;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
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
import io.quartic.weyl.core.model.StaticSchema;
import io.quartic.weyl.core.model.StaticSchemaImpl;
import org.immutables.value.Value;
import org.slf4j.Logger;
import rx.Observable;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.google.common.collect.Iterables.concat;
import static io.quartic.weyl.core.compute.SpatialPredicate.CONTAINS;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

@SweetStyle
@Value.Immutable
public abstract class BucketComputation implements LayerPopulator {
    private static final Logger LOG = getLogger(BucketComputation.class);

    protected abstract LayerId layerId();
    protected abstract BucketSpec bucketSpec();

    @Value.Default
    protected Clock clock() {
        return Clock.systemUTC();
    }

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
                        String.format("%s bucketed by %s aggregating by %s", featureName, bucketName, bucketSpec().aggregation().describe()),
                        String.format("%s / %s", featureLayer.spec().metadata().attribution(), bucketLayer.spec().metadata().attribution()),
                        clock().instant()
                ),
                IDENTITY_VIEW,
                schemaFrom(bucketLayer.spec().staticSchema(), featureName),
                true
        );
    }

    private StaticSchema schemaFrom(StaticSchema original, String rawAttributeName) {
        final AttributeName attributeName = AttributeNameImpl.of(rawAttributeName);
        return StaticSchemaImpl.copyOf(original)
                .withBlessedAttributes(concat(singletonList(attributeName), original.blessedAttributes()))
                .withPrimaryAttribute(attributeName);
    }

    @Override
    public Observable<LayerUpdate> updates(List<Layer> dependencies) {
        final Layer featureLayer = dependencies.get(0);
        final Layer bucketLayer = dependencies.get(1);

        final ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        try {
            return Observable.<LayerUpdate>never().startWith(LayerUpdateImpl.of(
                    LayerUpdateType.REPLACE,
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
                Optional.of(bucket.entityId().getUid()),
                bucket.geometry(),
                builder.build()
        );
    }
}
