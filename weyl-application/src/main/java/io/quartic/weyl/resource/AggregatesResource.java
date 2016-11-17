package io.quartic.weyl.resource;

import io.quartic.weyl.core.compute.AbstractHistogram;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.model.EntityId;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Path("/aggregates")
@Value.Immutable
public abstract class AggregatesResource {
    private static final Logger LOG = LoggerFactory.getLogger(AggregatesResource.class);

    public static AggregatesResource of(EntityStoreQuerier querier) {
        return ImmutableAggregatesResource.of(querier);
    }

    @Value.Parameter
    protected abstract EntityStoreQuerier querier();
    @Value.Default
    protected HistogramCalculator calculator() {
        return new HistogramCalculator();
    }

    @POST
    @Path("/histograms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<AbstractHistogram> getHistogram(List<EntityId> entityIds) {
        LOG.info("Histogramming {} entities", entityIds.size());
        return calculator().calculate(querier().retrieveEntitiesOrThrow(entityIds).collect(toList()));
    }
}
