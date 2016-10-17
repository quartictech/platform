package io.quartic.weyl.core.v2;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.internal.EntryDefault;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.stream.Collectors;

@SweetStyle
@Value.Immutable
public abstract class AbstractDefaultLayer implements Layer {
    abstract Layer prev();
    abstract FeatureCollection features();
    abstract RTree<Feature, Rectangle> spatialIndex();

    public static Layer empty() {
        return DefaultLayer.of(null, new FeatureCollection((x) -> {}), RTree.create());
    }

    private Rectangle jtsToRectangle(Geometry geometry) {
        Envelope envelope = geometry.getEnvelopeInternal();
        return Geometries.rectangle(envelope.getMinX(), envelope.getMinY(),
                envelope.getMaxX(), envelope.getMaxY());
    }

    @Override
    public AbstractDefaultLayer withFeatures(Collection<Feature> features) {
        RTree<Feature, Rectangle> spatialIndex = spatialIndex().add(
                features.stream().map(feature ->
                        EntryDefault.entry(feature, jtsToRectangle(feature.geometry())))
                .collect(Collectors.toList())
        );
       return DefaultLayer.of(this, this.features().append(features), spatialIndex);
    }

    @Override
    public Iterable<Feature> query(Geometry geometry) {
        return spatialIndex().search(jtsToRectangle(geometry))
                .map(Entry::value)
                .toBlocking().toIterable();
    }
}
