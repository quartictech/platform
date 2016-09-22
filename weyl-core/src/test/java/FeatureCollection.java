import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as=ImmutableFeatureCollection.class)
@JsonDeserialize(as=ImmutableFeatureCollection.class)
public interface FeatureCollection {
    @Value.Parameter default String type() { return "FeatureCollection"; }
    @Value.Parameter List<Feature> features();
}
