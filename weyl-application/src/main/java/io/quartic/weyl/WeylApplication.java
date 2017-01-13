package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.assets.AssetsBundle;
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
import io.quartic.common.uid.UidGenerator;
import io.quartic.common.websocket.WebsocketClientSessionFactory;
import io.quartic.common.websocket.WebsocketListener;
import io.quartic.howl.api.HowlClient;
import io.quartic.weyl.core.LayerRouter;
import io.quartic.weyl.core.LayerRouterImpl;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.catalogue.CatalogueWatcher;
import io.quartic.weyl.core.catalogue.CatalogueWatcherImpl;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.render.LayerExporter;
import io.quartic.weyl.core.source.GeoJsonSource;
import io.quartic.weyl.core.source.PostgresSource;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceManager;
import io.quartic.weyl.core.source.SourceManagerImpl;
import io.quartic.weyl.core.source.TerminatorSourceFactory;
import io.quartic.weyl.core.source.WebsocketSource;
import io.quartic.weyl.resource.*;
import io.quartic.weyl.update.AttributesUpdateGenerator;
import io.quartic.weyl.update.ChartUpdateGenerator;
import io.quartic.weyl.update.HistogramsUpdateGenerator;
import io.quartic.weyl.update.SelectionHandler;
import io.quartic.weyl.update.WebsocketEndpoint;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.GeofenceStatusHandler;
import io.quartic.weyl.websocket.LayerListUpdateGenerator;
import io.quartic.weyl.websocket.OpenLayerHandler;
import io.quartic.weyl.websocket.message.AlertMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.websocket.server.ServerEndpointConfig;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.client.ClientUtilsKt.userAgentFor;
import static io.quartic.common.rx.RxUtilsKt.likeBehavior;
import static io.quartic.common.uid.UidUtilsKt.randomGenerator;
import static io.quartic.common.websocket.WebsocketUtilsKt.serverEndpointConfig;
import static rx.Observable.merge;

public class WeylApplication extends ApplicationBase<WeylConfiguration> {
    private final WebsocketBundle websocketBundle = new WebsocketBundle(new ServerEndpointConfig[0]);
    private final UidGenerator<LayerId> lidGenerator = randomGenerator(LayerId::new);   // Use a random generator to ensure MapBox tile caching doesn't break things

    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        bootstrap.addBundle(websocketBundle);
    }

    @Override
    public void runApplication(WeylConfiguration configuration, Environment environment) {
        final WebsocketClientSessionFactory websocketFactory = new WebsocketClientSessionFactory(getClass());

        final CatalogueWatcher catalogueWatcher = CatalogueWatcherImpl.of(
                new WebsocketListener.Factory(configuration.getCatalogueWatchUrl(), websocketFactory)
        );

        final SourceManager sourceManager = SourceManagerImpl.builder()
                .catalogueEvents(catalogueWatcher.events())
                .sourceFactories(createSourceFactories(configuration, environment, websocketFactory))
                .scheduler(Schedulers.from(Executors.newScheduledThreadPool(2)))
                .build();

        final ComputeResource computeResource = ComputeResourceImpl.of(lidGenerator);

        final LayerRouter router = createRouter(merge(
                sourceManager.layerPopulators(),
                computeResource.layerPopulators()
        ));

        final Observable<LayerSnapshotSequence> snapshotSequences = router.snapshotSequences();

        final AlertResource alertResource = new AlertResource();

        environment.jersey().register(computeResource);
        environment.jersey().register(createTileResource(snapshotSequences));
        environment.jersey().register(alertResource);
        environment.jersey().register(createLayerExportResource(snapshotSequences, configuration.getHowlStorageUrl()));

        websocketBundle.addEndpoint(serverEndpointConfig("/ws", createWebsocketEndpoint(snapshotSequences, alertResource)));
    }

    private WebsocketEndpoint createWebsocketEndpoint(Observable<LayerSnapshotSequence> snapshotSequences, AlertResource alertResource) {
        final Collection<ClientStatusMessageHandler> handlers = newArrayList(
                createSelectionHandler(snapshotSequences),
                createOpenLayerHandler(snapshotSequences),
                createGeofenceStatusHandler(snapshotSequences)
        );

        final Observable<SocketMessage> layerListUpdates = snapshotSequences
                .compose(new LayerListUpdateGenerator())
                .compose(likeBehavior());

        final Observable<SocketMessage> messages = merge(
                alertResource.alerts().map(AlertMessageImpl::of),
                layerListUpdates
        );

        return new WebsocketEndpoint(messages, handlers);
    }

    private TileResource createTileResource(Observable<LayerSnapshotSequence> snapshotSequences) {
        return TileResourceImpl.builder()
                .snapshotSequences(snapshotSequences)
                .build();
    }

    private LayerRouter createRouter(Observable<LayerPopulator> populators) {
        return LayerRouterImpl.builder()
                .populators(populators)
                .build();
    }

    private SelectionHandler createSelectionHandler(Observable<LayerSnapshotSequence> snapshotSequences) {
        return new SelectionHandler(
                snapshotSequences,
                newArrayList(
                        new ChartUpdateGenerator(),
                        new HistogramsUpdateGenerator(new HistogramCalculator()),
                        new AttributesUpdateGenerator()
                )
        );
    }

    private OpenLayerHandler createOpenLayerHandler(Observable<LayerSnapshotSequence> snapshotSequences) {
        return new OpenLayerHandler(snapshotSequences, featureConverter());
    }

    private GeofenceStatusHandler createGeofenceStatusHandler(Observable<LayerSnapshotSequence> snapshotSequences) {
        final GeofenceViolationDetector geofenceViolationDetector = new GeofenceViolationDetector();
        return new GeofenceStatusHandler(snapshotSequences, geofenceViolationDetector, featureConverter());
    }

    private LayerExportResource createLayerExportResource(Observable<LayerSnapshotSequence> layerSnapshotSequences,
                                                          String howlStorageUrl) {
        HowlClient howlClient = new HowlClient("weyl", howlStorageUrl);

        LayerExporter layerExporter = new LayerExporter(layerSnapshotSequences, howlClient, featureConverter());
        return new LayerExportResource(layerExporter);
    }

    private Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> createSourceFactories(
            WeylConfiguration configuration,
            Environment environment,
            WebsocketClientSessionFactory websocketFactory
    ) {
        final TerminatorSourceFactory terminatorSourceFactory = TerminatorSourceFactory.builder()
                .listenerFactory(new WebsocketListener.Factory(configuration.getTerminatorUrl(), websocketFactory))
                .metrics(environment.metrics())
                .build();

        final String userAgent = userAgentFor(getClass());

        return ImmutableMap.of(
                PostgresDatasetLocatorImpl.class, config -> PostgresSource.builder()
                        .name(config.metadata().name())
                        .locator((PostgresDatasetLocator) config.locator())
                        .attributesFactory(attributesFactory())
                        .build(),
                GeoJsonDatasetLocatorImpl.class, config -> GeoJsonSource.builder()
                        .name(config.metadata().name())
                        .url(((GeoJsonDatasetLocator) config.locator()).url())
                        .userAgent(userAgent)
                        .converter(featureConverter())
                        .build(),
                WebsocketDatasetLocatorImpl.class, config -> WebsocketSource.builder()
                        .name(config.metadata().name())
                        .listenerFactory(new WebsocketListener.Factory(((WebsocketDatasetLocator) config.locator()).url(), websocketFactory))
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
                        .userAgent(userAgent)
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