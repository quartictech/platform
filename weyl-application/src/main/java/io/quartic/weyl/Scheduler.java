package io.quartic.weyl;

import com.google.common.collect.ImmutableList;
import io.dropwizard.lifecycle.Managed;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler implements Managed {
    @SweetStyle
    @Value.Immutable
    public interface AbstractScheduleItem {
        long periodSeconds();
        Runnable runnable();
    }

    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    private final List<ScheduleItem> scheduleItems;

    public Scheduler(List<ScheduleItem> scheduleItems) {
        this.scheduleItems = ImmutableList.copyOf(scheduleItems);
    }

    @Override
    public void start() throws Exception {
        LOG.info("Starting schedule");
        scheduleItems.forEach(item ->
                service.scheduleAtFixedRate(item.runnable(), item.periodSeconds(), item.periodSeconds(), TimeUnit.SECONDS)
        );
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Stopping schedule");
        service.shutdown();
    }
}
