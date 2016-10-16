package io.quartic.weyl.core;

import com.google.common.collect.Iterables;
import io.quartic.weyl.core.compute.BucketOp;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.importer.Importer;
import io.quartic.weyl.core.live.LiveLayerView;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.IndexedLayer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.utils.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

public class LayerStore extends AbstractLayerStore {
    private static final Logger log = LoggerFactory.getLogger(LayerStore.class);
    public static final LiveLayerView IDENTITY_VIEW = (g, f) -> f.stream();
    private final UidGenerator<LayerId> lidGenerator;

    public LayerStore(FeatureStore featureStore, UidGenerator<LayerId> lidGenerator) {
        super(featureStore);
        this.lidGenerator = lidGenerator;
    }

    public LayerId createAndImportToLayer(Importer importer, LayerMetadata metadata) {
        final LayerId layerId = lidGenerator.get();
        createLayer(layerId, metadata, IDENTITY_VIEW);
        importToLayer(layerId, importer);
        return layerId;
    }

    private void importToLayer(LayerId layerId, Importer importer) {
        checkLayerExists(layerId);

        Collection<Feature> features = importer.get();
        log.info("imported {} features", features.size());
        log.info("envelope: {}:", Iterables.getFirst(features, null).geometry().getEnvelopeInternal());

        final IndexedLayer layer = layers.get(layerId);

        final FeatureCollection updatedFeatures = layer.layer().features().append(importer.get());

        putLayer(index(layerId,
                layer.layer()
                        .withFeatures(updatedFeatures)
                        .withSchema(createSchema(updatedFeatures)),
                IDENTITY_VIEW
        ));
    }

    public Optional<IndexedLayer> bucket(BucketSpec bucketSpec) {
        Optional<IndexedLayer> layer = BucketOp.create(this, bucketSpec).map((layer1) -> index(lidGenerator.get(), layer1, (g, f) -> f.stream()));
        layer.ifPresent(this::putLayer);
        return layer;
    }
}
