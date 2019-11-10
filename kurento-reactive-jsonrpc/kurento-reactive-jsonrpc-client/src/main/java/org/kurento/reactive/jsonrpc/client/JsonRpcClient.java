package org.kurento.reactive.jsonrpc.client;

import org.kurento.reactive.jsonrpc.internal.JsonRpcRequestSender;
import org.kurento.reactive.jsonrpc.message.Response;
import reactor.core.publisher.Mono;

import java.io.Closeable;

public abstract class JsonRpcClient implements JsonRpcRequestSender, Closeable {

    /**
     * Sends JSON RPC request with method and result class parameters.
     *
     * @param method      String JSON RPC method name.
     * @param resultClass Result class.
     * @return Mono.
     */
    @Override
    public Mono sendRequest(String method, Class resultClass) {
        return null;
    }

    /**
     * Sends JSON RPC request with some parameters.
     *
     * @param method      String JSON RPC method name.
     * @param params      {@link Mono} parameters.
     * @param resultClass expected result class.
     * @return {@link Mono<Response>}.
     */
    @Override
    public Mono sendRequest(String method, Mono params, Class resultClass) {
        return null;
    }

    /**
     * Sends notification (any response is unexpected).
     *
     * @param method String JSON RPC method name.
     * @param params some parameters as Mono.
     * @return {@link Mono<Void>}
     */
    @Override
    public Mono<Void> sendNotification(String method, Mono params) {
        return null;
    }

    /**
     * Sends notification without any parameters.
     *
     * @param method String JSON RPC method name.
     * @return {@link Mono<Void>}
     */
    @Override
    public Mono<Void> sendNotification(String method) {
        return null;
    }

    /**
     * Sends {@link Mono<Request>} and expects response with given type.
     *
     * @param request     {@link Mono<Request>}
     * @param requestType class type of response.
     * @return {@link Mono< Response >}.
     */
    @Override
    public Mono<Response> sendRequest(Mono request, Class requestType) {
        return null;
    }
}
