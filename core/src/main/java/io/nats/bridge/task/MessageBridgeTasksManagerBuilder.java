package io.nats.bridge.task;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBridgeTasksManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Function;

public class MessageBridgeTasksManagerBuilder {

    private String name;
    private Logger logger;
    private Function<String, MessageBridge> bridgeFactory;
    private int workers;
    private int tasks;
    private Duration pollDuration;
    private boolean namePerTask;

    public String getName() {
        return name;
    }

    public MessageBridgeTasksManagerBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public Logger getLogger() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(MessageBridgeTasksManager.class.toString() + "." + getName());
        }
        return logger;
    }

    public MessageBridgeTasksManagerBuilder withLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public Function<String, MessageBridge> getBridgeFactory() {
        return bridgeFactory;
    }

    public MessageBridgeTasksManagerBuilder withBridgeFactory(Function<String, MessageBridge> bridgeFactory) {
        this.bridgeFactory = bridgeFactory;
        return this;
    }

    public int getWorkers() {
        return workers;
    }

    public MessageBridgeTasksManagerBuilder withWorkers(int workers) {
        this.workers = workers;
        return this;
    }

    public int getTasks() {
        return tasks;
    }

    public MessageBridgeTasksManagerBuilder withTasks(int tasks) {
        this.tasks = tasks;
        return this;
    }

    public Duration getPollDuration() {
        return pollDuration;
    }

    public MessageBridgeTasksManagerBuilder withPollDuration(Duration pollDuration) {
        this.pollDuration = pollDuration;
        return this;
    }

    public boolean isNamePerTask() {
        return namePerTask;
    }

    public MessageBridgeTasksManagerBuilder withNamePerTask(boolean namePerTask) {
        this.namePerTask = namePerTask;
        return this;
    }

    public static MessageBridgeTasksManagerBuilder builder() {
        return new MessageBridgeTasksManagerBuilder();
    }

    public MessageBridgeTasksManager build() {
        return new MessageBridgeTasksManagerImpl(getName(), getLogger(),
                getBridgeFactory(), getWorkers(), getTasks(), getPollDuration(), isNamePerTask());
    }
}
