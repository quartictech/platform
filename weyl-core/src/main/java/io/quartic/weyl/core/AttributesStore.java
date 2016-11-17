package io.quartic.weyl.core;

import io.quartic.weyl.core.model.AbstractAttributes;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;

import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;

public class AttributesStore {
    private final Map<EntityId, AbstractAttributes> attributes = newConcurrentMap();

    public void putAll(Collection<AbstractFeature> features) {
        features.forEach(f -> attributes.put(f.entityId(), f.attributes()));
    }

    public AbstractAttributes get(EntityId id) {
        return attributes.get(id);
    }
}
