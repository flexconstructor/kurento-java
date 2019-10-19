package org.kurento.reactive.jsonrpc.internal.websocket;

import com.google.gson.JsonElement;
import org.kurento.reactive.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.internal.server.ServerSession;
import org.kurento.reactive.jsonrpc.internal.server.SessionsManager;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * {@link ServerSession} implementation.
 */
public class WebSocketServerSession extends ServerSession {

    /**
     * {@link WebSocketSession} instance.
     */
    private WebSocketSession wsSession;

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
    }

    /**
     * Handle response.
     *
     * @param response {@link Response}.
     */
    @Override
    public void handleResponse(Response<JsonElement> response) {

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
    void updateWebSocketSession(WebSocketSession wsSession) {
        synchronized (wsSession) {
            this.wsSession = wsSession;
        }
    }
}
