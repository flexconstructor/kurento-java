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

import org.kurento.commons.PropertiesManager;
import org.kurento.commons.ThreadFactoryCreator;
import org.kurento.reactive.jsonrpc.internal.client.AbstractSession;
import org.kurento.reactive.jsonrpc.message.Response;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Abstract implementation of {@link AbstractSession}.
 *
 * @param <T> data type.
 */
public abstract class ServerSession<T> extends AbstractSession {
    /**
     * Session reconnection timeout. After the timeout exceeded the session should been deleted.
     */
    private static final String SESSION_RECONNECTION_TIME_PROP = "ws.sessionReconnectionTime";

    /**
     * Times of session reconnected.
     */
    private static final int SESSION_RECONNECTION_TIME_DEFAULT = 10;

    /**
     * Session manager.
     * TODO: make it deprecated.
     */
    private final SessionsManager sessionsManager;

    /**
     * Web socket session ID.
     */
    private String transportId;

    /**
     * Close session task.
     * TODO: impement close session via Flux or Mono.
     */
    private ScheduledFuture<?> closeTimerTask;

    /**
     * Session executor.
     */
    private ExecutorService sessionExecutor;

    /**
     * Session attributes.
     */
    private volatile ConcurrentMap<String, Object> attributes;

    /**
     * Reconnection timeout in mille seconds.
     */
    private long reconnectionTimeoutInMillis = PropertiesManager.getProperty(
            SESSION_RECONNECTION_TIME_PROP, SESSION_RECONNECTION_TIME_DEFAULT) * 1000;

    /**
     * It true if session is gracefully closed.
     */
    private boolean gracefullyClosed;

    /**
     * Constructs new instance of server session.
     *
     * @param sessionId       Given session ID.
     * @param registerInfo    Some request info.
     * @param sessionsManager {@link SessionsManager} instance.
     * @param transportId     web-socket session ID.
     */
    public ServerSession(String sessionId, Object registerInfo, SessionsManager sessionsManager,
                         String transportId) {
        super(sessionId, registerInfo);
        this.transportId = transportId;
        this.sessionsManager = sessionsManager;
        this.sessionExecutor = Executors.newSingleThreadExecutor(ThreadFactoryCreator
                .create("SessionHandler-" + sessionId));
    }

    /**
     * Handles {@link Response} from client.
     *
     * @param response {@link Response} client`s response.
     * @return requests queue as {@link Flux<T>}
     */
    public abstract Flux<T> handleResponse(Response<T> response);

    /**
     * Returns web-socket session ID.
     *
     * @return {@link String}.
     */
    String getTransportId() {
        return this.transportId;
    }

    /**
     * Sets new web-socket session ID for reconnection for example.
     *
     * @param transportId
     */
    void setTransportId(String transportId) {
        this.transportId = transportId;
    }

    /**
     * Closes the session.
     *
     * @throws IOException it can throws exception.
     */
    @Override
    public void close() throws IOException {
        this.sessionsManager.remove(this.getSessionId());
        this.sessionExecutor.shutdownNow();
    }

    /**
     * It returns true if the session was closed gracefully.
     */
    void setGracefullyClosed() {
        this.gracefullyClosed = true;
    }

    /**
     * Returns close timer task.
     *
     * @return {@link ScheduledFuture<?>}
     */
    ScheduledFuture<?> getCloseTimerTask() {
        return closeTimerTask;
    }

    /**
     * Sets new value of reconnection timeout.
     *
     * @param reconnectionTimeoutInMillis timeout in millis.
     */
    @Override
    public void setReconnectionTimeout(long reconnectionTimeoutInMillis) {
        this.reconnectionTimeoutInMillis = reconnectionTimeoutInMillis;
    }

    /**
     * Returns session attributes.
     *
     * @return {@link Map<String, Object>}
     */
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

    /**
     * Closes native web-socket session with given status and reason.
     *
     * @param status close status.
     * @param reason close reason.
     * @return {@link Mono<Void>}
     */
    public abstract Mono<Void> closeNativeSession(int status, String reason);

    /**
     * Updates web-socket session.
     *
     * @param wsSession {@link WebSocketSession}.
     */
    public abstract void updateWebSocketSession(WebSocketSession wsSession);

}
