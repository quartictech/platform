package io.quartic.terminator;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func2;

import java.util.List;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ChangeCalculatorShould {
    private final Func2<Integer, Double, String> onAdded = mock(Func2.class);
    private final Func2<Integer, Double, String> onRemoved = mock(Func2.class);

    @Test
    public void invoke_added_callback_for_initial_elements() throws Exception {
        when(onAdded.call(any(), any()))
                .thenReturn("foo")
                .thenReturn("bar");

        final ChangeCalculator<Integer, Double, String> calculator = ChangeCalculator.create(onAdded, onRemoved);

        final List<String> result = toList(calculator.call(ImmutableMap.of(3, 1.0, 4, 2.0)));

        verify(onAdded).call(3, 1.0);
        verify(onAdded).call(4, 2.0);
        verifyZeroInteractions(onRemoved);

        assertThat(result, Matchers.contains("foo", "bar"));
    }

    @Test
    public void invoke_added_callback_for_elements_not_present_previously() throws Exception {
        when(onAdded.call(any(), any()))
                .thenReturn("foo")
                .thenReturn("bar");

        final ChangeCalculator<Integer, Double, String> calculator = ChangeCalculator.create(onAdded, onRemoved,
                ImmutableMap.of(3, 1.0, 4, 2.0));

        final List<String> result = toList(calculator.call(ImmutableMap.of(3, 1.0, 4, 2.0, 5, 3.0, 6, 4.0)));

        verify(onAdded).call(5, 3.0);
        verify(onAdded).call(6, 4.0);
        verifyZeroInteractions(onRemoved);

        assertThat(result, Matchers.contains("foo", "bar"));
    }

    @Test
    public void invoke_removed_callback_for_elements_no_longer_present() throws Exception {
        when(onRemoved.call(any(), any()))
                .thenReturn("foo")
                .thenReturn("bar");

        final ChangeCalculator<Integer, Double, String> calculator = ChangeCalculator.create(onAdded, onRemoved,
                ImmutableMap.of(3, 1.0, 4, 2.0));

        final List<String> result = toList(calculator.call(ImmutableMap.of(4, 2.0)));

        verify(onRemoved).call(3, 1.0);
        verifyZeroInteractions(onAdded);

        assertThat(result, Matchers.contains("foo"));
    }

    @Test
    public void track_state_from_one_call_to_the_next() throws Exception {
        final ChangeCalculator<Integer, Double, String> calculator = ChangeCalculator.create(onAdded, onRemoved);

        toList(calculator.call(ImmutableMap.of(3, 1.0)));
        toList(calculator.call(ImmutableMap.of(3, 1.0, 4, 2.0)));

        verify(onAdded, times(1)).call(3, 1.0);
        verify(onAdded, times(1)).call(4, 2.0);
    }

    private List<String> toList(Observable<String> observable) {
        return observable.toList().toBlocking().single();
    }
}
