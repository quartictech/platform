package io.quartic.weyl.core.source;

import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import io.quartic.common.geojson.Feature;
import io.quartic.common.geojson.GeoJsonParser;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.api.LayerUpdateType.REPLACE;

@Value.Immutable
public abstract class GeoJsonSource implements Source {
    private static final Logger LOG = LoggerFactory.getLogger(GeoJsonSource.class);
    public static ImmutableGeoJsonSource.Builder builder() {
        return ImmutableGeoJsonSource.builder();
    }

    protected abstract String name();
    protected abstract String url();
    protected abstract String userAgent();
    protected abstract FeatureConverter converter();

    @Override
    public Observable<LayerUpdate> observable() {
        return Observable.create(sub -> {
            try {
                sub.onNext(new LayerUpdate(REPLACE, importAllFeatures()));
            } catch (IOException e) {
                sub.onError(e);
            }
        });
    }

    @Override
    public boolean indexable() {
        return true;
    }

    private List<NakedFeature> importAllFeatures() throws IOException {
        final URLConnection conn = parseURL(url()).openConnection();
        conn.setRequestProperty(HttpHeaders.USER_AGENT, userAgent());

        try (final InputStream inputStream = conn.getInputStream()) {
            Stream<Feature> featureStream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new GeoJsonParser(inputStream), Spliterator.ORDERED),
                false);

            List<NakedFeature> convertedFeatures = Lists.newArrayList();
            List<Feature> features = Lists.newArrayListWithCapacity(10000);

            AtomicLong counter = new AtomicLong();
            featureStream.forEach(feature -> {
                counter.incrementAndGet();
                if (features.size() == 10000) {
                    LOG.info("[{}] importing features: {}", name(), counter);
                    convertedFeatures.addAll(converter().featuresToModel(features));
                    features.clear();
                }
                features.add(feature);
            });
            convertedFeatures.addAll(converter().featuresToModel(features));

            return convertedFeatures;
        }
    }

    private URL parseURL(String url) throws MalformedURLException {
        return new URL(url);
    }
}
