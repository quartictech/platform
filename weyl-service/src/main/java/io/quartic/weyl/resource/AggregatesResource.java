package io.quartic.weyl.resource;

import io.quartic.weyl.core.compute.AbstractHistogram;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Path("/aggregates")
public class AggregatesResource {
    private static final Logger LOG = LoggerFactory.getLogger(AggregatesResource.class);
    private final FeatureStore featureStore;
    private final HistogramCalculator calculator = new HistogramCalculator();

    public AggregatesResource(FeatureStore featureStore) {
        this.featureStore = featureStore;
    }

    @POST
    @Path("/histograms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<AbstractHistogram> getHistogram(List<FeatureId> featureIds) {
        Collection<Feature> features = featureIds.stream()
                .map(featureStore::get)
                .collect(Collectors.toSet());

        return calculator.calculate(features);
    }
}
