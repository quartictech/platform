package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.catalogue.CatalogueWatcher;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.CloudGeoJsonDatasetLocator;
import io.quartic.catalogue.api.CloudGeoJsonDatasetLocatorImpl;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.GeoJsonDatasetLocator;
import io.quartic.catalogue.api.GeoJsonDatasetLocatorImpl;
import io.quartic.catalogue.api.PostgresDatasetLocator;
import io.quartic.catalogue.api.PostgresDatasetLocatorImpl;
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
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.export.HowlGeoJsonLayerWriter;
import io.quartic.weyl.core.export.LayerExporter;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.source.GeoJsonSource;
import io.quartic.weyl.core.source.PostgresSource;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceManager;
import io.quartic.weyl.core.source.SourceManagerImpl;
import io.quartic.weyl.core.source.WebsocketSource;
import io.quartic.weyl.resource.AlertResource;
import io.quartic.weyl.resource.ComputeResource;
import io.quartic.weyl.resource.ComputeResourceImpl;
import io.quartic.weyl.resource.LayerExportResource;
import io.quartic.weyl.resource.TileResource;
import io.quartic.weyl.resource.TileResourceImpl;
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
import static io.quartic.common.client.ClientUtilsKt.client;
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

        final CatalogueWatcher catalogueWatcher = new CatalogueWatcher(
                new WebsocketListener.Factory(configuration.getCatalogue().getWatchUrl(), websocketFactory)
        );

        final SourceManager sourceManager = SourceManagerImpl.builder()
                .catalogueEvents(catalogueWatcher.getEvents())
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

        HowlClient howlClient = new HowlClient(userAgentFor(getClass()), configuration.getHowlStorageUrl());
        CatalogueService catalogueService = client(CatalogueService.class, getClass(),
                configuration.getCatalogue().getRestUrl());
        environment.jersey().register(computeResource);
        environment.jersey().register(createTileResource(snapshotSequences));
        environment.jersey().register(alertResource);
        environment.jersey().register(createLayerExportResource(snapshotSequences, howlClient, catalogueService));

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
                                                          HowlClient howlClient, CatalogueService catalogueService) {
        LayerExporter layerExporter = new LayerExporter(layerSnapshotSequences,
                new HowlGeoJsonLayerWriter(howlClient, featureConverter()), catalogueService);
        return new LayerExportResource(layerExporter);
    }

    private Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> createSourceFactories(
            WeylConfiguration configuration,
            Environment environment,
            WebsocketClientSessionFactory websocketFactory
    ) {
        return ImmutableMap.of(
                PostgresDatasetLocatorImpl.class, config -> PostgresSource.builder()
                        .name(config.metadata().name())
                        .locator((PostgresDatasetLocator) config.locator())
                        .attributesFactory(attributesFactory())
                        .build(),
                GeoJsonDatasetLocatorImpl.class, config -> geojsonSource(config, ((GeoJsonDatasetLocator) config.locator()).url()),
                WebsocketDatasetLocatorImpl.class, config -> websocketSource(environment, config,
                        new WebsocketListener.Factory(((WebsocketDatasetLocator) config.locator()).url(), websocketFactory)),
                CloudGeoJsonDatasetLocatorImpl.class, config -> {
                    // TODO: can remove the geojsonSource variant once we've regularised the Rain path
                    final CloudGeoJsonDatasetLocator cgjLocator = (CloudGeoJsonDatasetLocator) config.locator();
                    return cgjLocator.streaming()
                            ? websocketSource(environment, config,
                            new WebsocketListener.Factory(configuration.getRainWsUrlRoot() + cgjLocator.path(), websocketFactory))
                            : geojsonSource(config, configuration.getHowlStorageUrl() + cgjLocator.path());
                }
        );
    }

    private GeoJsonSource geojsonSource(DatasetConfig config, String url) {
        return GeoJsonSource.builder()
        .name(config.metadata().name())
        .url(url)
        .userAgent(userAgentFor(getClass()))
        .converter(featureConverter())
        .build();
    }

    private WebsocketSource websocketSource(Environment environment, DatasetConfig config, WebsocketListener.Factory listenerFactory) {
        return WebsocketSource.builder()
                .name(config.metadata().name())
                .listenerFactory(listenerFactory)
                .converter(featureConverter())
                .metrics(environment.metrics())
                .build();
    }

    private FeatureConverter featureConverter() {
        return new FeatureConverter(attributesFactory());
    }


    private AttributesFactory attributesFactory() {
        return new AttributesFactory();
    }
}