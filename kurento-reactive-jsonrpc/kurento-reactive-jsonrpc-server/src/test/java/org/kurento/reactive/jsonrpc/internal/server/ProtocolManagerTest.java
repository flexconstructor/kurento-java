package org.kurento.reactive.jsonrpc.internal.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.JsonRpcHandler;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

public class ProtocolManagerTest {

    private class JsonRPCHandlerMock implements JsonRpcHandler {

        @Override
        public Mono<Response<JsonElement>> handleRequest(Mono mono, Session session) {
            return null;
        }

        @Override
        public void afterConnectionClosed(ServerSession session, String reason) {

        }

        @Override
        public void handleTransportError(ServerSession session, Throwable exception) {

        }
    }

    @Mock
    private JsonRPCHandlerMock jsonRPCHandlerMock;

    @Mock
    private SessionsManager sessionsManager;

    @Mock
    private ThreadPoolTaskScheduler executor;

    @InjectMocks
    private ProtocolManager protocolManager;


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
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(Request.METHOD_FIELD_NAME, "test_method");
        jsonObject.addProperty("id", 1);
        jsonObject.addProperty("argument", "testArgument");
        Mono<Request<JsonElement>> requestMono = this.protocolManager.convertToRequest(Mono.just(jsonObject));
        StepVerifier.create(requestMono).assertNext(request -> {
            assertNotNull(request);
            assertEquals(1, request.getId().intValue());
        }).verifyComplete();
    }

    @Test
    public void handleRequestTest() {

        ProtocolManager.ServerSessionFactory sessionFactory = Mockito.mock(ProtocolManager.ServerSessionFactory.class);
        ServerSession session = Mockito.mock(ServerSession.class);
        Mockito.when(session.getTransportId()).thenReturn("test_transport_id");
        Response<JsonElement> handlerResponse = JsonUtils.fromJsonResponse("{'jsonrpc': '2.0', 'id':'1', 'result': 'test_result'}", JsonElement.class);
        Mockito.when(this.jsonRPCHandlerMock.handleRequest(any(), any())).thenReturn(Mono.just(handlerResponse));
        Mockito.when(this.sessionsManager.getByTransportId(eq("test_transport_id"))).thenReturn(session);

        StepVerifier.create(this.protocolManager.processMessage(this.protocolManager
                        .convertToJsonObject("{'method': 'test_method_name', 'id':'1', 'argument': 'test_argument'}"),
                sessionFactory, "test_transport_id")).assertNext(response -> {
            Mockito.verify(this.jsonRPCHandlerMock).handleRequest(any(), eq(session));
            assertNotNull(response);
            assertEquals(Integer.valueOf(1), response.getId());
        }).verifyComplete();
    }

}

//mvn test -DfailIfNoTests=false -Dtest=org.kurento.reactive.jsonrpc.internal.server.ProtocolManagerTest -am