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
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.RequiredArgsConstructor;
import org.kurento.commons.SecretGenerator;
import org.kurento.reactive.jsonrpc.JsonUtils;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.message.ResponseError;
import org.kurento.reactive.jsonrpc.JsonRpcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.kurento.reactive.jsonrpc.internal.JsonRpcConstants.*;

/**
 * Defines json rpc protocol manager.
 */
@RequiredArgsConstructor
public class ProtocolManager {

    /**
     * Reason for close web-socket session by client.
     */
    private static final String CLIENT_CLOSED_CLOSE_REASON = "Client sent close message";

    /**
     * Ping interval property name.
     */
    private static final String INTERVAL_PROPERTY = "interval";

    /**
     * Server session factory interface.
     */
    public interface ServerSessionFactory {

        /**
         * Creates new instance of {@link ServerSession}.
         *
         * @param sessionId       session id
         * @param registerInfo    request info object
         * @param sessionsManager {@link SessionsManager} instance.
         * @return new instance of server session.
         */
        ServerSession<JsonObject> createSession(String sessionId, Object registerInfo,
                                       SessionsManager sessionsManager);

        /**
         * Updates {@link ServerSession} after reconnection.
         *
         * @param session {@link ServerSession}
         */
        void updateSessionOnReconnection(ServerSession<JsonObject> session);
    }

    /**
     * Logger instance.
     */
    private static final Logger log = LoggerFactory.getLogger(ProtocolManager.class);

    /**
     * Secret generator for string ids.
     */
    private SecretGenerator secretGenerator = new SecretGenerator();

    /**
     * Implementation of {@link JsonRpcHandler}.
     */
    private final JsonRpcHandler<JsonObject> handler;

    /**
     * {@link SessionsManager} instance.
     */
    private final SessionsManager sessionsManager;

    /**
     * {@link PingWatchdogManager} instance.
     */
    private final PingWatchdogManager pingWachdogManager;

    /**
     * Label for logger.
     */
    private String label = "";

    /**
     * Converts string message to {@link JsonObject}.
     *
     * @param message string message.
     * @return returns {@link JsonObject}.
     */
    public Mono<JsonObject> convertToJsonObject(String message) {
        JsonObject messagetJsonObject = JsonUtils.fromJson(message, JsonObject.class);
        if (messagetJsonObject == null) {
            throw new ClassCastException(String.format("unable to convert %s to jsonObject", message));
        }
        return Mono.just(messagetJsonObject);
    }

    /**
     * Converts {@link JsonObject} to {@link Request}.
     *
     * @param source {@link JsonObject} instance.
     *
     * @return {@link Request}
     */
    Mono<Request<JsonElement>> convertToRequest(Mono<JsonObject> source) {
        return source.map(jsonpObject -> JsonUtils.fromJsonRequest(jsonpObject, JsonElement.class));
    }


    /**
     * Process incoming message. The response is sent using responseSender. If null, the session will
     * be used.
     */
    public Mono<Response> processMessage(Mono<JsonObject> messagetJsonObject, ServerSessionFactory factory,
                                         String internalSessionId) {

        return messagetJsonObject.flatMap(jsonObject -> {
            if (jsonObject.has(Request.METHOD_FIELD_NAME)) {

                return processRequestMessage(factory, this.convertToRequest(messagetJsonObject), internalSessionId);
            } else {
                return processResponseMessage(messagetJsonObject, internalSessionId);
            }
        });
    }

    // TODO Unify ServerSessionFactory, ResponseSender and transportId in a
    // entity "RequestContext" or similar. In this way, there are less
    // parameters
    // and the implementation is easier
    private Mono<Response<?>> processRequestMessage(ServerSessionFactory factory, Mono<Request<JsonElement>> requestJsonObject,
                                                    String transportId) {

        return requestJsonObject.flatMap(request -> {
            switch (request.getMethod()) {
                case METHOD_CONNECT:
                    return processReconnectMessage(factory, Mono.fromSupplier(() -> request), transportId);
                case METHOD_PING:
                    return processPingMessage(Mono.fromSupplier(() -> request), transportId);
                case METHOD_CLOSE:
                    return processCloseMessage(Mono.fromSupplier(() -> request), transportId);
                default:
                    return this.handler.handleRequest(Mono.fromSupplier(() -> request), getOrCreateSession(factory,
                            transportId, request));
            }
        });
    }


    private ServerSession<JsonObject> getOrCreateSession(ServerSessionFactory factory, String transportId,
                                                Request<JsonElement> request) {

        ServerSession<JsonObject> session = null;

        String reqSessionId = request.getSessionId();

        if (reqSessionId != null) {

            session = sessionsManager.get(reqSessionId);

            if (session == null) {

                session = createSessionAsOldIfKnowByHandler(factory, reqSessionId);

                if (session == null) {
                    log.warn(label + "There is no session with specified id '{}'." + "Creating a new one.",
                            reqSessionId);
                }
            }

        } else if (transportId != null) {
            session = sessionsManager.getByTransportId(transportId);
        }

        if (session == null) {

            session = createSession(factory);

        } else {
            session.setNew(false);
        }

        return session;
    }

