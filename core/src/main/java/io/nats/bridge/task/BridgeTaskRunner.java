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
                    if (count == 0) pause = true;
                    count = 0;
                }
            }
            //Clean up
            messageBridges.forEach(messageBridge -> {
                try {
                    messageBridge.close();
                } catch (Exception ex) {
                    logger.error("Issue closing bridge", ex);
                }
            });
            processNotifier.notifyStopped();
        } catch (final Exception ex){
            logger.error(String.format("Bridge Task Runner %s Stopped by Exception %s", name, ex.getClass().getSimpleName()), ex);
            processNotifier.notifyStoppedByError(ex);
        }
    }
}
