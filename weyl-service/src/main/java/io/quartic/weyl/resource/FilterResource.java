package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableSet;
import io.dropwizard.jersey.sessions.Session;
import io.quartic.weyl.core.filter.*;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.session.SessionId;
import io.quartic.weyl.session.UserSession;
import io.quartic.weyl.session.UserSessionStore;

import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Path("/filter")
@Consumes("application/json")
public class FilterResource {
    private final UserSessionStore sessionStore;

    public FilterResource(UserSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    private static Filter filterFromFilterState(Map<String, List<String>> filterState) {
        return BooleanFilter.of(BooleanOperator.NOR,
                filterState.entrySet().stream()
                        .map(entry ->
                                SetPropertyValueFilter.of(entry.getKey(), SetPropertyValueOperator.IN, ImmutableSet.copyOf(entry.getValue()), true))
                        .collect(Collectors.toList()));
    }

    @PUT
    @Path("/{layerId}")
    public void putFilterState(@Session HttpSession httpSession,
                        @PathParam("layerId") String layerId,
                        Map<String, List<String>> filterState) {
        Filter filter = filterFromFilterState(filterState);

        sessionStore.update(SessionId.of(httpSession.getId()), userSession ->
                UserSession.builder().from(userSession).putFilters(LayerId.of(layerId), filter)
                .build());
    }
}
