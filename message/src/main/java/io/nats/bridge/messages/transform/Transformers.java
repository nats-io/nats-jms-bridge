// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package io.nats.bridge.messages.transform;

import io.nats.bridge.messages.Message;

import java.util.*;

/**
 * Support class / utility to load and apply transformations to NATS bridge messages.
 */
public class Transformers {

    /** Load the transformations from the classpaths.
     *
     * @return mapping of names and transformers.
     */
    public static Map<String, TransformMessage> loadTransforms() {
        final ServiceLoader<TransformMessage> load = ServiceLoader.load(TransformMessage.class);
        Map<String, TransformMessage> map = new HashMap<>();
        for (TransformMessage transformMessage : load) {
            map.put(transformMessage.name(), transformMessage);
        }
        return Collections.unmodifiableMap(map);
    }


    /**
     *
     * @param transformers transformers to use
     * @param names names to apply from transformers mapping
     * @param inputMessage the input message to transform
     * @return the result of the transformation.
     */
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
