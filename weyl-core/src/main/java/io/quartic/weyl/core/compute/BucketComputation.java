package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.StaticSchema;
import org.immutables.value.Value;
import org.slf4j.Logger;
import rx.Observable;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.weyl.api.LayerUpdateType.REPLACE;
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

        final String featureName = featureLayer.getSpec().getMetadata().getName();
        final String bucketName = bucketLayer.getSpec().getMetadata().getName();

        return new LayerSpec(
                layerId(),
                new LayerMetadata(
                        String.format("%s (bucketed)", featureName),
                        String.format("%s bucketed by %s aggregating by %s", featureName, bucketName, bucketSpec().aggregation().describe()),
                        String.format("%s / %s", featureLayer.getSpec().getMetadata().getAttribution(), bucketLayer.getSpec().getMetadata().getAttribution()),
                        clock().instant()
                ),
                IDENTITY_VIEW,
                schemaFrom(bucketLayer.getSpec().getStaticSchema(), featureName),
                true
        );
    }

    private StaticSchema schemaFrom(StaticSchema original, String rawAttributeName) {
        final AttributeName attributeName = new AttributeName(rawAttributeName);
        // TODO: Kotlin-ify this
        return new StaticSchema(
                original.getTitleAttribute(),
                attributeName,
                original.getImageAttribute(),
                newHashSet(concat(singletonList(attributeName), original.getBlessedAttributes())),
                original.getCategoricalAttributes(),
                original.getAttributeTypes()
        );
    }

    @Override
    public Observable<LayerUpdate> updates(List<Layer> dependencies) {
        final Layer featureLayer = dependencies.get(0);
        final Layer bucketLayer = dependencies.get(1);

        final ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        try {
            return Observable.<LayerUpdate>never().startWith(new LayerUpdate(
                    REPLACE,
                    forkJoinPool.submit(() -> {
                        // The order here is that CONTAINS applies from left -> right and
                        // the spatial index on the right layer is the one that is queried
                        Map<Feature, List<Tuple>> groups = joiner()
                                .innerJoin(bucketLayer, featureLayer, CONTAINS)
                                .collect(groupingBy(Tuple::getLeft));

                        return groups.entrySet().parallelStream()
                                .map(entry -> featureForBucket(featureLayer.getSpec().getMetadata().getName(), entry))
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
                entry.getValue().stream().map(Tuple::getRight).collect(toList()));

        if (bucketSpec().normalizeToArea()) {
            if (bucket.getGeometry().getArea() > 0) {
                value /= bucket.getGeometry().getArea();
            }
        }

        final AttributesFactory.AttributesBuilder builder = attributesFactory.builder(bucket.getAttributes());
        builder.put(attributeName, value);
        return new NakedFeature(
                bucket.getEntityId().getUid(),
                bucket.getGeometry(),
                builder.build()
        );
    }
}
