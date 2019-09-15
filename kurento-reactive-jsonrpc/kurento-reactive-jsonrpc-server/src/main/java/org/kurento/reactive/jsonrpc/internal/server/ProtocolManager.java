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
import com.google.gson.reflect.TypeToken;
import org.kurento.commons.SecretGenerator;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.JsonRpcHandlerManager;
import org.kurento.jsonrpc.internal.client.AbstractSession;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.kurento.jsonrpc.internal.JsonRpcConstants.*;

public class ProtocolManager {

    public static final String CLIENT_CLOSED_CLOSE_REASON = "Client sent close message";

    private static final String INTERVAL_PROPERTY = "interval";

    public interface ServerSessionFactory {
        ServerSession createSession(String sessionId, Object registerInfo,
                                    SessionsManager sessionsManager);

        void updateSessionOnReconnection(ServerSession session);
    }

    private static final Logger log = LoggerFactory.getLogger(ProtocolManager.class);

    private static final SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss,S");

    protected SecretGenerator secretGenerator = new SecretGenerator();

    @Autowired
    private SessionsManager sessionsManager;

    @Autowired
    @Qualifier("jsonrpcTaskScheduler")
    private TaskScheduler taskScheduler;

    private final JsonRpcHandlerManager handlerManager;

    private String label = "";

    private int maxHeartbeats = 0;

    private int heartbeats = 0;

    private PingWatchdogManager pingWachdogManager;

    public ProtocolManager(JsonRpcHandler<?> handler) {
        this.handlerManager = new JsonRpcHandlerManager(handler);
    }

    public ProtocolManager(JsonRpcHandler<?> handler, SessionsManager sessionsManager,
                           TaskScheduler taskScheduler) {
        this.handlerManager = new JsonRpcHandlerManager(handler);
        this.sessionsManager = sessionsManager;
        this.taskScheduler = taskScheduler;
        postConstruct();
    }

    @PostConstruct
    private void postConstruct() {

        PingWatchdogManager.NativeSessionCloser nativeSessionCloser = new PingWatchdogManager.NativeSessionCloser() {
            @Override
            public void closeSession(String transportId) {
                ServerSession serverSession = sessionsManager.getByTransportId(transportId);
                if (serverSession != null) {
                    serverSession.closeNativeSession("Close for not receive ping from client");
                } else {
                    log.warn("Ping wachdog trying to close a non-registered ServerSession");
                }
            }
        };

        this.pingWachdogManager = new PingWatchdogManager(taskScheduler, nativeSessionCloser);
    }

    public void setLabel(String label) {
        this.label = "[" + label + "] ";
    }

    public Mono<JsonObject> convertToJsonObject(String message) throws Exception {
        JsonObject messagetJsonObject = JsonUtils.fromJson(message, JsonObject.class);
        if (messagetJsonObject == null) {
            throw new ClassCastException(String.format("unable to convert %s to jsonObject", message));
        }
        return Mono.just(messagetJsonObject);
    }

    public Mono<Request<JsonElement>> convertToRequest(Mono<JsonObject> source) {
        return source.map(jsonpObject -> JsonUtils.fromJsonRequest(jsonpObject, JsonElement.class));
    }


    /**
     * Process incoming message. The response is sent using responseSender. If null, the session will
     * be used.
     *
     * @param messagetJsonObject
     * @param factory
     * @param responseSender
     * @param internalSessionId
     * @throws IOException
     */
    public Mono<Response> processMessage(Mono<JsonObject> messagetJsonObject, ServerSessionFactory factory,
                                     ResponseSender responseSender, String internalSessionId) {

        return messagetJsonObject.flatMap(jsonObject -> {
            if (jsonObject.has(Request.METHOD_FIELD_NAME)) {
                return processRequestMessage(factory, messagetJsonObject, responseSender, internalSessionId);
            } else {
                return processResponseMessage(messagetJsonObject, internalSessionId);
            }
        });
    }

