package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.catalogue.CatalogueWatcher;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.model.CloudGeoJsonDatasetLocator;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetLocator;
import io.quartic.catalogue.api.model.DatasetNamespace;
import io.quartic.catalogue.api.model.GeoJsonDatasetLocator;
import io.quartic.catalogue.api.model.PostgresDatasetLocator;
import io.quartic.catalogue.api.model.WebsocketDatasetLocator;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.uid.UidGenerator;
import io.quartic.common.websocket.WebsocketClientSessionFactory;
import io.quartic.common.websocket.WebsocketListener;
import io.quartic.howl.api.HowlClient;
import io.quartic.weyl.WeylConfiguration.MapConfig;
import io.quartic.weyl.core.LayerRouter;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.export.HowlGeoJsonLayerWriter;
import io.quartic.weyl.core.export.LayerExporter;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.source.GeoJsonSource;
import io.quartic.weyl.core.source.PostgresSource;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceManager;
import io.quartic.weyl.core.source.WebsocketSource;
import io.quartic.weyl.resource.AlertResource;
import io.quartic.weyl.resource.ComputeResource;
import io.quartic.weyl.resource.LayerExportResource;
import io.quartic.weyl.resource.TileResource;
import io.quartic.weyl.update.AttributesUpdateGenerator;
import io.quartic.weyl.update.ChartUpdateGenerator;
import io.quartic.weyl.update.DetailsUpdateGenerator;
import io.quartic.weyl.update.HistogramsUpdateGenerator;
import io.quartic.weyl.update.SelectionHandler;
import io.quartic.weyl.update.WebsocketEndpoint;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.GeofenceStatusHandler;
import io.quartic.weyl.websocket.LayerListUpdateGenerator;
import io.quartic.weyl.websocket.OpenLayerHandler;
import io.quartic.weyl.websocket.message.AlertMessage;
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
                new WebsocketListener.Factory(configuration.getCatalogue().getWatchUrl(), websocketFactory),
                configuration.getDefaultCatalogueNamespace()
        );

        final SourceManager sourceManager = new SourceManager(
                catalogueWatcher.getEvents(),
                createSourceFactories(configuration, environment, websocketFactory),
                Schedulers.from(Executors.newScheduledThreadPool(2))
        );

        final ComputeResource computeResource = new ComputeResource(lidGenerator);

        final LayerRouter router = new LayerRouter(merge(
                sourceManager.layerPopulators(),
                computeResource.layerPopulators()
        ));

        final Observable<LayerSnapshotSequence> snapshotSequences = router.snapshotSequences();

        final AlertResource alertResource = new AlertResource();

        HowlClient howlClient = new HowlClient(userAgentFor(getClass()), configuration.getHowlStorageUrl());
        CatalogueService catalogueService = client(CatalogueService.class, getClass(),
                configuration.getCatalogue().getRestUrl());
        environment.jersey().register(computeResource);
        environment.jersey().register(new TileResource(snapshotSequences));
        environment.jersey().register(alertResource);
        environment.jersey().register(createLayerExportResource(snapshotSequences, howlClient, catalogueService, configuration.getDefaultCatalogueNamespace()));

        websocketBundle.addEndpoint(serverEndpointConfig("/ws",
                createWebsocketEndpoint(snapshotSequences, alertResource, configuration.getMap())));
    }

    private WebsocketEndpoint createWebsocketEndpoint(
            Observable<LayerSnapshotSequence> snapshotSequences,
            AlertResource alertResource,
            MapConfig mapConfig
    ) {
        final Collection<ClientStatusMessageHandler> handlers = newArrayList(
                createSelectionHandler(snapshotSequences),
                createOpenLayerHandler(snapshotSequences),
                createGeofenceStatusHandler(snapshotSequences)
        );

        final Observable<SocketMessage> layerListUpdates = snapshotSequences
                .compose(new LayerListUpdateGenerator())
                .compose(likeBehavior());

        final Observable<SocketMessage> messages = merge(
                alertResource.alerts().map(AlertMessage::new),
                layerListUpdates
        );

        return new WebsocketEndpoint(messages, handlers, mapConfig);
    }

    private SelectionHandler createSelectionHandler(Observable<LayerSnapshotSequence> snapshotSequences) {
        return new SelectionHandler(
                snapshotSequences,
                newArrayList(
                        new ChartUpdateGenerator(),
                        new HistogramsUpdateGenerator(new HistogramCalculator()),
                        new AttributesUpdateGenerator(),
                        new DetailsUpdateGenerator()
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

    private LayerExportResource createLayerExportResource(
            Observable<LayerSnapshotSequence> layerSnapshotSequences,
            HowlClient howlClient,
            CatalogueService catalogueService,
            DatasetNamespace defaultCatalogueNamespace
    ) {
        LayerExporter layerExporter = new LayerExporter(
                layerSnapshotSequences,
                new HowlGeoJsonLayerWriter(howlClient, featureConverter()),
                catalogueService,
                defaultCatalogueNamespace);
        return new LayerExportResource(layerExporter);
    }

    private Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> createSourceFactories(
            WeylConfiguration configuration,
            Environment environment,
            WebsocketClientSessionFactory websocketFactory
    ) {
        return ImmutableMap.of(
                PostgresDatasetLocator.class, config -> new PostgresSource(
                        config.getMetadata().getName(),
                        (PostgresDatasetLocator) config.getLocator(),
                        attributesFactory()
                ),
                GeoJsonDatasetLocator.class, config -> geojsonSource(config, ((GeoJsonDatasetLocator) config.getLocator()).getUrl()),
                WebsocketDatasetLocator.class, config -> websocketSource(environment, config,
                        new WebsocketListener.Factory(((WebsocketDatasetLocator) config.getLocator()).getUrl(), websocketFactory),
                                false),
                CloudGeoJsonDatasetLocator.class, config -> {
                    // TODO: can remove the geojsonSource variant once we've regularised the Rain path
                    final CloudGeoJsonDatasetLocator cgjLocator = (CloudGeoJsonDatasetLocator) config.getLocator();
                    return cgjLocator.getStreaming()
                            ? websocketSource(environment, config,
                            new WebsocketListener.Factory(configuration.getRainWsUrlRoot() + cgjLocator.getPath(), websocketFactory), true)
                            : geojsonSource(config, configuration.getHowlStorageUrl() + cgjLocator.getPath());
                }
        );
    }

    private GeoJsonSource geojsonSource(DatasetConfig config, String url) {
        return new GeoJsonSource(
                config.getMetadata().getName(),
                url,
                userAgentFor(getClass()),
                featureConverter()
        );
    }

    private WebsocketSource websocketSource(Environment environment, DatasetConfig config, WebsocketListener.Factory listenerFactory, boolean indexable) {
        return new WebsocketSource(
                config.getMetadata().getName(),
                featureConverter(),
                environment.metrics(),
                listenerFactory,
                indexable
        );
    }

    private FeatureConverter featureConverter() {
        return new FeatureConverter(attributesFactory());
    }

    private AttributesFactory attributesFactory() {
        return new AttributesFactory();
    }
}