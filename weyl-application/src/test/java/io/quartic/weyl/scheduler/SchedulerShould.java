package io.quartic.weyl.scheduler;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;


public class SchedulerShould {
    @Test
    public void not_stop_running_items_upon_error() throws Exception {
        final AtomicInteger calls = new AtomicInteger();

        final ScheduleItem item = ScheduleItem.of(10, () -> {
            if (calls.getAndIncrement() == 0) {
                throw new RuntimeException("oops");
            }
        });

        Scheduler.builder()
                .scheduleItem(item)
                .build()
                .start();

        Thread.sleep(50);

        assertThat(calls.get(), greaterThan(1));
    }


}
