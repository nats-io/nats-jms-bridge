package io.nats.bridge.messages.transform;

import io.nats.bridge.messages.Message;

import java.util.*;

public class Transformers {

    public static Map<String, TransformMessage> loadTransforms() {
        final ServiceLoader<TransformMessage> load = ServiceLoader.load(TransformMessage.class);
        Map<String, TransformMessage> map = new HashMap<>();
        for (TransformMessage transformMessage : load) {
            map.put(transformMessage.name(), transformMessage);
        }
        return Collections.unmodifiableMap(map);
    }


    public static TransformResult runTransforms(final Map<String, TransformMessage> transformers, final List<String> names,
                                                final Message inputMessage) {

        Message message = inputMessage;
        TransformResult result = null;

        for (String name : names) {
            TransformMessage transformMessage = transformers.get(name);
            if (transformMessage == null) {
                return TransformResult.error("Transformer named " + name + " was not found", new IllegalStateException("Not found " + name));
            }

            try {
                result = transformMessage.transform(message);
                if (result == null) {
                    return TransformResult.error("Transformers returned null result", new IllegalStateException("Result was null"));
                }
            } catch (Exception ex) {
                return TransformResult.error("Transformation " + name + " threw an exception " + ex.getMessage(), ex);
            }

            switch (result.getResult()) {
                case ERROR:
                case SYSTEM_ERROR:
                case SKIP:
                    return result;
                case TRANSFORMED:
                    message = result.getTransformedMessage();
                case NOT_TRANSFORMED:
                    //No op

            }
        }

        return result != null ? result : TransformResult.error("Transformers not found", new IllegalStateException("No transformers found"));

    }

}
