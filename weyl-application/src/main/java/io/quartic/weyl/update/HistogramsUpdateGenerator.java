package io.quartic.weyl.update;

import io.quartic.weyl.core.compute.Histogram;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;

public class HistogramsUpdateGenerator implements SelectionDrivenUpdateGenerator {
    private final HistogramCalculator calculator;

    public HistogramsUpdateGenerator(HistogramCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public String name() {
        return "histograms";
    }

    @Override
    public Collection<Histogram> generate(Collection<Feature> entities) {
        return calculator.calculate(entities);
    }
}
