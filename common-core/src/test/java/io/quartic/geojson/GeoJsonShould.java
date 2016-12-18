package io.quartic.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

public class GeoJsonShould {

    @Test
    public void deserializeBackToOriginal() throws Exception {
        FeatureCollection original = FeatureCollectionImpl.of(list(
                FeatureImpl.of(
                        Optional.empty(),
                        Optional.of(PointImpl.of(list(102.0, 0.5))),
                        ImmutableMap.of()
                ),
                FeatureImpl.of(
                        Optional.empty(),
                        Optional.of(LineStringImpl.of(list(
                                list(102.0, 0.0),
                                list(103.0, 1.0),
                                list(104.0, 0.0),
                                list(105.0, 1.0)
                                ))),
                        ImmutableMap.of()
                ),
                FeatureImpl.of(
                        Optional.empty(),
                        Optional.of(PolygonImpl.of(list(list(
                                list(100.0, 0.0),
                                list(101.0, 0.0),
                                list(101.0, 1.0),
                                list(100.0, 1.0),
                                list(100.0, 0.0)
                        )))),
                        ImmutableMap.of()
                )
        ));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, original);

        String json = sw.toString();

        Assert.assertThat(mapper.readValue(json, FeatureCollection.class), Matchers.equalTo(original));
    }

    @SafeVarargs
    public static <T> List<T> list(T... stuff) {
        return ImmutableList.copyOf(stuff);
    }
}
