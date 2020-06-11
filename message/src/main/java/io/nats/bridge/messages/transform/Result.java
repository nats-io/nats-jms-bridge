package io.nats.bridge.messages.transform;

public enum Result {
    /** Message was Transformed, send transformed message*/
    TRANSFORMED,
    /** Message transformation was not done, send the original message as is */
    NOT_TRANSFORMED,
    /** Message transformation was not done, but skip sending this message, like a filter. */
    SKIP,
    /** There was an error while transforming the message, and the message cannot be transformed or sent as is. */
    ERROR,
    /** There was an uncaught error while transforming the message, and the message cannot be transformed or sent as is. */
    SYSTEM_ERROR
}