    // TODO Unify ServerSessionFactory, ResponseSender and transportId in a
    // entity "RequestContext" or similar. In this way, there are less
    // parameters
    // and the implementation is easier
    private Mono<Response<Object>> processRequestMessage(ServerSessionFactory factory, Mono<JsonObject> requestJsonObject,
                                             final ResponseSender responseSender, String transportId) {
       requestJsonObject.map(jsonObject -> this.convertToRequest(requestJsonObject)).map(requestMono -> requestMono.map(request ->{
        switch (request.getMethod()){
          case METHOD_CONNECT:
            return processReconnectMessage(factory, requestMono, transportId);
          case METHOD_PING:
            return processPingMessage(requestMono, transportId);
          case METHOD_CLOSE:
            return processCloseMessage(Mono.just(request), transportId);
          default:
            final ServerSession session = getOrCreateSession(factory, transportId, request);
            log.debug("{} Req-> {} [jsonRpcSessionId={}, transportId={}]", label, request,
                    session.getSessionId(), transportId);
            if (request.getMethod().equals(Request.POLL_METHOD_NAME)) {
              Type collectionType = new TypeToken<List<Response<JsonElement>>>() {
              }.getType();

              List<Response<JsonElement>> responseList = JsonUtils.fromJson(request.getParams(),
                      collectionType);
              responseList.forEach(session::handleResponse);
            }else{

              session.processRequest(() -> handlerManager.handleRequest(session, request, responseSender));

            }
            return Mono.just(new Response<Object>(request.getId(), Collections.emptyList()));
        }

      }));
       return Mono.empty();

    }


    private ServerSession getOrCreateSession(ServerSessionFactory factory, String transportId,
                                             Request<JsonElement> request) {

        ServerSession session = null;

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

            session = createSession(factory, null);

            handlerManager.afterConnectionEstablished(session);
        } else {
            session.setNew(false);
        }

