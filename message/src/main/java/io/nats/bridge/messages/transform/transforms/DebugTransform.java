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

package io.nats.bridge.messages.transform.transforms;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;

public class DebugTransform implements TransformMessage {
    @Override
    public TransformResult transform(Message inputMessage) {

        System.out.println("DEBUG TRANSFORMED CALLED");
        final TransformResult result = TransformResult.success(inputMessage);
        System.out.println("DEBUG TRANSFORMED DONE");
        return result;

    }

    @Override
    public String name() {
        return "debug";
    }
}
