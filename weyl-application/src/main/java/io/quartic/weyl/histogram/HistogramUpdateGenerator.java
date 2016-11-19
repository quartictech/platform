package io.quartic.weyl.histogram;

import io.quartic.weyl.UpdateMessageGenerator;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.message.SocketMessage;

import java.util.Collection;

public class HistogramUpdateGenerator implements UpdateMessageGenerator {
    private final HistogramCalculator calculator;

    public HistogramUpdateGenerator(HistogramCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public SocketMessage generate(int seqNum, Collection<AbstractFeature> entities) {
        return HistogramUpdateMessage.of(seqNum, calculator.calculate(entities));
    }
}
