package io.quartic.weyl.core.source;

import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.util.stream.Collectors.toList;

@Value.Immutable
public abstract class GeoJsonSource implements Source {
    public static ImmutableGeoJsonSource.Builder builder() {
        return ImmutableGeoJsonSource.builder();
    }

    private static final Logger LOG = LoggerFactory.getLogger(GeoJsonSource.class);

    protected abstract String name();
    protected abstract String url();
    @Value.Default
    protected GeometryTransformer geometryTransformer() {
        return GeometryTransformer.wgs84toWebMercator();
    }
    @Value.Derived
    protected FeatureConverter featureConverter() {
        return new FeatureConverter(geometryTransformer());
    }

    @Override
    public Observable<SourceUpdate> observable() {
        return Observable.create(sub -> {
            try {
                sub.onNext(SourceUpdateImpl.of(importAllFeatures()));
                sub.onCompleted();
            } catch (IOException e) {
                sub.onError(e);
            }
        });
    }

    @Override
    public boolean indexable() {
        return true;
    }

    private Collection<NakedFeature> importAllFeatures() throws IOException {
        final FeatureCollection featureCollection = OBJECT_MAPPER.readValue(parseURL(url()), FeatureCollection.class);
        return featureCollection.features().stream()
                .filter(f -> f.geometry().isPresent())
                .map(f -> featureConverter().toModel(f))
                .collect(toList());
    }

    private URL parseURL(String url) throws MalformedURLException {
        return new URL(url);
    }
}
