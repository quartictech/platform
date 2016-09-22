import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as=ImmutablePoint.class)
@JsonDeserialize(as=ImmutablePoint.class)
public interface Point extends Geometry {
    default @Value.Parameter String type() { return "Point"; };
}
