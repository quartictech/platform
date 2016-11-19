package io.quartic.weyl;

import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.message.SocketMessage;

import java.util.Collection;

public interface UpdateMessageGenerator {
    SocketMessage generate(Collection<AbstractFeature> entities);
}
