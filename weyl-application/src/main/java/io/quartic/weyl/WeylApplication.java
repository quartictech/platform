package io.quartic.weyl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.resource.*;
import io.quartic.weyl.update.AttributesUpdateGenerator;
import io.quartic.weyl.update.ChartUpdateGenerator;
import io.quartic.weyl.update.HistogramsUpdateGenerator;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.websocket.server.ServerEndpointConfig;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatortoWgs84;
import static io.quartic.weyl.core.utils.GeometryTransformer.wgs84toWebMercator;

public class WeylApplication extends ApplicationBase<WeylConfiguration> {
    private final GeometryTransformer transformFromFrontend = webMercatortoWgs84();
    private final GeometryTransformer transformToFrontend = wgs84toWebMercator();
    private final UidGenerator<LayerId> lidGenerator = RandomUidGenerator.of(LayerIdImpl::of);   // Use a random generator to ensure MapBox tile caching doesn't break things

    private final ObservableStore<EntityId, Feature> entityStore = new ObservableStore<>();
    private final LayerStore layerStore = LayerStoreImpl.builder()
            .entityStore(entityStore).lidGenerator(lidGenerator).build();

    private final Observable<LiveLayerChange> liveLayerChanges = LiveLayerChangeAggregator.layerChanges(
            layerStore.allLayers(),
            layerStore::liveLayerChanges
    );

    private final GeofenceStore geofenceStore = new GeofenceStore(liveLayerChanges);
    private final AlertProcessor alertProcessor = new AlertProcessor(geofenceStore);

    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        bootstrap.addBundle(configureWebsockets(bootstrap.getObjectMapper()));
    }

    private WebsocketBundle configureWebsockets(ObjectMapper objectMapper) {
        final ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(UpdateServer.class, "/ws")
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return (T) new UpdateServer(layerStore,
                                Multiplexer.create(entityStore::get),
                                newArrayList(
                                        new ChartUpdateGenerator(),
                                        new HistogramsUpdateGenerator(new HistogramCalculator()),
                                        new AttributesUpdateGenerator()
                                ),
                                geofenceStore,
                                alertProcessor,
                                transformFromFrontend,
                                objectMapper);
                    }
                })
                .build();
        return new WebsocketBundle(config);
    }

    @Override
    public void runApplication(WeylConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response
        environment.jersey().setUrlPattern("/api/*");

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new LayerResource(layerStore));
        environment.jersey().register(new TileResource(layerStore));
        environment.jersey().register(new GeofenceResource(transformToFrontend, geofenceStore, layerStore));
        environment.jersey().register(new AlertResource(alertProcessor));

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
                        .url(configuration.getCloudStorageUrl() + ((CloudGeoJsonDatasetLocator) config.locator()).path())
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
