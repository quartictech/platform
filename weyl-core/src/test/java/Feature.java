import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Map;

public interface Feature {
    String type();
    Geometry geometry();
    @Value.Parameter Map<String,Object> properties();
}
