package io.quartic.weyl.update;

import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import static io.quartic.weyl.core.attributes.AttributeUtils.isSimple;
import static java.util.stream.Collectors.toMap;

public class AttributesUpdateGenerator implements SelectionDrivenUpdateGenerator {
    @Override
    public String name() {
        return "attributes";
    }

    @Override
    public Map<EntityId, Attributes> generate(Collection<Feature> entities) {
        return entities.stream().collect(toMap(Feature::getEntityId, this::externalAttributes));
    }

    private Attributes externalAttributes(Feature feature) {
        return () -> feature.getAttributes().attributes().entrySet().stream()
                .filter(e -> isSimple(e.getValue()))
                .collect(toMap(Entry::getKey, Entry::getValue));
    }
}
