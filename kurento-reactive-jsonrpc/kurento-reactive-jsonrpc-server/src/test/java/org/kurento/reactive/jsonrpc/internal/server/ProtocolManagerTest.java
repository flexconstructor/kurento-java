package org.kurento.reactive.jsonrpc.internal.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.kurento.reactive.jsonrpc.JsonUtils;
import org.kurento.reactive.jsonrpc.Session;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.JsonRpcHandler;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.ScheduledFuture;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;

/**
 * Defines unit tests for {@link ProtocolManager}.
 */
public class ProtocolManagerTest {

    /**
     * Mocked implementation of {@link JsonRpcHandler}
     */
    @Mock
    private JsonRpcHandler jsonRPCHandlerMock;

    /**
     * Mocked instance of {@link SessionsManager}.
     */
    @Mock
    private SessionsManager sessionsManager;

    /**
     * Mocked instance of {@link PingWatchdogManager}.
     */
    @Mock
    private PingWatchdogManager pingWatchdogManager;

    /**
     * Instance of {@link ProtocolManager} for tests.
     */
    @InjectMocks
    private ProtocolManager protocolManager;

    /**
     * Session factory.
     */
    @Mock
    private ProtocolManager.ServerSessionFactory sessionFactory;

    /**
     * Mocked instance of {@link ServerSession}.
     */
    @Mock
    private ServerSession session;

    /**
     * Initializes all mocks before each tests.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Tests {@link ProtocolManager#convertToJsonObject(String)}. Expects: It returns a {@link JsonObject}.
     */
    @Test
    public void convertToJsonObjectTest() {
        String json = "{'id':'1', 'success':'true', 'method': 'test_method'}";
        StepVerifier.create(this.protocolManager.convertToJsonObject(json)).assertNext(result -> {
            assertEquals(1, result.get("id").getAsInt());
            assertTrue(result.get("success").getAsBoolean());
            assertTrue(result.has(Request.METHOD_FIELD_NAME));
        }).verifyComplete();
    }

