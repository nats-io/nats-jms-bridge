package io.nats.bridge.messages.transform;

import io.nats.bridge.messages.Message;

import java.util.Objects;
import java.util.Optional;

public class TransformResult {

    private final Optional<Throwable> error;
    private final Optional<String> statusMessage;
    private final Result result;
    private final Optional<Message> message;

    public TransformResult(Throwable error, String transformationMessage, Result result, Message message) {
        this.error = Optional.ofNullable(error);
        this.statusMessage = Optional.ofNullable(transformationMessage);
        this.message = Optional.ofNullable(message);
        this.result = Objects.requireNonNullElse(result, Result.TRANSFORMED);
    }

    public TransformResult() {
        this(null, null, null, null);
    }

    public static TransformResult success(final Message transformedMessage) {
        return new TransformResult(null, null, Result.TRANSFORMED, transformedMessage);
    }

    public static TransformResult success(final String transformationMessage, final Message transformedMessage) {
        return new TransformResult(null, transformationMessage, Result.TRANSFORMED, transformedMessage);
    }

    public static TransformResult skip() {
        return new TransformResult(null, null, Result.SKIP, null);
    }


    public static TransformResult skip(final String transformationMessage) {
        return new TransformResult(null, transformationMessage, Result.SKIP, null);
    }

    public static TransformResult notTransformed() {
        return new TransformResult(null, null, Result.NOT_TRANSFORMED, null);
    }

    public static TransformResult notTransformed(final String transformationMessage) {
        return new TransformResult(null, transformationMessage, Result.NOT_TRANSFORMED, null);
    }

    public static TransformResult error(final Throwable exception) {
        return new TransformResult(exception, null, Result.ERROR, null);
    }

    public static TransformResult error(final String transformationMessage, final Throwable exception) {
        return new TransformResult(exception, transformationMessage, Result.ERROR, null);
    }

    public Result getResult() {
        return result;
    }

    public boolean isError() {
        return result == Result.ERROR || result == Result.SYSTEM_ERROR;
    }

    public boolean wasTransformed() {
        return result == Result.TRANSFORMED;
    }

    public Throwable getError() {
        return error.orElse(null);
    }

    public Message getTransformedMessage() {
        return message.orElse(null);
    }

    public Optional<String> getStatusMessage() {
        return statusMessage;
    }
}
