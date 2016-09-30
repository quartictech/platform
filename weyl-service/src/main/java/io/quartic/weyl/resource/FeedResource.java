package io.quartic.weyl.resource;

import io.quartic.weyl.core.feed.AbstractElaboratedFeedEvent;
import io.quartic.weyl.core.feed.FeedStore;
import io.quartic.weyl.core.feed.SequenceId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.response.AbstractFeedResponse;
import io.quartic.weyl.response.FeedResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

import static io.quartic.weyl.core.feed.FeedStore.INITIAL_SEQUENCE_ID;
import static java.util.stream.Collectors.toList;

@Path("/feed")
public class FeedResource {
    private final FeedStore feedStore;

    public FeedResource(FeedStore feedStore) {
        this.feedStore = feedStore;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AbstractFeedResponse getEventsSince(
            @QueryParam("layerIds") List<String> layerIds,
            @QueryParam("since") Optional<Integer> sequenceId) {
        final List<AbstractElaboratedFeedEvent> events = feedStore.getEventsSince(
                layerIds.stream().map(LayerId::of).collect(toList()),
                sequenceId.map(SequenceId::of).orElse(INITIAL_SEQUENCE_ID));
        return FeedResponse.of(events, feedStore.getNextSequenceId());
    }

    @GET
    @Path("/nextSequenceId")
    @Produces(MediaType.APPLICATION_JSON)
    public SequenceId getNextSequenceId() {
        return feedStore.getNextSequenceId();
    }
}
