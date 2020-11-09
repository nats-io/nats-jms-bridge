package io.nats.bridge.support;

import io.nats.bridge.MessageBus;
import io.nats.bridge.jms.JMSMessageBus;
import org.slf4j.Logger;

import java.time.Duration;

public class MessageBusRestarter {


    private final String name;
    private final Logger logger;
    private final Duration ignoreRestartBackoffAfter = Duration.ofMinutes(10);
    private final int backoffMax = 60;
    private long lastRestart = System.currentTimeMillis();
    private int backoffSeconds = 1;

    public MessageBusRestarter(String name, Logger logger) {
        this.name = name;
        this.logger = logger;
    }


    public void restartMessageBus(final Exception ex, final MessageBus messageBus) {


        if (messageBus instanceof JMSMessageBus) {
            logger.info("Restarting Message Bus {} {}", name, backoffSeconds);

            final long now = System.currentTimeMillis();

            if (lastRestart > (now - ignoreRestartBackoffAfter.toMillis()) && backoffSeconds < backoffMax) {
                backoffSeconds = backoffSeconds * 2;
            }

            logger.info("Restart reason for " + name, ex);

            try {
                messageBus.close();
                logger.info("Restart reason {} === CLOSED", name);
            } catch (Exception exClose) {
                logger.debug("Unable to close", exClose);
            }

            try {
                messageBus.init();
                logger.info("Restart reason {} === RESTARTED", name);
            } catch (Exception exClose) {
                logger.error("Unable to recreate", exClose);

                try {
                    Thread.sleep(backoffSeconds * 1000);
                } catch (InterruptedException e) {
                }
                return;
            }
            backoffSeconds = 1;
            lastRestart = System.currentTimeMillis();

            logger.info("Restarting Message Bus for {}, sleeping {}", name, backoffSeconds);
            logger.info("Restarted Message Bus for {}", name);

        } else {
            rethrow(ex);
        }
    }

    private void rethrow(Exception ex) {
        if (ex instanceof RuntimeException) {
            throw ((RuntimeException) ex);
        } else {
            throw new RuntimeException(ex);
        }
    }
}