        return session;
    }

    private ServerSession createSessionAsOldIfKnowByHandler(ServerSessionFactory factory,
                                                            String reqSessionId) {

        ServerSession session = null;

        JsonRpcHandler<?> handler = handlerManager.getHandler();
        if (handler instanceof NativeSessionHandler) {
            NativeSessionHandler nativeHandler = (NativeSessionHandler) handler;
            if (nativeHandler.isSessionKnown(reqSessionId)) {

                log.debug("Session {} is already known by NativeSessionHandler", reqSessionId);

                session = createSession(factory, null, reqSessionId);
                session.setNew(false);
                nativeHandler.processNewCreatedKnownSession(session);
            }
        }
        return session;
    }

    private Mono<Response> processPingMessage( Mono<Request<JsonElement>> monoRequest, String transportId) {
        return monoRequest.map(request -> {
            if (maxHeartbeats == 0 || maxHeartbeats > ++heartbeats) {
              long interval = -1;
              if (request.getParams() != null) {
                JsonObject element = (JsonObject) request.getParams();
                if (element.has(INTERVAL_PROPERTY)) {
                  interval = element.get(INTERVAL_PROPERTY).getAsLong();
                  pingWachdogManager.pingReceived(transportId, interval);
                  String sessionId = request.getSessionId();
                  JsonObject pongPayload = new JsonObject();
                  pongPayload.add(PONG_PAYLOAD, new JsonPrimitive(PONG));
                  return new Response<Object>(sessionId, request.getId(), pongPayload);
                }
              }
            }
            return new Response();
        });
    }

    private Mono<Response> processCloseMessage( Mono<Request<JsonElement>> monoRequest, String transportId) {
        return monoRequest.map(request -> {
            ServerSession session = sessionsManager.getByTransportId(transportId);
            if (session != null) {
                session.setGracefullyClosed();
                cancelCloseTimer(session);
            }
            if (session != null) {
                this.closeSession(session, CLIENT_CLOSED_CLOSE_REASON);
            }
            return new Response<Object>(request.getId(), "bye");
        });
    }

    private Mono<Response> processReconnectMessage(ServerSessionFactory factory, Mono<Request<JsonElement>> monoRequest,
                                                   String transportId) {

        return monoRequest.map(request -> {
            String sessionId = request.getSessionId();
            if (sessionId == null) {

                ServerSession session = getOrCreateSession(factory, transportId, request);
                return new Response<Object>(session.getSessionId(), request.getId(), "OK");
            } else {

                ServerSession session = sessionsManager.get(sessionId);
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

    private ServerSession createSession(ServerSessionFactory factory, Object registerInfo,
                                        String sessionId) {

        ServerSession session = factory.createSession(sessionId, registerInfo, sessionsManager);

        pingWachdogManager.associateSessionId(session.getTransportId(), sessionId);

        sessionsManager.put(session);

        return session;
    }

    private ServerSession createSession(ServerSessionFactory factory, Object registerInfo) {

        String sessionId = secretGenerator.nextSecret();

        return createSession(factory, registerInfo, sessionId);
    }

    private Mono<Response> processResponseMessage(Mono<JsonObject> monoJsonObject, String internalSessionId) {

        return monoJsonObject.flatMap(jsonObject -> {
            Response<JsonElement> response = JsonUtils.fromJsonResponse(jsonObject,
                    JsonElement.class);
            ServerSession session = sessionsManager.getByTransportId(internalSessionId);
            if (session != null) {
                session.handleResponse(response);
            } else {
                log.debug("Processing response {} for non-existent session {}", response.toString(),
                        internalSessionId);
            }
            return Mono.empty();
        });
    }

    public void closeSessionIfTimeout(final String transportId, final String reason) {

        final ServerSession session = sessionsManager.getByTransportId(transportId);

        if (session != null) {

            try {

                Date closeTime = new Date(
                        System.currentTimeMillis() + session.getReconnectionTimeoutInMillis());

                log.debug(label + "Configuring close timeout for session: {} transportId: {} at {}",
                        session.getSessionId(), transportId, format.format(closeTime));

                ScheduledFuture<?> lastStartedTimerFuture = taskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        closeSession(session, reason);
                    }
                }, closeTime);

                session.setCloseTimerTask(lastStartedTimerFuture);

                pingWachdogManager.disablePingWatchdogForSession(transportId);

            } catch (TaskRejectedException e) {
                log.warn(label + "Close timeout for session {} with transportId {} can not be set "
                        + "because the scheduler is shutdown", session.getSessionId(), transportId);
            }
        }
    }

    public void closeSession(ServerSession session, String reason) {
        log.debug("{} Removing session {} with transportId {} in ProtocolManager", label,
                session.getSessionId(), session.getTransportId());
        try {
            session.close();
        } catch (IOException e) {
            log.warn("{} Could not close WsSession session {}", label, session.getSessionId(), e);
        }
        sessionsManager.remove(session);
        pingWachdogManager.removeSession(session);
        handlerManager.afterConnectionClosed(session, reason);
    }

    public void cancelCloseTimer(ServerSession session) {
        if (session.getCloseTimerTask() != null) {
            session.getCloseTimerTask().cancel(false);
        }
    }

    public void processTransportError(String transportId, Throwable exception) {
        final ServerSession session = sessionsManager.getByTransportId(transportId);
        handlerManager.handleTransportError(session, exception);
    }

    /**
     * Method intended to be used for testing purposes
     *
     * @param maxHeartbeats
     */
    public void setMaxNumberOfHeartbeats(int maxHeartbeats) {
        this.maxHeartbeats = maxHeartbeats;
    }

    public void setPingWachdog(boolean pingWachdog) {
        this.pingWachdogManager.setPingWatchdog(pingWachdog);
    }

    public AbstractSession getSessionByTransportId(String transportId) {
        return sessionsManager.getByTransportId(transportId);
    }
}
