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

package org.kurento.reactive.jsonrpc.internal;

import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import reactor.core.publisher.Mono;

/**
 * Interface for JSON RPC request sender.
 *
 * @param <R> type of Request data object.
 */
public interface JsonRpcRequestSender<R> {

    /**
     * Sends JSON RPC request with method and result class parameters.
     *
     * @param method        String JSON RPC method name.
     * @param resultClass   Result class.
     *
     * @return Mono.
     */
    Mono<R> sendRequest(String method, Class<R> resultClass);

    /**
     * Sends JSON RPC request with some parameters.
     *
     * @param method      String JSON RPC method name.
     * @param params      {@link Mono} parameters.
     * @param resultClass expected result class.
     *
     * @return {@link Mono<Response>}.
     */
    Mono<R> sendRequest(String method, Mono<R> params, Class<R> resultClass);

    /**
     * Sends {@link Mono<Request>} and expects response with given type.
     *
     * @param request      {@link Mono<Request>}
     * @param requestType  class type of response.
     *
     * @return {@link Mono<Response>}.
     */
    Mono<Response<R>> sendRequest(Mono<Request<R>> request, Class<R> requestType);

    /**
     * Sends notification (any response is unexpected).
     *
     * @param method  String JSON RPC method name.
     * @param params  some parameters as Mono.
     *
     * @return {@link Mono<Void>}
     */
    Mono<Void> sendNotification(String method, Mono params);

    /**
     * Sends notification without any parameters.
     *
     * @param method String JSON RPC method name.
     *
     * @return {@link Mono<Void>}
     */
    Mono<Void> sendNotification(String method);
}
