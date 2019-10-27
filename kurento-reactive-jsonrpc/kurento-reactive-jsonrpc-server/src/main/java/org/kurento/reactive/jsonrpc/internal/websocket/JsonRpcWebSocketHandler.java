package org.kurento.reactive.jsonrpc.internal.websocket;

import com.google.gson.JsonObject;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.reactive.jsonrpc.internal.server.ProtocolManager;
import org.kurento.reactive.jsonrpc.internal.server.ServerSession;
import org.kurento.reactive.jsonrpc.internal.server.SessionsManager;
import org.kurento.reactive.jsonrpc.message.Message;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Defines implementation of {@link WebSocketHandler} for processing web-socket messages.
 */
public class JsonRpcWebSocketHandler implements WebSocketHandler {

    /**
     * Instance of {@link ProtocolManager}.
     */
    private final ProtocolManager protocolManager;

    /**
     * Defines max websocket messages exception.
     */
    private static class MaxNumberWsConnectionsReachedException extends Exception {

        private static final long serialVersionUID = -6621614523181088993L;
    }

    /**
     * Max value of web-socket messages count.
     */
    private static final long MAX_WS_CONNECTIONS =
            PropertiesManager.getProperty("ws.maxSessions", Long.MAX_VALUE);
    /**
     * Count of established web-socket connections.
     */
    private static final AtomicLong numConnections = new AtomicLong();

    /**
     * Instance of logger.
     */
    private static final Logger log = LoggerFactory.getLogger(JsonRpcWebSocketHandler.class);

    /**
     * Label for logging.
     */
    private String label = "";

    /**
     * Constructs new instance of JsonRpcWebSocketHandler.
     *
     * @param protocolManager {@link ProtocolManager} instance.
     */
    public JsonRpcWebSocketHandler(ProtocolManager protocolManager) {
        this.protocolManager = protocolManager;
    }

    /**
     * Handle web-socket messages. Just calls {@link ProtocolManager#processMessage(Mono, ProtocolManager.ServerSessionFactory, String)}
     *
     * @param webSocketSession instance of {@link WebSocketSession}
     * @return {@link Mono<Void>}
     */
    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {
        try {
            this.afterConnectionEstablished(webSocketSession);
        } catch (Exception ex) {
            webSocketSession.close(CloseStatus.POLICY_VIOLATION);
        }

        Queue<Message> messageQueue = new LinkedList<>();
        Flux<Message> outputMessages = Flux.fromIterable(messageQueue);

        ProtocolManager.ServerSessionFactory factory = new ProtocolManager.ServerSessionFactory() {
            @Override
            public ServerSession<JsonObject> createSession(String sessionId, Object registerInfo,
                                                           SessionsManager sessionsManager) {

                return new WebSocketServerSession<JsonObject>(sessionId, registerInfo, sessionsManager, webSocketSession.getId()) {
                    @Override
                    public Mono<Response> internalSendRequest(Mono mono, Class requestType) {
                        return this.processRequest(mono, requestType);
                    }

                    private Mono<Response<JsonObject>> processRequest(Mono<Request<JsonObject>> requestMono, Class requestType){
                        requestMono.doOnNext(messageQueue::add);
                        if(requestType != Void.class) {
                            return requestMono.flatMap(this::pendingResponse);
                        }
                        return Mono.empty();
                    }
                };
            }

            /**
             * Updates {@link ServerSession} after reconnection.
             *
             * @param session {@link ServerSession}
             */
            @Override
            public void updateSessionOnReconnection(ServerSession<JsonObject> session) {

            }

        };


        Mono<Void> input = webSocketSession.receive()
                .map(message -> {
                    try {
                        return this.protocolManager.convertToJsonObject(message.getPayloadAsText());
                    } catch (Exception ex) {
                        String exceptionMessage = String.format("Unable to convert message %s to json, because: %s", message, ex.getMessage());
                        log.warn(exceptionMessage);
                        throw new KurentoException(exceptionMessage);
                    }
                }).map(jsonObjectMono -> this.protocolManager.processMessage(jsonObjectMono, factory, webSocketSession.getId()))
                .doOnNext(monoResponse -> {
                    monoResponse.map((Function<Response, Object>) messageQueue::add);
                }).then();


        return webSocketSession.send(outputMessages.flatMap(message -> Mono.just(message.toString())).map(webSocketSession::textMessage)).and(input);
    }

    private void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            incNumConnectionsIfAllowed();
        } catch (MaxNumberWsConnectionsReachedException e) {
            log.warn("Closed a WS connection because MAX_WS_CONNECTIONS={} limit reached",
                    MAX_WS_CONNECTIONS);
            session.close();
        }
        try {
            // We send this notification to the JsonRpcHandler when the JsonRpc
            // session is established, not when websocket session is established
            log.debug(
                    "{} Client connection established from session={} uri={} headers={} acceptedProtocol={} attributes={}",
                    label, session.getHandshakeInfo().getRemoteAddress(), session.getHandshakeInfo().getUri(), session.getHandshakeInfo().getHeaders(), session.getHandshakeInfo().getSubProtocol(),
                    session.getAttributes());
        } catch (Throwable t) {
            log.error("{} Exception processing afterConnectionEstablished in session={}", label,
                    session.getId(), t);
        }
    }


    private void incNumConnectionsIfAllowed() throws MaxNumberWsConnectionsReachedException {

        while (true) {

            long curNumConn = numConnections.get();
            if (curNumConn >= MAX_WS_CONNECTIONS) {
                throw new MaxNumberWsConnectionsReachedException();
            }

            // Try updating the value, but only if it's equal to the
            // one we've just seen. If it is different, we have to check again if now
            // there are room for a new client.
            boolean setSuccessful = numConnections.compareAndSet(curNumConn, curNumConn + 1);

            if (setSuccessful) {
                // We have incremented numConnections. Exiting.
                break;
            }

            // Another thread updated the numConnections between our get and
            // compareAndSet calls. It is possible that we check again
        }
    }
}
