package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.response.AbstractAggregatesResponse;
import io.quartic.weyl.response.AggregatesResponse;
import io.quartic.weyl.response.ValueId;
import io.quartic.weyl.response.ValueStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/aggregates")
public class AggregatesResource {
    private static final Logger LOG = LoggerFactory.getLogger(LiveLayerServer.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AbstractAggregatesResponse getAggregates(List<FeatureId> features) {
        LOG.info("Feature IDs = {}", features);

        // TODO
        return AggregatesResponse.of(
                ImmutableMap.of(
                        ValueId.of("123"), ValueStats.of("Alice", 123L),
                        ValueId.of("124"), ValueStats.of("Bob", 53L),
                        ValueId.of("125"), ValueStats.of("Charles", 27L),
                        ValueId.of("126"), ValueStats.of("Dog", 15L),
                        ValueId.of("127"), ValueStats.of("Cat", 47L)
                ),
                ImmutableMultimap.of(
                        "Name", ValueId.of("123"),
                        "Name", ValueId.of("124"),
                        "Name", ValueId.of("125"),
                        "Species", ValueId.of("126"),
                        "Species", ValueId.of("127")
                )
        );
    }
}
