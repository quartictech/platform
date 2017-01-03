package io.quartic.weyl.core.source;

import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.GeoJsonParser;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;

@Value.Immutable
public abstract class GeoJsonSource implements Source {
    public static ImmutableGeoJsonSource.Builder builder() {
        return ImmutableGeoJsonSource.builder();
    }

    private static final Logger LOG = LoggerFactory.getLogger(GeoJsonSource.class);

    protected abstract String name();
    protected abstract String url();
    protected abstract FeatureConverter converter();

    @Override
    public Observable<LayerUpdate> observable() {
        return Observable.create(sub -> {
            try {
                sub.onNext(LayerUpdateImpl.of(importAllFeatures()));
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
        InputStream inputStream = parseURL(url()).openStream();
        FeatureCollection featureCollection = new FeatureCollection(newArrayList(new GeoJsonParser(inputStream)));
        return converter().toModel(featureCollection);
    }

    private URL parseURL(String url) throws MalformedURLException {
        return new URL(url);
    }
}
