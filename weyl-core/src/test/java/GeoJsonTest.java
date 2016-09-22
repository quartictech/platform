import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.List;

public class GeoJsonTest {


    public static List<Double> point(double x, double y) {
        return ImmutableList.of(x, y);
    }

    public static List<List<Double>> points(List<Double> points) {
        return ImmutableList.of(points);
    }

    @Test
    public void name() throws Exception {

        FeatureCollection fc = ImmutableFeatureCollection.of(
                ImmutableList.of(
                    ImmutableFeature.of(
                            ImmutablePoint.of(points(point(102.0, 0.5))),
                            ImmutableMap.of()
                    )
                )
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(System.out, fc);
    }
}
