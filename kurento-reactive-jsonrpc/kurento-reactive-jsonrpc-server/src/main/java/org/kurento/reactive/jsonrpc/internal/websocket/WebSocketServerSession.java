package org.kurento.reactive.jsonrpc.internal.websocket;

import com.google.gson.JsonElement;
import org.kurento.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.internal.server.ServerSession;
import org.kurento.reactive.jsonrpc.internal.server.SessionsManager;
import org.springframework.web.reactive.socket.WebSocketSession;

public class WebSocketServerSession extends ServerSession {
    private WebSocketSession wsSession;

    public WebSocketServerSession(String sessionId, Object registerInfo, SessionsManager sessionsManager, String transportId) {
        super(sessionId, registerInfo, sessionsManager, transportId);
    }

    @Override
    public void handleResponse(Response<JsonElement> response) {

    }

    @Override
    public void closeNativeSession(String reason) {

    }

    public void updateWebSocketSession(WebSocketSession wsSession) {
        synchronized (wsSession) {
            this.wsSession = wsSession;
        }
    }
}
