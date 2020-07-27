package io.nats.bridge.task;

import io.nats.bridge.MessageBridge;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;

public class BridgeTaskRunner {

    private final List<MessageBridge> messageBridges;
    private final ProcessNotifier processNotifier;
    private final Duration pollDuration;
    private final Logger logger;
    private final String name;

    public BridgeTaskRunner(final List<MessageBridge> messageBridges, final ProcessNotifier processNotifier,
                            final Duration pollDuration, final Logger logger, final String name) {
        this.messageBridges = messageBridges;
        this.processNotifier = processNotifier;
        this.pollDuration = pollDuration;
        this.logger = logger;
        this.name = name;
    }

    public boolean isHealthy() {
        return !processNotifier.wasError();
    }

    public void process() {
        processNotifier.notifyStarted();
        int count = 0;
        boolean pause = false;
        try {
            //Process

            while (processNotifier.keepRunning()) {
                for (int index = 0; index < 100; index++) { //reduce calling atomic by 100x
                    for (MessageBridge messageBridge : messageBridges) {
                        if (pause) {
                            count += messageBridge.process(pollDuration);
                        } else {
                            count += messageBridge.process();
                        }
                    }
                    pause = count == 0;
                    count = 0;
                }
            }
            //Clean up
            cleanUp();
            processNotifier.notifyStopped();
        } catch (final Exception ex){
            logger.error(String.format("Bridge Task Runner %s Stopped by Exception %s", name, ex.getClass().getSimpleName()), ex);
            processNotifier.notifyStoppedByException(ex);
            cleanUp();
        }
        catch (final Throwable ex){
            logger.error(String.format("Bridge Task Runner %s Stopped by Error %s", name, ex.getClass().getSimpleName()), ex);
            processNotifier.notifyStoppedByError(ex);
            cleanUp();
        }
    }

    private void cleanUp() {
        messageBridges.forEach(messageBridge -> {
            try {
                messageBridge.close();
            } catch (Exception ex) {
                logger.error("Issue closing bridge", ex);
            }
        });
    }
}
