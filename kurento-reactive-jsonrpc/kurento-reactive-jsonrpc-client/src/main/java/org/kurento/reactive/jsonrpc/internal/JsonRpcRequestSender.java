
package org.kurento.reactive.jsonrpc.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import reactor.core.publisher.Mono;

public interface JsonRpcRequestSender<R> {

    Mono<R> sendRequest(String method, Class<R> resultClass);

    Mono<R> sendRequest(String method, Mono<R> params, Class<R> resultClass);

    Mono<Response<R>> sendRequest(Mono<Request<R>> request, Class<R> requestType);

    Mono<Void> sendNotification(String method, Mono params);

    Mono<Void> sendNotification(String method);

}