    private ServerSession<JsonObject> createSessionAsOldIfKnowByHandler(ServerSessionFactory factory,
                                                               String reqSessionId) {

        ServerSession<JsonObject> session = null;

        if (this.handler instanceof NativeSessionHandler) {
            NativeSessionHandler nativeHandler = (NativeSessionHandler) handler;
            if (nativeHandler.isSessionKnown(reqSessionId)) {

                log.debug("Session {} is already known by NativeSessionHandler", reqSessionId);

                session = createSession(factory, reqSessionId);
                session.setNew(false);
                nativeHandler.processNewCreatedKnownSession(session);
            }
        }
        return session;
    }

    private Mono<Response<JsonElement>> processPingMessage(Mono<Request<JsonElement>> monoRequest, String transportId) {

        return monoRequest.map(request -> {
            long interval;
            if (request.getParams() != null) {
                JsonObject element = (JsonObject) request.getParams();
                if (element.has(INTERVAL_PROPERTY)) {
                    interval = element.get(INTERVAL_PROPERTY).getAsLong();
                    pingWachdogManager.pingReceived(transportId, interval);
                    String sessionId = request.getSessionId();
                    JsonObject pongPayload = new JsonObject();
                    pongPayload.add(PONG_PAYLOAD, new JsonPrimitive(PONG));
                    return new Response<>(sessionId, request.getId(), pongPayload);
                }
            }
            return new Response<>();
        });
    }

    private Mono<Response<?>> processCloseMessage(Mono<Request<JsonElement>> monoRequest, String transportId) {
        return monoRequest.map(request -> {
            ServerSession<JsonObject> session = sessionsManager.getByTransportId(transportId);
            if (session != null) {
                session.setGracefullyClosed();
                cancelCloseTimer(session);
            }
            if (session != null) {
                this.closeSession(session);
            }
            return new Response<Object>(request.getId(), "bye");
        });
    }

    private Mono<Response<?>> processReconnectMessage(ServerSessionFactory factory, Mono<Request<JsonElement>> monoRequest,
                                                      String transportId) {

        return monoRequest.map(request -> {
            String sessionId = request.getSessionId();
            if (sessionId == null) {

                ServerSession<JsonObject> session = getOrCreateSession(factory, transportId, request);
                return new Response<Object>(session.getSessionId(), request.getId(), "OK");
            } else {

                ServerSession<JsonObject> session = sessionsManager.get(sessionId);
                if (session != null) {

                    String oldTransportId = session.getTransportId();
                    session.setTransportId(transportId);
                    factory.updateSessionOnReconnection(session);
                    pingWachdogManager.updateTransportId(transportId, oldTransportId);
                    sessionsManager.updateTransportId(session, oldTransportId);

                    // FIXME: Possible race condition if session is disposed when
                    // reconnect method has arrived
                    cancelCloseTimer(session);
                    return new Response<Object>(sessionId, request.getId(), RECONNECTION_SUCCESSFUL);

                } else {

                    session = createSessionAsOldIfKnowByHandler(factory, sessionId);

                    if (session != null) {
                        return new Response<Object>(sessionId, request.getId(), RECONNECTION_SUCCESSFUL);
                    } else {
                        return new Response<>(request.getId(), new ResponseError(40007, RECONNECTION_ERROR));
                    }
                }
            }
        });
    }

    private ServerSession<JsonObject> createSession(ServerSessionFactory factory,
                                           String sessionId) {

        ServerSession<JsonObject> session = factory.createSession(sessionId, null, sessionsManager);

        pingWachdogManager.associateSessionId(session.getTransportId(), sessionId);

        sessionsManager.put(session);

        return session;
    }

    private ServerSession<JsonObject> createSession(ServerSessionFactory factory) {

        String sessionId = secretGenerator.nextSecret();

        return createSession(factory, sessionId);
    }

    private Mono<Response> processResponseMessage(Mono<JsonObject> monoJsonObject, String internalSessionId) {

        return monoJsonObject.flatMap(jsonObject -> {
            Response<JsonElement> response = JsonUtils.fromJsonResponse(jsonObject,
                    JsonElement.class);
            ServerSession<JsonObject> session = sessionsManager.getByTransportId(internalSessionId);
            if (session != null) {
                // TODO: handle response.
               // session.handleResponse(response);
            } else {
                log.debug("Processing response {} for non-existent session {}", response.toString(),
                        internalSessionId);
            }
            return Mono.empty();
        });
    }

    private void closeSession(ServerSession<JsonObject> session) {
        log.debug("{} Removing session {} with transportId {} in ProtocolManager", label,
                session.getSessionId(), session.getTransportId());
        try {
            session.close();
        } catch (IOException e) {
            log.warn("{} Could not close WsSession session {}", label, session.getSessionId(), e);
        }
        sessionsManager.remove(session);
        pingWachdogManager.removeSession(session);
        this.handler.afterConnectionClosed(session, ProtocolManager.CLIENT_CLOSED_CLOSE_REASON);
    }

    private void cancelCloseTimer(ServerSession<JsonObject> session) {
        if (session.getCloseTimerTask() != null) {
            session.getCloseTimerTask().cancel(false);
        }
    }
}
