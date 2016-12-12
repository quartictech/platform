package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.catalogue.api.*;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.client.WebsocketClientSessionFactory;
import io.quartic.common.client.WebsocketListener;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.common.uid.RandomUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.catalogue.CatalogueWatcher;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.LayerStoreImpl;
import io.quartic.weyl.core.ObservableStore;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.LiveLayerChange;
import io.quartic.weyl.core.geofence.LiveLayerChangeAggregator;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerIdImpl;
import io.quartic.weyl.core.source.*;
import io.quartic.weyl.resource.AlertResource;
import io.quartic.weyl.resource.LayerResource;
import io.quartic.weyl.resource.TileResource;
import io.quartic.weyl.update.*;
import io.quartic.weyl.websocket.GeofenceStatusHandler;
import io.quartic.weyl.websocket.LayerSubscriptionHandler;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.websocket.server.ServerEndpointConfig;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static rx.Observable.merge;

public class WeylApplication extends ApplicationBase<WeylConfiguration> {
    private final UidGenerator<LayerId> lidGenerator = RandomUidGenerator.of(LayerIdImpl::of);   // Use a random generator to ensure MapBox tile caching doesn't break things

    private final ObservableStore<EntityId, Feature> entityStore = new ObservableStore<>();
    private final LayerStore layerStore = LayerStoreImpl.builder()
            .entityStore(entityStore).lidGenerator(lidGenerator).build();
    private final Observable<LiveLayerChange> liveLayerChanges = LiveLayerChangeAggregator.layerChanges(
            layerStore.allLayers(),
            layerStore::liveLayerChanges
    );
    private AlertResource alertResource = new AlertResource();

    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        bootstrap.addBundle(configureWebsockets());
    }

    private WebsocketBundle configureWebsockets() {
        // These ones are global
        final SelectionHandler selectionHandler = createSelectionHandler();
        final LayerSubscriptionHandler layerSubscriptionHandler = createLayerSubscriptionHandler();

        final ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(UpdateServer.class, "/ws")
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        //noinspection unchecked
                        return (T) createUpdateServer(selectionHandler, layerSubscriptionHandler);
                    }
                })
                .build();
        return new WebsocketBundle(config);
    }

    private UpdateServer createUpdateServer(SelectionHandler selectionHandler, LayerSubscriptionHandler layerSubscriptionHandler) {
        // As a hack, theses one are per-user so each user has their own geofence state
        final GeofenceStore geofenceStore = new GeofenceStore(liveLayerChanges);
        final AlertProcessor alertProcessor = new AlertProcessor(geofenceStore);
        final GeofenceStatusHandler geofenceStatusHandler = createGeofenceStatusHandler(geofenceStore);

        return new UpdateServer(
                merge(alertProcessor.alerts(), alertResource.alerts()),
                newArrayList(
                        selectionHandler,
                        layerSubscriptionHandler,
                        geofenceStatusHandler
                ));
    }

    private SelectionHandler createSelectionHandler() {
        return new SelectionHandler(
                    newArrayList(
                            new ChartUpdateGenerator(),
                            new HistogramsUpdateGenerator(new HistogramCalculator()),
                            new AttributesUpdateGenerator()
                    ),
                    Multiplexer.create(entityStore::get));
    }

    private LayerSubscriptionHandler createLayerSubscriptionHandler() {
        return new LayerSubscriptionHandler(layerStore, featureConverter());
    }

    private GeofenceStatusHandler createGeofenceStatusHandler(GeofenceStore geofenceStore) {
        return new GeofenceStatusHandler(geofenceStore, layerStore, featureConverter());
    }

    @Override
    public void runApplication(WeylConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response
        environment.jersey().setUrlPattern("/api/*");

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new LayerResource(layerStore));
        environment.jersey().register(new TileResource(layerStore));
        environment.jersey().register(alertResource);

        final WebsocketClientSessionFactory websocketFactory = new WebsocketClientSessionFactory(getClass());

        final CatalogueWatcher catalogueWatcher = CatalogueWatcher.builder()
                .listenerFactory(WebsocketListener.Factory.of(configuration.getCatalogueWatchUrl(), websocketFactory))
                .sourceFactories(createSourceFactories(configuration, environment, websocketFactory))
                .layerStore(layerStore)
                .scheduler(Schedulers.from(Executors.newScheduledThreadPool(2)))
                .build();

        catalogueWatcher.start();
    }

    private Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> createSourceFactories(
            WeylConfiguration configuration,
            Environment environment,
            WebsocketClientSessionFactory websocketFactory) {
        final TerminatorSourceFactory terminatorSourceFactory = TerminatorSourceFactory.builder()
                .listenerFactory(WebsocketListener.Factory.of(configuration.getTerminatorUrl(), websocketFactory))
                .metrics(environment.metrics())
                .build();

        return ImmutableMap.of(
                PostgresDatasetLocatorImpl.class, config -> PostgresSource.builder()
                        .name(config.metadata().name())
                        .locator((PostgresDatasetLocator) config.locator())
                        .attributesFactory(attributesFactory())
                        .build(),
                GeoJsonDatasetLocatorImpl.class, config -> GeoJsonSource.builder()
                        .name(config.metadata().name())
                        .url(((GeoJsonDatasetLocator) config.locator()).url())
                        .converter(featureConverter())
                        .build(),
                WebsocketDatasetLocatorImpl.class, config -> WebsocketSource.builder()
                        .name(config.metadata().name())
                        .listenerFactory(WebsocketListener.Factory.of(((WebsocketDatasetLocator) config.locator()).url(), websocketFactory))
                        .converter(featureConverter())
                        .metrics(environment.metrics())
                        .build(),
                TerminatorDatasetLocatorImpl.class, config -> terminatorSourceFactory.sourceFor(
                        (TerminatorDatasetLocator) config.locator(),
                        featureConverter()
                ),
                CloudGeoJsonDatasetLocatorImpl.class, config -> GeoJsonSource.builder()
                        .name(config.metadata().name())
                        .url(configuration.getHowlStorageUrl() + ((CloudGeoJsonDatasetLocator) config.locator()).path())
                        .converter(featureConverter())
                        .build()
        );
    }

    private FeatureConverter featureConverter() {
        return new FeatureConverter(attributesFactory());
    }

    private AttributesFactory attributesFactory() {
        return new AttributesFactory();
    }
}
