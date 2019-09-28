package org.kurento.reactive.jsonrpc;

import com.google.gson.JsonElement;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.internal.server.ServerSession;
import reactor.core.publisher.Mono;

public interface JsonRpcHandler<P> {
    Mono<Response<JsonElement>> handleRequest(Mono<Request<JsonElement>> requestMono, Session session);
    void afterConnectionClosed(ServerSession session, String reason);
   void  handleTransportError(ServerSession session, Throwable exception);
}
