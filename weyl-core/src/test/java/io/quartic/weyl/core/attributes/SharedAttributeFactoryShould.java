package io.quartic.weyl.core.attributes;

import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Attributes;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class SharedAttributeFactoryShould {

    private final SharedAttributesFactory factory = new SharedAttributesFactory();

    @Test
    public void produce_attributes() throws Exception {
        final Attributes attributes = buildAttributes();

        assertThat(attributes.attributes(), equalTo(ImmutableMap.of(
                name("name"), "Oliver",
                name("weight"), 80.0,
                name("height"), 185.0
        )));
    }

    @Test
    public void produce_attributes_that_support_get() throws Exception {
        final Attributes attributes = buildAttributes();

        assertThat(attributes.attributes().get(name("name")), equalTo("Oliver"));
        assertThat(attributes.attributes().get(name("weight")), equalTo(80.0));
        assertThat(attributes.attributes().get(name("height")), equalTo(185.0));
    }

    @Test
    public void produce_attributes_that_support_containsKey() throws Exception {
        final Attributes attributes = buildAttributes();

        assertThat(attributes.attributes().containsKey(name("name")), equalTo(true));
        assertThat(attributes.attributes().containsKey(name("disease")), equalTo(false));
    }

    @Test
    public void produce_attributes_that_support_containsValue() throws Exception {
        final Attributes attributes = buildAttributes();

        assertThat(attributes.attributes().containsValue("Oliver"), equalTo(true));
        assertThat(attributes.attributes().containsValue("Arlo"), equalTo(false));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void produce_immutable_results() throws Exception {
        final Attributes attributes = buildAttributes();
        attributes.attributes().put(name("foo"), "bar");
    }

    @Test
    public void handle_multiple_sets_of_attributes() throws Exception {
        final Attributes attributesA = buildAttributes();
        final Attributes attributesB = buildAttributes("Arlo", 200.0, 170.0);

        assertThat(attributesA.attributes(), equalTo(ImmutableMap.of(
                name("name"), "Oliver",
                name("weight"), 80.0,
                name("height"), 185.0
        )));

        assertThat(attributesB.attributes(), equalTo(ImmutableMap.of(
                name("name"), "Arlo",
                name("weight"), 200.0,
                name("height"), 170.0
        )));
    }

    @Test
    public void not_throw_when_attempting_to_get_an_attribute_name_that_was_added_later() throws Exception {
        final Attributes attributesA = buildAttributes();
        builder().put("wat", 32).build();

        assertThat(attributesA.attributes().get(name("wat")), nullValue());
    }

    private AttributeNameImpl name(String name) {
        return AttributeNameImpl.of(name);
    }

    private Attributes buildAttributes() {
        return buildAttributes("Oliver", 80.0, 185.0);
    }

    private Attributes buildAttributes(String name, double weight, double height) {
        return builder()
                .put("name", name)
                .put("weight", weight)
                .put("height", height)
                .build();
    }

    private SharedAttributesFactory.AttributesBuilder builder() {
        return factory.attributesBuilder();
    }

    // TODO: can get a name that didn't previously exist, and should return null (rather than throw)
}
