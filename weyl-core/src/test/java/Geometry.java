import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as=ImmutableGeometry.class)
@JsonDeserialize(as=ImmutableGeometry.class)
public interface Geometry {
    @Value.Parameter String type();
    @Value.Parameter List<List<Double>> coordinates();
}
