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

package org.kurento.reactive.jsonrpc.internal.server;

import com.google.gson.JsonElement;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.ThreadFactoryCreator;
import org.kurento.reactive.jsonrpc.internal.client.AbstractSession;
import org.kurento.reactive.jsonrpc.message.Response;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public abstract  class ServerSession<T> extends AbstractSession {

    private static final String SESSION_RECONNECTION_TIME_PROP = "ws.sessionReconnectionTime";
    private static final int SESSION_RECONNECTION_TIME_DEFAULT = 10;

    private final SessionsManager sessionsManager;
    private String transportId;
    private ScheduledFuture<?> closeTimerTask;
    private ExecutorService sessionExecutor;

    private volatile ConcurrentMap<String, Object> attributes;

    private long reconnectionTimeoutInMillis = PropertiesManager.getProperty(
            SESSION_RECONNECTION_TIME_PROP, SESSION_RECONNECTION_TIME_DEFAULT) * 1000;
    private boolean gracefullyClosed;

    public ServerSession(String sessionId, Object registerInfo, SessionsManager sessionsManager,
                         String transportId) {

        super(sessionId, registerInfo);

        this.transportId = transportId;
        this.sessionsManager = sessionsManager;

        this.sessionExecutor = Executors.newSingleThreadExecutor(ThreadFactoryCreator
                .create("SessionHandler-" + sessionId));
    }

    public abstract void handleResponse(Response<T> response);

    String getTransportId() {
        return transportId;
    }

    void setTransportId(String transportId) {
        this.transportId = transportId;
    }

    @Override
    public void close() throws IOException {
        this.sessionsManager.remove(this.getSessionId());
        this.sessionExecutor.shutdownNow();
    }


    void setGracefullyClosed() {
        this.gracefullyClosed = true;
    }



    ScheduledFuture<?> getCloseTimerTask() {
        return closeTimerTask;
    }

    @Override
    public void setReconnectionTimeout(long reconnectionTimeoutInMillis) {
        this.reconnectionTimeoutInMillis = reconnectionTimeoutInMillis;
    }



    @Override
    public Map<String, Object> getAttributes() {
        if (attributes == null) {
            synchronized (this) {
                if (attributes == null) {
                    attributes = new ConcurrentHashMap<>();
                }
            }
        }

        return attributes;
    }

    public abstract Mono<Void> closeNativeSession(int status, String reason);

    public abstract void updateWebSocketSession(WebSocketSession wsSession);

}
