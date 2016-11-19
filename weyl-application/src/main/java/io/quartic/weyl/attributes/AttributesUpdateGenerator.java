package io.quartic.weyl.attributes;

import io.quartic.weyl.UpdateMessageGenerator;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.message.SocketMessage;

import java.util.Collection;
import java.util.Map;

public class AttributesUpdateGenerator implements UpdateMessageGenerator {
    @Override
    public SocketMessage generate(Collection<AbstractFeature> entities) {
        final AttributesUpdateMessage.Builder builder = AttributesUpdateMessage.builder();
        entities.forEach(e -> builder.attribute(e.entityId(), externalAttributes(e)));
        return builder.build();
    }

    private Attributes externalAttributes(AbstractFeature feature) {
        final Attributes.Builder builder = Attributes.builder();
        feature.attributes().attributes().entrySet().stream()
                .filter(e -> !(e.getValue() instanceof ComplexAttribute || e.getValue() instanceof Map))
                .forEach(builder::attribute);
        return builder.build();
    }
}
