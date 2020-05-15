package io.nats.bridge.task;

import io.nats.bridge.MessageBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class BridgeTaskRunnerBuilder {

    private List<MessageBridge> messageBridges;
    private ProcessNotifier processNotifier;
    private Duration pollDuration;
    private Logger logger;
    private String name;
    private int worker;

    public int getWorker() {
        return worker;
    }

    public BridgeTaskRunnerBuilder withWorker(int worker) {
        this.worker = worker;
        return this;
    }

    public List<MessageBridge> getMessageBridges() {
        return messageBridges;
    }

    public BridgeTaskRunnerBuilder withMessageBridges(List<MessageBridge> messageBridges) {
        this.messageBridges = messageBridges;
        return this;
    }

    public ProcessNotifier getProcessNotifier() {
        return processNotifier;
    }

    public BridgeTaskRunnerBuilder withProcessNotifier(ProcessNotifier processNotifier) {
        this.processNotifier = processNotifier;
        return this;
    }

    public Duration getPollDuration() {
        return pollDuration;
    }

    public BridgeTaskRunnerBuilder withPollDuration(Duration pollDuration) {
        this.pollDuration = pollDuration;
        return this;
    }

    public Logger getLogger() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(BridgeTaskRunner.class.toString() + "." + name + "." + worker);
        }
        return logger;
    }

    public BridgeTaskRunnerBuilder withLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public String getName() {
        return name;
    }

    public BridgeTaskRunnerBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public BridgeTaskRunner build() {
        return new BridgeTaskRunner(getMessageBridges(), getProcessNotifier(), getPollDuration(), getLogger(), getName());
    }

    public static BridgeTaskRunnerBuilder  builder() {
        return new BridgeTaskRunnerBuilder();
    }
}
