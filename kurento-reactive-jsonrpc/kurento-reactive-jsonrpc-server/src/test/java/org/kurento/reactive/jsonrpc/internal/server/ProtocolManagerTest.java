package org.kurento.reactive.jsonrpc.internal.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
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

public class ProtocolManagerTest {


    @Mock
    private JsonRpcHandler jsonRPCHandlerMock;

    @Mock
    private SessionsManager sessionsManager;

    @Mock
    private PingWatchdogManager pingWatchdogManager;

    @InjectMocks
    private ProtocolManager protocolManager;

    @Mock
    private ProtocolManager.ServerSessionFactory sessionFactory;


    @Mock
    private ServerSession session;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void convertToJsonObjectTest() {
        String json = "{'id':'1', 'success':'true', 'method': 'test_method'}";
        StepVerifier.create(this.protocolManager.convertToJsonObject(json)).assertNext(result -> {
            assertEquals(1, result.get("id").getAsInt());
            assertTrue(result.get("success").getAsBoolean());
            assertTrue(result.has(Request.METHOD_FIELD_NAME));
        }).verifyComplete();
    }

    @Test
    public void convertToRequestTes() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(Request.METHOD_FIELD_NAME, "test_method");
        jsonObject.addProperty("id", 1);
        jsonObject.addProperty("argument", "testArgument");

        Request<JsonElement> elementRequest = JsonUtils.fromJsonRequest(jsonObject, JsonElement.class);
        assertNotNull(elementRequest);


    }

    @Test
    public void convertToRequestTest() {
        Mono<Request<JsonElement>> requestMono = this.protocolManager.convertToRequest(this.protocolManager.convertToJsonObject("{'jsonrpc': '2.0', 'method': 'test_method', 'id':'1', params: {'sessionId': 'test_session_id'}}"));
        StepVerifier.create(requestMono).assertNext(request -> {
            assertNotNull(request);
            assertEquals(1, request.getId().intValue());
            assertEquals("test_session_id", request.getSessionId());
            assertEquals("test_method", request.getMethod());
        }).verifyComplete();
    }

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
}


//mvn test -DfailIfNoTests=false -Dtest=org.kurento.reactive.jsonrpc.internal.server.ProtocolManagerTest -am