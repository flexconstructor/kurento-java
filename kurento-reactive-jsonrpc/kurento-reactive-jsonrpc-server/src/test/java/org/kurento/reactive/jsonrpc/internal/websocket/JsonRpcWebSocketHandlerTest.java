package org.kurento.reactive.jsonrpc.internal.websocket;

import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.kurento.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.internal.server.ProtocolManager;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.LinkedList;
import java.util.Queue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

/**
 * Defines tests for {@link JsonRpcWebSocketHandler}.
 */
public class JsonRpcWebSocketHandlerTest {

    /**
     * Mocked instance of {@link ProtocolManager}.
     */
    @Mock
    private ProtocolManager protocolManager;

    /**
     * Mocked instance of {@link Logger}
     */
    @Mock
    private Logger logger;

    /**
     * Instance of {@link JsonRpcWebSocketHandler} for tests.
     */
    @InjectMocks
    private JsonRpcWebSocketHandler handler;

    /**
     * Initializes all mocks before each tests.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Tests {@link JsonRpcWebSocketHandler#handle(WebSocketSession)}
     * Checks assertions:
     *   {@link ProtocolManager#processMessage(Mono, ProtocolManager.ServerSessionFactory, String)} was called.
     */
    @Test
    public void handleTest() {
        Response<JsonObject> serverResponse = Mockito.mock(Response.class);
        HandshakeInfo handshakeInfo = Mockito.mock(HandshakeInfo.class);
        Mono<JsonObject> jsonObjectMono = Mono.just(new JsonObject());
        Mono<Response> responseMono = Mono.just(serverResponse);
        Mockito.when(this.protocolManager.convertToJsonObject("test_websocket_message")).thenReturn(jsonObjectMono);
        Mockito.when(this.protocolManager.processMessage(eq(jsonObjectMono), any(), eq("test_web_socket_session_id")))
                .thenReturn(responseMono);
        WebSocketSession webSocketSession = Mockito.mock(WebSocketSession.class);
        WebSocketMessage webSocketMessage = Mockito.mock(WebSocketMessage.class);
        Mockito.when(webSocketMessage.getPayloadAsText()).thenReturn("test_websocket_message");
        Mockito.when(webSocketSession.textMessage(eq("test_websocket_message"))).thenReturn(webSocketMessage);
        Mockito.when(webSocketSession.send(any())).thenReturn(Mono.empty());
        Mockito.when(webSocketSession.getHandshakeInfo()).thenReturn(handshakeInfo);
        Mockito.when(webSocketSession.getId()).thenReturn("test_web_socket_session_id");
        Mockito.when(webSocketSession.receive()).thenReturn(Flux.empty());
        Queue<WebSocketMessage> webSocketMessageQueue = new LinkedList<>();
        Flux<WebSocketMessage> flux = Flux.fromIterable(webSocketMessageQueue);
        Mockito.when(webSocketSession.receive()).thenReturn(flux);
        webSocketMessageQueue.add(webSocketMessage);
        StepVerifier.create(flux).expectNext(webSocketMessage).verifyComplete();
        StepVerifier.create(this.handler.handle(webSocketSession)).verifyComplete();
        Mockito.verify(this.protocolManager).processMessage(eq(jsonObjectMono), any(), eq("test_web_socket_session_id"));
        Mockito.verify(webSocketSession).send(any());
    }
}

//mvn test -DfailIfNoTests=false -Dtest=org.kurento.reactive.jsonrpc.internal.websocket.JsonRpcWebSocketHandlerTest -am
