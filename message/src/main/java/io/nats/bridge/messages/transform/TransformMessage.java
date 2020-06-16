package io.nats.bridge.messages.transform;

import io.nats.bridge.messages.Message;

/**
 * The TransformMessage uses the https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html
 * ServiceLoader pattern.
 *
 * Create a jar with a META-INF/services/io.nats.bridge.transform.TransformMessage text file with the name
 * of your service. The NATS JMS/IBM MQ bridge will load all services found which can be added to the projectHome/libs
 * directory of the NATS JMS/IBM MQ Bridge.
 *
 * The TransformMessages are loaded and sorted by the ordinal field.
 * If you don't specify an ordinal value, it will be added at the end in no known order after the transforms that
 * have ordinal values.
 *
 * The admin tool will also allow configuration via YAML using the name field.
 */
public interface TransformMessage {

    /**
     * Transform the message.
     *
     * @param inputMessage message to be transformed.
     * @return the new transformed message.
     */
    TransformResult transform(Message inputMessage);

    /**
     * Order of importance.
     * The lower the number, the soonest the transform is applied.
     *
     * @return ordinal value which determines when the transform will be applied.
     */
    default int ordinal() {
        return Integer.MAX_VALUE;
    }

    /**
     * @return name of the transformation
     */
    default String name() {
        return this.getClass().getSimpleName();
    }
}