    /**
     * Tests convert {@link JsonObject} to {@link Request} without sessionId. Expects: It returns a {@link Request}.
     */
    @Test
    public void convertToRequestTest() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(Request.METHOD_FIELD_NAME, "test_method");
        jsonObject.addProperty("id", 1);
        jsonObject.addProperty("argument", "testArgument");
        Request<JsonElement> elementRequest = JsonUtils.fromJsonRequest(jsonObject, JsonElement.class);
        assertNotNull(elementRequest);
    }

    /**
     * Tests convert {@link JsonObject} to {@link Request} with sessionId. Expects: It returns a {@link Request} which
     * contains sessionId.
     */
    @Test
    public void convertToRequestWithSessionTest() {
        Mono<Request<JsonElement>> requestMono = this.protocolManager.convertToRequest(this.protocolManager.convertToJsonObject("{'jsonrpc': '2.0', 'method': 'test_method', 'id':'1', params: {'sessionId': 'test_session_id'}}"));
        StepVerifier.create(requestMono).assertNext(request -> {
            assertNotNull(request);
            assertEquals(1, request.getId().intValue());
            assertEquals("test_session_id", request.getSessionId());
            assertEquals("test_method", request.getMethod());
        }).verifyComplete();
    }

    /**
     * Tests {@link ProtocolManager#processMessage(Mono, ProtocolManager.ServerSessionFactory, String)}. Expects:
     * {@link JsonRpcHandler#handleRequest(Mono, Session)} was called.
     */
    @Test
    public void handleRequestTest() {
        Mockito.when(this.session.getTransportId()).thenReturn("test_transport_id");
        Response<JsonElement> handlerResponse = JsonUtils.fromJsonResponse("{'jsonrpc': '2.0', 'id':'1', 'result': 'test_result'}", JsonElement.class);
        Mockito.when(this.jsonRPCHandlerMock.handleRequest(any(), any())).thenReturn(Mono.just(handlerResponse));
        Mockito.when(this.sessionsManager.getByTransportId(eq("test_transport_id"))).thenReturn(this.session);

        StepVerifier.create(this.protocolManager.processMessage(this.protocolManager
                        .convertToJsonObject("{'method': 'test_method_name', 'id':'1', 'argument': 'test_argument'}"),
                sessionFactory, "test_transport_id")).assertNext(response -> {
            Mockito.verify(this.jsonRPCHandlerMock).handleRequest(any(), eq(this.session));
            assertNotNull(response);
            assertEquals(Integer.valueOf(1), response.getId());
        }).verifyComplete();
    }

    /**
     * Tests handling of reconnect request by transportId. Expects: {@link SessionsManager#getByTransportId(String)} was
     * called.
     */
    @Test
    public void handleReconnectByTransportIdTest() {
        Mockito.when(this.sessionsManager.getByTransportId(eq("test_transport_id"))).thenReturn(this.session);
        StepVerifier.create(this.protocolManager.processMessage(this.protocolManager
                        .convertToJsonObject("{'method': 'connect', 'id':'2', 'argument': 'test_argument'}"),
                sessionFactory, "test_transport_id")).assertNext(response -> {
            assertNotNull(response);
            assertEquals(2, response.getId().intValue());
            assertNotNull(response.getResult());
            assertFalse(response.isError());
            Mockito.verify(this.sessionsManager).getByTransportId(eq("test_transport_id"));
            Mockito.verify(this.session).setNew(eq(false));
        }).verifyComplete();
    }

    /**
     * Tests handling of reconnect request by sessionId.
     * Checks assertions:
     * 1. {@link JsonRpcHandler#handleRequest(Mono, Session)} was not been called.
     * 2. Sets new transport id into existed session.
     * 3. The session was updated after reconnection.
     * 4. {@link PingWatchdogManager#updateTransportId(String, String)} was called.
     * 5. {@link SessionsManager#updateTransportId(ServerSession, String)} was called.
     * 6. Close task was chanseled.
     */
    @Test
    public void handleReconnectBySessionIdTest() {
        ScheduledFuture closeTask = Mockito.mock(ScheduledFuture.class);
        Mockito.when(this.session.getCloseTimerTask()).thenReturn(closeTask);
        Mockito.when(this.sessionsManager.get(eq("test_session_id"))).thenReturn(this.session);
        StepVerifier.create(this.protocolManager.processMessage(this.protocolManager
                        .convertToJsonObject("{ 'jsonrpc': '2.0', 'method': 'connect', 'id':'3', 'params': {'sessionId': 'test_session_id'}}"),
                sessionFactory, "test_transport_id")).assertNext(response -> {
            assertNotNull(response);
            assertFalse(response.isError());
            Mockito.verify(this.jsonRPCHandlerMock, Mockito.never()).handleRequest(any(), any());
            Mockito.verify(this.session).setTransportId(eq("test_transport_id"));
            Mockito.verify(this.sessionFactory).updateSessionOnReconnection(eq(this.session));
            Mockito.verify(this.pingWatchdogManager).updateTransportId(eq("test_transport_id"), anyString());
            Mockito.verify(this.sessionsManager).updateTransportId(eq(this.session), any());
            Mockito.verify(closeTask).cancel(eq(false));
        }).verifyComplete();
    }

    /**
     * Tests handling of ping request. Expects: {@link PingWatchdogManager#pingReceived(String, long)} wa called.
     */
    @Test
    public void handlePingTest() {
        StepVerifier.create(this.protocolManager.processMessage(this.protocolManager
                        .convertToJsonObject("{ 'jsonrpc': '2.0', 'method': 'ping', 'id':'4', 'params': {'sessionId': 'test_session_id', 'interval': 1500}}"),
                sessionFactory, "test_transport_id")).assertNext(response -> {
            assertNotNull(response);
            assertTrue(response.getResult() instanceof JsonObject);
            JsonObject responseJson = (JsonObject) response.getResult();
            assertTrue(responseJson.has("value"));
            assertEquals("pong", responseJson.get("value").getAsString());
            Mockito.verify(this.pingWatchdogManager).pingReceived(eq("test_transport_id"), eq(1500L));
        }).verifyComplete();
    }

    /**
     * Tests handling of close session request.
     * Checks assertion:
     * 1. {@link ServerSession#setGracefullyClosed()} was called.
     * 2. {@link ServerSession#close()} was called.
     * 3. "Bye" was sent.
     * 4. Close task canceled.
     * 5. {@link SessionsManager#remove(String)} was called.
     * 6. {@link PingWatchdogManager#removeSession(ServerSession)} was called.
     * 7. {@link JsonRpcHandler#afterConnectionClosed(ServerSession, String)} was called.
     */
    @Test
    public void handleCloseSessionTest() {
        ScheduledFuture closeTask = Mockito.mock(ScheduledFuture.class);
        Mockito.when(this.session.getCloseTimerTask()).thenReturn(closeTask);
        Mockito.when(this.sessionsManager.getByTransportId(eq("test_transport_id"))).thenReturn(this.session);
        StepVerifier.create(this.protocolManager.processMessage(this.protocolManager
                        .convertToJsonObject("{ 'jsonrpc': '2.0', 'method': 'closeSession', 'id':'5', 'params': {'sessionId': 'test_session_id'}}"),
                sessionFactory, "test_transport_id")).assertNext(response -> {
            assertNotNull(response);
            assertEquals(5, response.getId().intValue());
            assertEquals("bye", response.getResult());
            Mockito.verify(this.session).setGracefullyClosed();
            Mockito.verify(closeTask).cancel(eq(false));
            try {
                Mockito.verify(this.session).close();
            } catch (Exception ex) {
                assertNull(ex);
            }

            Mockito.verify(this.sessionsManager).remove(eq(this.session));
            Mockito.verify(this.pingWatchdogManager).removeSession(eq(this.session));
            Mockito.verify(this.jsonRPCHandlerMock).afterConnectionClosed(eq(this.session),
                    eq("Client sent close message"));

        }).verifyComplete();
    }

    /**
     * Tests handling of clients response. Expects {@link ServerSession#handleResponse(Response)} was called.
     */
    @Test
    public void processResponseTest() {

        Mockito.when(this.sessionsManager.getByTransportId(eq("test_transport_id"))).thenReturn(this.session);
        StepVerifier.create(this.protocolManager.processMessage(this.protocolManager
                        .convertToJsonObject("{ 'jsonrpc': '2.0', 'success': 'Ok'}"),
                sessionFactory, "test_transport_id")).assertNext(response -> Mockito.verify(this.session)
                .handleResponse(eq(response)));
    }
}


//mvn test -DfailIfNoTests=false -Dtest=org.kurento.reactive.jsonrpc.internal.server.ProtocolManagerTest -am