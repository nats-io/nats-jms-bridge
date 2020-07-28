package io.nats.bridge.task;

import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NonResumeProcessNotifier implements ProcessNotifier {
    private boolean started;
    private boolean stopped;
    private final AtomicBoolean healthy=new AtomicBoolean(true);
    private final String name;
    private final int worker;
    private final AtomicBoolean stop;
    private final Logger logger;
    private final AtomicInteger startedCount;
    private final AtomicReference<Exception> lastError;


    public NonResumeProcessNotifier(final String name, final int worker,
                                    final AtomicBoolean stop, final Logger logger,
                                    final AtomicInteger startedCount,
                                    final AtomicReference<Exception> lastError
                                    ) {
        this.name = name;
        this.worker = worker;
        this.stop = stop;
        this.logger = logger;
        this.startedCount = startedCount;
        this.lastError = lastError;
    }

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
    public void notifyStoppedByException(Exception ex) {

        logger.error("Worker sent an exception {} {} {}", name, worker, ex);
        lastError.set(ex);
        healthy.set(false);

    }

    @Override
    public void notifyStoppedByError(Throwable ex) {

        logger.error("Worker sent an ERROR {} {} {}", name, worker, ex);
        logger.error("ERROR ", ex);
        healthy.set(false);


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
        return !healthy.get();
    }
}

