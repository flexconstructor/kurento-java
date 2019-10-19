package org.kurento.reactive.jsonrpc;

import com.google.gson.JsonElement;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.internal.server.ServerSession;
import reactor.core.publisher.Mono;

/**
 * Defines interface for custom {@link Request} handler.
 *
 * @param <P> type of entity.
 */
public interface JsonRpcHandler<P> {

    /**
     * Handles clients {@link Request}.
     *
     * @param requestMono {@link Mono<Request>}
     * @param session     {@link Session} implementation.
     * @return {@link Mono<Response>}.
     */
    Mono<Response<JsonElement>> handleRequest(Mono<Request<JsonElement>> requestMono, Session session);

    /**
     * It should been called after web-socket connection closed.
     *
     * @param session {@link ServerSession} server session instance.
     * @param reason  reason for closing web-socket session.
     */
    void afterConnectionClosed(ServerSession session, String reason);

    /**
     * Process web-socket transport errors.
     *
     * @param session    {@link ServerSession} instance.
     * @param exception  {@link Throwable} exception instance.
     */
    void handleTransportError(ServerSession session, Throwable exception);
}
