package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Objects;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = BucketMeanImpl.class)
@JsonDeserialize(as = BucketMeanImpl.class)
public abstract class BucketMean implements BucketAggregation {
    abstract AttributeName attribute();

    @Override
    public double aggregate(Feature bucket, Collection<Feature> features) {
        if (features.size() == 0) {
            return 0;
        }
        else {
            return features.stream()
                    .map(feature -> feature.attributes().attributes().get(attribute()))
                    .filter(Objects::nonNull)
                    .mapToDouble(BucketUtils::mapToDouble)
                    .sum() / features.size();
        }
    }

    @Override
    public String describe() {
        return String.format("mean(%s)", attribute().name());
    }
}
