package io.quartic.weyl.session;

import java.util.function.Function;

public interface UserSessionStore {
    UserSession get(SessionId sessionId);
    void set(SessionId sessionId, UserSession userSession);

    default UserSession update(SessionId sessionId, Function<UserSession, UserSession> f) {
        UserSession newSession = f.apply(get(sessionId));
        set(sessionId, newSession);
        return newSession;
    }
}
