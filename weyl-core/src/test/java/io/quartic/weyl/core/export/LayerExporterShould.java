package io.quartic.weyl.core.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.model.CloudGeoJsonDatasetLocator;
import io.quartic.catalogue.api.model.DatasetNamespace;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.live.LayerView;
import io.quartic.weyl.core.model.DynamicSchema;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerStats;
import io.quartic.weyl.core.model.SnapshotId;
import io.quartic.weyl.core.model.StaticSchema;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static rx.Observable.just;

public class LayerExporterShould {
    private final DatasetNamespace namespace = new DatasetNamespace("foo");
    private final PublishSubject<LayerSnapshotSequence> layerSnapshots = PublishSubject.create();
    private final CatalogueService catalogueService = mock(CatalogueService.class);
    private final AttributesFactory attributesFactory = new AttributesFactory();
    private final TestLayerWriter layerWriter = new TestLayerWriter();
    private final LayerExporter layerExporter = layerExporter(layerWriter);
    private final TestSubscriber<Optional<LayerExportResult>> exportResult = TestSubscriber.create();

    private static class TestLayerWriter implements LayerWriter {
        private final List<Feature> features = Lists.newArrayList();

        @Override
        public LayerExportResult write(Layer layer) {
            layer.getFeatures().stream().collect(toCollection(() -> features));
            return new LayerExportResult(new CloudGeoJsonDatasetLocator("test", false), "ok");
        }

        public List<Feature> getFeatures() {
            return features;
        }
    }

    private static class FailingLayerWriter implements LayerWriter {
        @Override
        public LayerExportResult write(Layer layer) throws IOException {
            throw new IOException("some noob out");
        }
    }

    @Test
    public void export_features() {
        Layer layer = layer("layer", featureCollection(feature("foo")));
        List<Feature> features = ImmutableList.of(feature("foo"));
        layerSnapshots.onNext(sequence(layer, features));
        layerExporter.export(exportRequest("layer"))
                .subscribe(exportResult);

        exportResult.assertValue(Optional.of(new LayerExportResult(new CloudGeoJsonDatasetLocator("test", false), "ok")));
        assertThat(layerWriter.getFeatures().get(0).getEntityId(), equalTo(features.get(0).getEntityId()));
    }

    @Test
    public void report_error_on_unfound_layer() {
        Layer layer = layer("layer", featureCollection(feature("foo")));
        List<Feature> features = ImmutableList.of(feature("foo"));
        layerSnapshots.onNext(sequence(layer, features));
        layerExporter.export(exportRequest("noLayer"))
                .subscribe(exportResult);
        exportResult.assertValue(Optional.empty());
    }

    @NotNull
    private LayerExportRequest exportRequest(String layerId) {
        return new LayerExportRequest(new LayerId(layerId));
    }

    @Test
    public void write_to_catalogue() {
        Layer layer = layer("layer", featureCollection(feature("foo")));
        List<Feature> features = ImmutableList.of(feature("foo"));
        layerSnapshots.onNext(sequence(layer, features));
        layerExporter.export(exportRequest("layer"))
                .subscribe(exportResult);
        verify(catalogueService, only()).registerDataset(eq(namespace), any());
        assertThat(exportResult.getOnNextEvents().size(), equalTo(1));
    }

    @Test
    public void handle_writer_failure() {
        Layer layer = layer("layer", featureCollection(feature("foo")));
        List<Feature> features = ImmutableList.of(feature("foo"));
        LayerExporter failingLayerExporter = layerExporter(new FailingLayerWriter());
        layerSnapshots.onNext(sequence(layer, features));
        failingLayerExporter.export(exportRequest("layer"))
                .subscribe(exportResult);
        verify(catalogueService, times(0)).registerDataset(any(), any());
        exportResult.assertError(RuntimeException.class);
    }

    private LayerSnapshotSequence sequence(Layer layer, List<Feature> features) {
        return new LayerSnapshotSequence(layer.getSpec(), just(new Snapshot(mock(SnapshotId.class), layer, features)));
    }

    private Feature feature(String id){
        return new Feature(new EntityId(id), new GeometryFactory().createPoint(new Coordinate(0, 0)),
                attributesFactory.builder().put("foo", 1).build());
    }

    private FeatureCollection featureCollection(Feature... features) {
       return FeatureCollection.EMPTY_COLLECTION.append(ImmutableList.copyOf(features));
    }

    private Layer layer(String layerId, FeatureCollection features) {
        return new Layer(
                new LayerSpec(
                        new LayerId(layerId),
                        new LayerMetadata("yeh", "no", "weird", Instant.now()),
                        mock(LayerView.class),
                        new StaticSchema(),
                        true
                ),
                features,
                mock(DynamicSchema.class),
                mock(SpatialIndex.class),
                ImmutableList.of(),
                mock(LayerStats.class));
    }

    private LayerExporter layerExporter(LayerWriter layerWriter) {
        return new LayerExporter(layerSnapshots, layerWriter, catalogueService, namespace);
    }
}
