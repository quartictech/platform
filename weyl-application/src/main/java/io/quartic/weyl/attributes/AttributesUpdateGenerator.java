package io.quartic.weyl.attributes;

import io.quartic.weyl.UpdateMessageGenerator;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.message.SocketMessage;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class AttributesUpdateGenerator implements UpdateMessageGenerator {
    private static final Logger LOG = getLogger(AttributesUpdateGenerator.class);

    @Override
    public SocketMessage generate(int seqNum, Collection<AbstractFeature> entities) {
        LOG.info("seqNum: {}", seqNum);
        final AttributesUpdateMessage.Builder builder = AttributesUpdateMessage.builder();
        entities.forEach(e -> builder.attribute(e.entityId(), externalAttributes(e)));
        return builder.seqNum(seqNum).build();
    }

    private Attributes externalAttributes(AbstractFeature feature) {
        final Attributes.Builder builder = Attributes.builder();
        feature.attributes().attributes().entrySet().stream()
                .filter(e -> !(e.getValue() instanceof ComplexAttribute || e.getValue() instanceof Map))
                .forEach(builder::attribute);
        return builder.build();
    }
}
