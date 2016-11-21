package io.quartic.weyl.update;

import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class AttributesUpdateGenerator implements SelectionDrivenUpdateGenerator {
    @Override
    public String name() {
        return "attributes";
    }

    @Override
    public Map<EntityId, Attributes> generate(Collection<Feature> entities) {
        return entities.stream().collect(toMap(Feature::entityId, this::externalAttributes));
    }

    private Attributes externalAttributes(Feature feature) {
        final AttributesImpl.Builder builder = AttributesImpl.builder();
        feature.attributes().attributes().entrySet().stream()
                .filter(e -> !(e.getValue() instanceof ComplexAttribute || e.getValue() instanceof Map))
                .forEach(builder::attribute);
        return builder.build();
    }
}
