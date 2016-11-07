package io.quartic.weyl.core.source;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.terminator.api.FeatureCollectionWithDatasetId;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.live.LiveEventConverter;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;

import static rx.Observable.empty;
import static rx.Observable.just;

@Value.Immutable
public abstract class TerminatorSourceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TerminatorSourceFactory.class);

    public static ImmutableTerminatorSourceFactory.Builder builder() {
        return ImmutableTerminatorSourceFactory.builder();
    }

    protected abstract String url();
    protected abstract MetricRegistry metrics();
    protected abstract LiveEventConverter converter();
    protected abstract ObjectMapper objectMapper();

    @Value.Default
    protected WebsocketListener listener() {
        return WebsocketListener.builder()
                .name(getClass().getSimpleName())
                .url(url())
                .metrics(metrics())
                .build();
    }

    @Value.Derived
    protected Observable<FeatureCollectionWithDatasetId> collections() {
        return listener().observable().flatMap(this::convert);
    }

    private Observable<FeatureCollectionWithDatasetId> convert(String message) {
        try {
            return just(objectMapper().readValue(message, FeatureCollectionWithDatasetId.class));
        } catch (IOException e) {
            LOG.error("Error converting message", e);
            return empty();
        }
    }

    public Source sourceFor(DatasetId id) {
        return new Source() {
            @Override
            public Observable<SourceUpdate> observable() {
                return collections()
                        .filter(fcwdi -> fcwdi.datasetId().equals(id))
                        .map(fcwdi -> converter().updateFrom(fcwdi.featureCollection()));
            }

            @Override
            public boolean indexable() {
                return false;
            }

            @Override
            public LayerViewType viewType() {
                return LayerViewType.MOST_RECENT;
            }
        };
    }
}
