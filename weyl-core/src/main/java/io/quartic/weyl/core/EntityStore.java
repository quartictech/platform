package io.quartic.weyl.core;

import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;

import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;

public class EntityStore {
    private final Map<EntityId, AbstractFeature> attributes = newConcurrentMap();

    public void putAll(Collection<AbstractFeature> features) {
        features.forEach(f -> attributes.put(f.entityId(), f));
    }

    public AbstractFeature get(EntityId id) {
        return attributes.get(id);
    }
}
