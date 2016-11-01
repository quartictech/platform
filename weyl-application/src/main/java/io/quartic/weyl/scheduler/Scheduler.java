package io.quartic.weyl.scheduler;

import io.dropwizard.lifecycle.Managed;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Value.Style(
        add = "*",
        addAll = "all*",
        depluralize = true
)
@Value.Immutable
public abstract class Scheduler implements Managed {
    @SweetStyle
    @Value.Immutable
    public interface AbstractScheduleItem {
        long periodMilliseconds();
        Runnable runnable();
    }

    public static ImmutableScheduler.Builder builder() {
        return ImmutableScheduler.builder();
    }

    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    protected abstract List<ScheduleItem> scheduleItems();

    @Override
    public void start() throws Exception {
        LOG.info("Starting schedule");

        scheduleItems().forEach(item ->
                service.scheduleAtFixedRate(makeSafe(item.runnable()), item.periodMilliseconds(), item.periodMilliseconds(), TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Stopping schedule");
        service.shutdown();
    }

    private Runnable makeSafe(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                LOG.error("Error running item", e);
            }
        };
    }
}
