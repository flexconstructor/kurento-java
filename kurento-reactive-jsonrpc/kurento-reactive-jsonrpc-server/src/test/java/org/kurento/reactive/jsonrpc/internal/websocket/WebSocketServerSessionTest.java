package org.kurento.reactive.jsonrpc.internal.websocket;

import org.junit.Before;
import org.junit.Test;
import org.kurento.reactive.jsonrpc.internal.server.SessionsManager;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Defines unit tests for {@link WebSocketServerSession} methods.
 */
public class WebSocketServerSessionTest {

    /**
     * Mocked instance of {@link WebSocketSession}.
     */
    @Mock
    private WebSocketSession webSocketSession;

    /**
     * Mocked instance of {@link SessionsManager}.
     */
    @Mock
    private SessionsManager sessionsManager;

    /**
     * Mock of request info object.
     */
    @Mock
    private Object requestInfo;

    /**
     * Instance of {@link WebSocketServerSession} for tests.
     */
    private WebSocketServerSession webSocketServerSession;

    /**
     * Initializes all mocks before each test.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(webSocketSession.getId()).thenReturn("test_native_session_id");
        this.webSocketServerSession = new WebSocketServerSession("test_session_id", requestInfo,
                sessionsManager, webSocketSession) {
            @Override
            public Mono<Response> internalSendRequest(Mono mono, Class requestType) {
                return Mono.just(new Response(123, "test_response"));
            }
        };
        {
        }
    }

    /**
     * Tests {@link WebSocketServerSession#closeNativeSession(int, String)}. Expects close code is 4050, close reason is
     * `test_close_session_reason`.
     */
    @Test
    public void closeNativeSessionTest() {
        StepVerifier.create(this.webSocketServerSession.closeNativeSession(4050, "test_close_session_reason"))
                .assertNext(res -> {
                    ArgumentCaptor<CloseStatus> closeStatusArgumentCaptor = ArgumentCaptor.forClass(CloseStatus.class);
                    Mockito.verify(this.webSocketSession).close(closeStatusArgumentCaptor.capture());
                    assertEquals(4050, closeStatusArgumentCaptor.getValue().getCode());
                }).expectComplete();
    }

    /**
     * Tests {@link WebSocketServerSession#pendingResponse(Request)}. Expects it returns a {@link Mono<Response>} when
     * {@link WebSocketServerSession#handleResponse(Response)} was called.
     */
    @Test
    public void pendingResponseTest() {
        Request request = Mockito.mock(Request.class);
        Mockito.when(request.getId()).thenReturn(1235);
        Mockito.when(request.getSessionId()).thenReturn("test_session_id");

        Response response = Mockito.mock(Response.class);
        Mockito.when(response.getId()).thenReturn(1234);
        Mockito.when(response.getSessionId()).thenReturn("test_session_id");

        StepVerifier.create(this.webSocketServerSession.pendingResponse(request)).then(() -> {
            StepVerifier.create(this.webSocketServerSession.handleResponse(response)).expectNext(response).expectComplete();
        }).expectNext(response).expectComplete();

    }

    /**
     * Tests {@link WebSocketServerSession#sendRequest(String, Class)}. Expects: It returns {@link Mono<Response>}.
     */
    @Test
    public void sendRequestTest() {
        StepVerifier.create(this.webSocketServerSession.sendRequest("test_method", Request.class))
                .assertNext(response -> {
                    assertTrue(response instanceof Response);
                }).expectComplete();
    }

    /**
     * Tests {@link WebSocketServerSession#sendRequest(Mono, Class)}. Expects: It returns {@link Mono<Response>}.
     */
    @Test
    public void sendRequestReactiveTest() {
        Mono<Request> requestMono = Mono.just(new Request());
        StepVerifier.create(this.webSocketServerSession.sendRequest(requestMono, Request.class))
                .assertNext(response -> {
                    assertTrue(response instanceof Response);
                }).expectComplete();
    }

    /**
     * Tests {@link WebSocketServerSession#sendNotification(String)}. Expects: It returns {@link Mono<Response>}.
     */
    @Test
    public void sendNotificationTest() {
        StepVerifier.create(this.webSocketServerSession.sendNotification("test_method"))
                .assertNext(response -> {
                    assertTrue(response instanceof Response);
                }).expectComplete();
    }

}

// /mvn test -DfailIfNoTests=false -Dtest=org.kurento.reactive.jsonrpc.internal.websocket.WebSocketServerSessionTest -am
