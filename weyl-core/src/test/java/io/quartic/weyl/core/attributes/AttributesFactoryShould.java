package io.quartic.weyl.core.attributes;

import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.Attributes;
import org.junit.Test;

import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class AttributesFactoryShould {

    private final AttributesFactory factory = new AttributesFactory();

    @Test
    public void produce_attributes() throws Exception {
        final Attributes attributes = buildAttributes();

        assertThat(attributes.attributes(), equalTo(map(
                entry(name("name"), "Oliver"),
                entry(name("weight"), 80.0),
                entry(name("height"), 185.0)
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

        assertThat(attributesA.attributes(), equalTo(map(
                entry(name("name"), "Oliver"),
                entry(name("weight"), 80.0),
                entry(name("height"), 185.0)
        )));

        assertThat(attributesB.attributes(), equalTo(map(
                entry(name("name"), "Arlo"),
                entry(name("weight"), 200.0),
                entry(name("height"), 170.0)
        )));
    }

    @Test
    public void share_attribute_names() throws Exception {
        final Attributes attributesA = builder().put("foo", 1.2).build();
        final Attributes attributesB = builder().put("foo", 3.4).build();

        assertThat(attributesA.attributes().keySet().iterator().next(),
                sameInstance(attributesB.attributes().keySet().iterator().next()));
    }

    @SuppressWarnings("RedundantStringConstructorCall")
    @Test
    public void intern_string_values() throws Exception {
        final Attributes attributesA = builder().put("foo", new String("hello")).build();
        final Attributes attributesB = builder().put("bar", new String("hello")).build();

        assertThat(attributesA.attributes().get(name("foo")),
                sameInstance(attributesB.attributes().get(name("bar"))));
    }

    // This is really just to cover the (index < values.size()) behaviour
    @Test
    public void not_throw_when_attempting_to_get_an_attribute_name_that_was_added_later() throws Exception {
        final Attributes attributes = buildAttributes();
        builder().put("wat", 32).build();

        assertThat(attributes.attributes().get(name("wat")), nullValue());
    }

    @Test
    public void return_true_for_entries_corresponding_to_missing_attribute_names() throws Exception {
        builder().put("wat", 32).build();
        final Attributes attributes = buildAttributes();

        assertThat(attributes.attributes().containsKey(name("wat")), equalTo(true));
    }

    @Test
    public void not_list_entries_corresponding_to_missing_attribute_names() throws Exception {
        builder().put("wat", 32).build();
        final Attributes attributes = buildAttributes();

        assertThat(attributes.attributes(), equalTo(map(
                entry(name("name"), "Oliver"),
                entry(name("weight"), 80.0),
                entry(name("height"), 185.0),
                entry(name("wat"), null)
        )));
    }

    @Test
    public void allow_building_starting_with_existing_attributes() throws Exception {
        final Attributes original = () -> ImmutableMap.of(name("foo"), 1.2);

        final Attributes attributes = factory.builder(original).put("bar", 3.4).build();

        assertThat(attributes.attributes(), equalTo(map(
                entry(name("foo"), 1.2),
                entry(name("bar"), 3.4)
        )));
    }

    private AttributeName name(String name) {
        return new AttributeName(name);
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

    private AttributesFactory.AttributesBuilder builder() {
        return factory.builder();
    }
}
