package io.nats.bridge.task;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBridgeTasksManager;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private AtomicReference<Exception> lastError = new AtomicReference<>();

    public MessageBridgeTasksManagerImpl(final String name, final Logger logger, final Function<String, MessageBridge> bridgeFactory,
                                         final int workers, final int tasks, final Duration pollDuration, boolean namePerTask) {
        this.name = name;
        this.logger = logger;
        this.bridgeBuilder = bridgeFactory;
        this.workers = workers;
        this.tasks = tasks;
        this.pool = Executors.newWorkStealingPool(workers);
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
                pool.submit(runner::process);
            }
        }catch (Exception ex) {
            logger.error("Error starting message bridge task manager", ex);
        }
    }

    private BridgeTaskRunner createBridgeTaskRunner(final int worker, List<MessageBridge> bridges) {

        BridgeTaskRunnerBuilder bridgeTaskRunnerBuilder = BridgeTaskRunnerBuilder.builder().withName(name()).withPollDuration(pollDuration)
                .withMessageBridges(bridges).withWorker(worker).withProcessNotifier(new ProcessNotifier() {
                    boolean started;
                    boolean stopped;

                    @Override
                    public boolean stopRunning() {
                        return stop.get();
                    }

                    @Override
                    public void notifyStopped() {
                        stopped = true;
                        logger.info("Worker stopped {} {}", name, worker);
                    }

                    @Override
                    public void notifyStoppedByError(final Exception ex) {

                        logger.info("Worker sent an error {} {}", name, worker);
                        lastError.set(ex);
                    }

                    @Override
                    public void notifyStarted() {
                        started = true;
                        logger.info("Worker sent a start signal {} {}", name, worker);
                        startedCount.incrementAndGet();
                    }

                    @Override
                    public boolean wasStarted() {
                        return started;
                    }

                    @Override
                    public boolean wasStopped() {
                        return stopped;
                    }

                    @Override
                    public boolean wasError() {
                        return !MessageBridgeTasksManagerImpl.this.isHealthy();
                    }
                });

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
