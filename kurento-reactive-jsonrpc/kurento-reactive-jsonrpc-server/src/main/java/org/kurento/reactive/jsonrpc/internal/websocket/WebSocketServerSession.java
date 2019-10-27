package org.kurento.reactive.jsonrpc.internal.websocket;

import org.kurento.reactive.jsonrpc.internal.server.ServerSession;
import org.kurento.reactive.jsonrpc.internal.server.SessionsManager;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ServerSession} implementation.
 */
public abstract class WebSocketServerSession<T> extends ServerSession<T> {

    /**
     * {@link WebSocketSession} instance.
     */
    private WebSocketSession wsSession;

   Flux<Response<T>> responseFlux;

    /**
     * Constructs new instance of WebSocketServerSession.
     *
     * @param sessionId       session ID.
     * @param registerInfo    request info.
     * @param sessionsManager {@link SessionsManager} instance.
     * @param transportId     web-socket session ID.
     */
    WebSocketServerSession(String sessionId, Object registerInfo, SessionsManager sessionsManager, String transportId) {
        super(sessionId, registerInfo, sessionsManager, transportId);
        this.responseFlux = Flux.empty();
    }

    /**
     * Handle response.
     *
     * @param response {@link Response}.
     */
    @Override
    public void handleResponse(Response<T> response) {
        this.responseFlux.concatWithValues(response);

    }

    /**
     * Closes native session.
     *
     * @param status status code.
     * @param reason close reason.
     * @return {@link Mono<Void>}
     */
    @Override
    public Mono<Void> closeNativeSession(int status, String reason) {
        return this.wsSession.close(new CloseStatus(status, reason));
    }

    /**
     * Updates web-socket session after reconnection.
     *
     * @param wsSession {@link WebSocketSession} instance.
     */
    public void updateWebSocketSession(WebSocketSession wsSession) {
        synchronized (wsSession) {
            this.wsSession = wsSession;
        }
    }

    Mono<Response<T>> pendingResponse(Request<T> request) {
     return this.responseFlux.skipWhile(response -> response.getId().equals(request.getId())).next();
    }
}
