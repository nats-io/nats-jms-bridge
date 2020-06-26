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
