package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.filter.Filter;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.session.SessionId;
import io.quartic.weyl.session.UserSession;
import io.quartic.weyl.session.UserSessionStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class FilterResourceShould {
    private final UserSessionStore userSessionStore = mock(UserSessionStore.class);
    private final FilterResource resource = new FilterResource(userSessionStore);

    @Before
    public void setUp() throws Exception {
        when(userSessionStore.get(SessionId.of("1234"))).thenReturn(UserSession.of(ImmutableMap.of()));
        when(userSessionStore.update(any(SessionId.class), any(Function.class))).thenCallRealMethod();
    }

    @Test
    public void set_a_filter() {
        HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getId()).thenReturn("1234");

        Map<String, List<String>> filterState = ImmutableMap.of("a", ImmutableList.of("1", "2"), "b", ImmutableList.of("3", "4"));
        resource.putFilterState(httpSession, "5678", filterState);

        ArgumentCaptor<UserSession> userSessionArgumentCaptor = ArgumentCaptor.forClass(UserSession.class);
        ArgumentCaptor<SessionId> sessionIdArgumentCaptor = ArgumentCaptor.forClass(SessionId.class);
        verify(userSessionStore).set(sessionIdArgumentCaptor.capture(), userSessionArgumentCaptor.capture());
        Filter filter = userSessionArgumentCaptor.getValue().filters().get(LayerId.of("5678"));
        assertThat(filter, notNullValue());

        Feature feature = feature(ImmutableMap.of("a", "1"));
        assertFalse(filter.apply(feature));
    }

    private Feature feature(Map<String, Object> properties) {
        return ImmutableFeature.of("1234", FeatureId.of("0"), new GeometryFactory().createPoint(new Coordinate(0, 0)), properties);
    }

}
