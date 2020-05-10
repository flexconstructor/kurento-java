/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.reactive.jsonrpc;

import org.kurento.reactive.jsonrpc.internal.JsonRpcRequestSender;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

/**
 * JSON RPC session interface.
 *
 * @param <R> session data type.
 */
public interface Session<R> extends JsonRpcRequestSender<R> {

    /**
     * Returns session ID.
     *
     * @return {@link String} session ID.
     */
    String getSessionId();

    /**
     * Returns Request info.
     *
     * @return {@link Object}.
     */
    Object getRegisterInfo();

    /**
     * Returns true if is new session.
     *
     * @return boolen.
     */
    boolean isNew();

    /**
     * Closes the session.
     *
     * @throws IOException it can trows exception.
     */
    Mono<Void> close();

    void setReconnectionTimeout(long millis);

    /**
     * Returns session attributes.
     *
     * @return {@link Map<String, Object>}.
     */
    Map<String, Object> getAttributes();

}
