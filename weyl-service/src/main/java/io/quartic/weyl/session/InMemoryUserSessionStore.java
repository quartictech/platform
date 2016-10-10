package io.quartic.weyl.session;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserSessionStore implements UserSessionStore {
    private final Map<SessionId, UserSession> sessions = new ConcurrentHashMap<>();
    private static final UserSession DEFAULT_SESSION = UserSession.of(ImmutableMap.of());

    @Override
    public UserSession get(SessionId sessionId) {
        return sessions.getOrDefault(sessionId, DEFAULT_SESSION);
    }

    @Override
    public void set(SessionId sessionId, UserSession userSession) {
        sessions.put(sessionId, userSession);
    }
}
