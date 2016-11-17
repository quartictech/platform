package io.quartic.weyl.resource;

import io.quartic.weyl.core.EntityStore;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;
import org.slf4j.Logger;

import javax.ws.rs.NotFoundException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

public class EntityStoreQuerier {
    private static final Logger LOG = getLogger(EntityStoreQuerier.class);

    private final EntityStore entityStore;

    public EntityStoreQuerier(EntityStore entityStore) {
        this.entityStore = entityStore;
    }

    public Stream<AbstractFeature> retrieveEntitiesOrThrow(List<EntityId> entityIds) {
        LOG.info("Retrieving {} entities", entityIds.size());

        final List<Entry<EntityId, AbstractFeature>> entities = map(entityIds, id -> new SimpleEntry<>(id, entityStore.get(id)));
        throwIfAnyEntitiesMissing(entities);

        return entities.stream().map(Entry::getValue);
    }

    private void throwIfAnyEntitiesMissing(List<Entry<EntityId, AbstractFeature>> entities) {
        final List<EntityId> missingIds = entities.stream()
                .filter(e -> e.getValue() == null)
                .map(Entry::getKey)
                .collect(toList());

        if (!missingIds.isEmpty()) {
            final List<String> rawIds = map(missingIds, EntityId::toString);
            final String message = String.format("Could not retrieve for entities: %s", rawIds);
            LOG.error(message);
            throw new NotFoundException(message);
        }
    }

    private <T, U> List<U> map(Collection<T> items, Function<T, U> func) {
        return items.stream().map(func).collect(toList());
    }
}
