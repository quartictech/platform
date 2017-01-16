package io.quartic.weyl.core.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.CloudGeoJsonDatasetLocatorImpl;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.live.LayerView;
import io.quartic.weyl.core.model.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.internal.verification.Only;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LayerExporterShould {
    private PublishSubject<LayerSnapshotSequence> layerSnapshots = PublishSubject.create();
    private CatalogueService catalogueService = mock(CatalogueService.class);
    private AttributesFactory attributesFactory = new AttributesFactory();

    private static class TestLayerWriter implements LayerWriter {
        private final List<Feature> features = Lists.newArrayList();

        @Override
        public LayerExportResult write(Layer layer) {
            layer.features().stream().collect(toCollection(() -> features));
            return LayerExportResult.success(CloudGeoJsonDatasetLocatorImpl.of("test"), "ok");
        }

        public List<Feature> getFeatures() {
            return features;
        }
    }

    @Test
    public void export_features() {
        TestLayerWriter layerWriter = new TestLayerWriter();
        LayerExporter layerExporter = layerExporter(layerWriter);
        Layer layer = layer("layer", featureCollection(feature("foo")));
        List<Feature> features = ImmutableList.of(feature("foo"));
        layerSnapshots.onNext(LayerSnapshotSequenceImpl.of(layer.spec(), Observable.just(SnapshotImpl.of(layer,
                features))));
        TestSubscriber<LayerExportResult> exportResult = TestSubscriber.create();
        layerExporter.export(exportRequest("layer"))
                .subscribe(exportResult);

        exportResult.assertValue(LayerExportResult.success(CloudGeoJsonDatasetLocatorImpl.of("test"), "ok"));
        assertThat(layerWriter.getFeatures().get(0).entityId(), equalTo(features.get(0).entityId()));
    }

    @NotNull
    private LayerExportRequestImpl exportRequest(String layerId) {
        return LayerExportRequestImpl.of(LayerId.fromString(layerId));
    }

    @Test
    public void write_to_catalogue() {
        TestLayerWriter layerWriter = new TestLayerWriter();
        LayerExporter layerExporter = layerExporter(layerWriter);
        Layer layer = layer("layer", featureCollection(feature("foo")));
        List<Feature> features = ImmutableList.of(feature("foo"));
        layerSnapshots.onNext(LayerSnapshotSequenceImpl.of(layer.spec(), Observable.just(SnapshotImpl.of(layer,
                features))));
        TestSubscriber<LayerExportResult> exportResult = TestSubscriber.create();
        layerExporter.export(exportRequest("layer"))
                .subscribe(exportResult);
        verify(catalogueService, new Only()).registerDataset(ArgumentMatchers.any());
    }

    private Feature feature(String id){
        return FeatureImpl.of(EntityId.fromString(id), new GeometryFactory().createPoint(new Coordinate(0, 0)),
                attributesFactory.builder().put("foo", 1).build());
    }

    private FeatureCollection featureCollection(Feature... features) {
       return FeatureCollection.EMPTY_COLLECTION.append(ImmutableList.copyOf(features));
    }

    private Layer layer(String layerId, FeatureCollection features) {
        return LayerImpl.of(
                LayerSpecImpl.of(LayerId.fromString(layerId),
                        LayerMetadataImpl.of("yeh", "no", "weird", Instant.now(), Optional.empty()),
                        mock(LayerView.class),
                        StaticSchemaImpl.builder().build(), true),
                features,
                mock(DynamicSchema.class),
                mock(SpatialIndex.class),
                ImmutableList.of(),
                mock(LayerStats.class));
    }

    private LayerExporter layerExporter(LayerWriter layerWriter) {
        return new LayerExporter(layerSnapshots, layerWriter, catalogueService);
    }
}
