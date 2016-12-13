package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.catalogue.api.CloudGeoJsonDatasetLocator;
import io.quartic.catalogue.api.CloudGeoJsonDatasetLocatorImpl;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.GeoJsonDatasetLocator;
import io.quartic.catalogue.api.GeoJsonDatasetLocatorImpl;
import io.quartic.catalogue.api.PostgresDatasetLocator;
import io.quartic.catalogue.api.PostgresDatasetLocatorImpl;
import io.quartic.catalogue.api.TerminatorDatasetLocator;
import io.quartic.catalogue.api.TerminatorDatasetLocatorImpl;
import io.quartic.catalogue.api.WebsocketDatasetLocator;
import io.quartic.catalogue.api.WebsocketDatasetLocatorImpl;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.client.WebsocketClientSessionFactory;
import io.quartic.common.client.WebsocketListener;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.common.uid.RandomUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.LayerStoreImpl;
import io.quartic.weyl.core.ObservableStore;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.catalogue.CatalogueWatcher;
import io.quartic.weyl.core.catalogue.CatalogueWatcherImpl;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.LiveLayerChange;
import io.quartic.weyl.core.geofence.LiveLayerChangeAggregator;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerIdImpl;
import io.quartic.weyl.core.source.GeoJsonSource;
import io.quartic.weyl.core.source.PostgresSource;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceManager;
import io.quartic.weyl.core.source.SourceManagerImpl;
import io.quartic.weyl.core.source.TerminatorSourceFactory;
import io.quartic.weyl.core.source.WebsocketSource;
import io.quartic.weyl.resource.AlertResource;
import io.quartic.weyl.resource.ComputeResource;
import io.quartic.weyl.resource.ComputeResourceImpl;
import io.quartic.weyl.resource.LayerResource;
import io.quartic.weyl.resource.TileResource;
import io.quartic.weyl.update.AttributesUpdateGenerator;
import io.quartic.weyl.update.ChartUpdateGenerator;
import io.quartic.weyl.update.HistogramsUpdateGenerator;
import io.quartic.weyl.update.SelectionHandler;
import io.quartic.weyl.update.UpdateServer;
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
    private final WebsocketBundle websocketBundle = new WebsocketBundle(new ServerEndpointConfig[0]);
    private final UidGenerator<LayerId> lidGenerator = RandomUidGenerator.of(LayerIdImpl::of);   // Use a random generator to ensure MapBox tile caching doesn't break things

    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        bootstrap.addBundle(websocketBundle);
    }

    @Override
    public void runApplication(WeylConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response
        environment.jersey().setUrlPattern("/api/*");

        final WebsocketClientSessionFactory websocketFactory = new WebsocketClientSessionFactory(getClass());

        final CatalogueWatcher catalogueWatcher = CatalogueWatcherImpl.of(
                WebsocketListener.Factory.of(configuration.getCatalogueWatchUrl(), websocketFactory)
        );

        final SourceManager sourceManager = SourceManagerImpl.builder()
                .catalogueEvents(catalogueWatcher.events())
                .sourceFactories(createSourceFactories(configuration, environment, websocketFactory))
                .scheduler(Schedulers.from(Executors.newScheduledThreadPool(2)))
                .build();

        final ComputeResource computeResource = ComputeResourceImpl.of(lidGenerator);

        final ObservableStore<EntityId, Feature> entityStore = new ObservableStore<>();
        final LayerStore layerStore = LayerStoreImpl.builder()
                .populators(merge(
                        sourceManager.layerPopulators(),
                        computeResource.layerPopulators()
                ))
                .entityStore(entityStore)
                .build();

        final Observable<LiveLayerChange> liveLayerChanges = LiveLayerChangeAggregator.layerChanges(
                layerStore.allLayers(),
                layerStore::liveLayerChanges
        ).share();

        final AlertResource alertResource = new AlertResource();

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new LayerResource(layerStore));
        environment.jersey().register(computeResource);
        environment.jersey().register(new TileResource(layerStore));
        environment.jersey().register(alertResource);

        final SelectionHandler selectionHandler = createSelectionHandler(entityStore);
        final LayerSubscriptionHandler layerSubscriptionHandler = createLayerSubscriptionHandler(layerStore);

        websocketBundle.addEndpoint(ServerEndpointConfig.Builder
                .create(UpdateServer.class, "/ws")
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        //noinspection unchecked
                        return (T) createUpdateServer(
                                layerStore,
                                liveLayerChanges,
                                selectionHandler,
                                layerSubscriptionHandler,
                                alertResource
                        );
                    }
                })
                .build()
        );
    }

    private UpdateServer createUpdateServer(
            LayerStore layerStore,
            Observable<LiveLayerChange> liveLayerChanges,
            SelectionHandler selectionHandler,
            LayerSubscriptionHandler layerSubscriptionHandler,
            AlertResource alertResource
    ) {
        // These are per-user so each user has their own geofence state
        final GeofenceStore geofenceStore = new GeofenceStore(liveLayerChanges);
        final AlertProcessor alertProcessor = new AlertProcessor(geofenceStore);
        final GeofenceStatusHandler geofenceStatusHandler = createGeofenceStatusHandler(geofenceStore, layerStore);

        return new UpdateServer(
                merge(alertProcessor.alerts(), alertResource.alerts()),
                newArrayList(
                        selectionHandler,
                        layerSubscriptionHandler,
                        geofenceStatusHandler
                ));
    }

    private SelectionHandler createSelectionHandler(ObservableStore<EntityId, Feature> entityStore) {
        return new SelectionHandler(
                newArrayList(
                        new ChartUpdateGenerator(),
                        new HistogramsUpdateGenerator(new HistogramCalculator()),
                        new AttributesUpdateGenerator()
                ),
                Multiplexer.create(entityStore::get));
    }

    private LayerSubscriptionHandler createLayerSubscriptionHandler(LayerStore layerStore) {
        return new LayerSubscriptionHandler(layerStore, featureConverter());
    }

    private GeofenceStatusHandler createGeofenceStatusHandler(GeofenceStore geofenceStore, LayerStore layerStore) {
        return new GeofenceStatusHandler(geofenceStore, layerStore, featureConverter());
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