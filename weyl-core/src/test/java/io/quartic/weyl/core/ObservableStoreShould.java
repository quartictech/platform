package io.quartic.weyl.core;

import org.junit.Test;
import rx.observers.TestSubscriber;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class ObservableStoreShould {

    private final ObservableStore<Key, Value> store = new ObservableStore<>();

    @Test
    public void emit_value_changes_after_subscribed() throws Exception {
        final Key key = new Key();
        final Value valueA = new Value(key);
        final Value valueB = new Value(key);

        final TestSubscriber<Value> sub = subscriberFor(store, key);
        store.putAll(Value::key, newArrayList(valueA));
        store.putAll(Value::key, newArrayList(valueB));

        sub.awaitValueCount(2, 100, MILLISECONDS);
        assertThat(sub.getOnNextEvents(), contains(valueA, valueB));
    }

    @Test
    public void emit_values_for_different_ids() throws Exception {
        final Key keyA = new Key();
        final Key keyB = new Key();
        final Value valueA = new Value(keyA);
        final Value valueB = new Value(keyB);

        final TestSubscriber<Value> subA = subscriberFor(store, keyA);
        final TestSubscriber<Value> subB = subscriberFor(store, keyB);
        store.putAll(Value::key, newArrayList(valueA));
        store.putAll(Value::key, newArrayList(valueB));

        subA.awaitValueCount(1, 100, MILLISECONDS);
        subB.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(subA.getOnNextEvents(), contains(valueA));
        assertThat(subB.getOnNextEvents(), contains(valueB));
    }

    @Test
    public void emit_current_value_on_subscription() throws Exception {
        final Key key = new Key();
        final Value value = new Value(key);

        store.putAll(Value::key, newArrayList(value));  // Before subscription

        final TestSubscriber<Value> sub = subscriberFor(store, key);

        sub.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(sub.getOnNextEvents(), contains(value));
    }

    private TestSubscriber<Value> subscriberFor(ObservableStore<Key, Value> store, Key key) {
        final TestSubscriber<Value> sub = TestSubscriber.create();
        store.get(key).subscribe(sub);
        return sub;
    }

    private static class Key {}

    private static class Value {
        private final Key key;

        private Value(Key key) {
            this.key = key;
        }

        Key key() { return key; }
    }
}
