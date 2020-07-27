package io.nats.bridge.task;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBridgeTasksManager;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class MessageBridgeTasksManagerImpl implements MessageBridgeTasksManager {

    private final String name;
    private final Logger logger;
    private final Function<String, MessageBridge> bridgeBuilder;
    private final int workers;
    private final int tasks;
    private final ExecutorService pool;
    private final Duration pollDuration;
    private final boolean namePerTask;

    private AtomicBoolean stop = new AtomicBoolean();
    private AtomicInteger startedCount = new AtomicInteger();
    private AtomicInteger restartCount = new AtomicInteger();
    private AtomicLong lastRestartTime = new AtomicLong(0L);
    private AtomicReference<Exception> lastError = new AtomicReference<>();
    private CopyOnWriteArrayList<BridgeTaskRunner> runners = new CopyOnWriteArrayList<>();

    public MessageBridgeTasksManagerImpl(final String name, final Logger logger, final Function<String, MessageBridge> bridgeFactory,
                                         final int workers, final int tasks, final Duration pollDuration, boolean namePerTask) {
        this.name = name;
        this.logger = logger;
        this.bridgeBuilder = bridgeFactory;
        this.workers = workers;
        this.tasks = tasks;
        this.pool = Executors.newWorkStealingPool(workers +1);
        this.pollDuration = pollDuration;
        this.namePerTask = namePerTask;
    }

    @Override
    public String name() {
        return name;
    }

    private String createName(int task, int worker) {
        if (namePerTask) {
            return String.format("%s_w%i_t%i", name, worker, task);
        } else {
            return name;
        }
    }

    @Override
    public void start() {
        try {
            for (int worker = 0; worker < workers; worker++) {
                List<MessageBridge> bridges = new ArrayList<>(tasks);
                for (int task = 0; task < tasks; task++) {
                    bridges.add(bridgeBuilder.apply(createName(task, worker)));
                }
                final BridgeTaskRunner runner = createBridgeTaskRunner(worker, bridges);
                runners.add(runner);
                pool.submit(runner::process);
            }
        }catch (Exception ex) {
            lastError.set(ex);
            logger.error("Error starting message bridge task manager", ex);
        }

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
        }
        installMonitor();
    }


    private void installMonitor() {
        pool.submit(() -> {
            while (!stop.get()) {

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {

                }

                boolean allHealthy=true;
                for (BridgeTaskRunner runner : runners) {
                    allHealthy = runner.isHealthy();
                    if (!allHealthy) {
                        break;
                    }
                }

                if (!allHealthy) {

                    final long now = System.currentTimeMillis();

                    final long lastRestart = lastRestartTime.get();

                    final boolean restart = (lastRestart + 60_000) < now;

                    if (restart) {
                        stop.set(true);
                        restartCount.incrementAndGet();
                        logger.error("Restarting bridge task manager {} starts {} restarts {}", name, startedCount.get(), restartCount.get());
                        try {
                            Thread.sleep(20_000);
                        } catch (InterruptedException e) {

                        }

                        final Exception exception = lastError.get();
                        if (exception != null) {
                            logger.error("Restarting bridge task manager with last error", exception);
                            lastError.set(null);
                        }
                        stop.set(false);
                        runners.clear();
                        startedCount.set(0);
                        MessageBridgeTasksManagerImpl.this.start();
                        lastRestartTime.set(System.currentTimeMillis());

                        try {
                            Thread.sleep(5_000);
                        } catch (InterruptedException e) {

                        }
                        logger.error("Restarting bridge task manager {} starts {} restarts {}", name, startedCount.get(), restartCount.get());
                    } else {
                        logger.error("Restarting bridge {} in the next 60 seconds starts {} restarts {}", name, startedCount.get(), restartCount.get());

                        final Exception exception = lastError.get();
                        if (exception != null) {
                            logger.error("Restarting bridge in the next 60 seconds", exception);
                        }
                    }

                }

            }
        });
    }

    private BridgeTaskRunner createBridgeTaskRunner(final int worker, List<MessageBridge> bridges) {

        BridgeTaskRunnerBuilder bridgeTaskRunnerBuilder = BridgeTaskRunnerBuilder.builder().withName(name()).withPollDuration(pollDuration)
                .withMessageBridges(bridges).withWorker(worker).withProcessNotifier(
                        new NonResumeProcessNotifier(name, worker, stop, logger, startedCount, lastError ));

        return bridgeTaskRunnerBuilder.build();
    }

    @Override
    public void close() {
        stop.set(true);
    }

    @Override
    public boolean isHealthy() {
        return lastError.get() == null;
    }

    @Override
    public Exception lastError() {
        return lastError.get();
    }

    @Override
    public boolean wasStarted() {
        return (workers == startedCount.get());
    }

    @Override
    public void clearLastError() {
        lastError.set(null);
    }


}
