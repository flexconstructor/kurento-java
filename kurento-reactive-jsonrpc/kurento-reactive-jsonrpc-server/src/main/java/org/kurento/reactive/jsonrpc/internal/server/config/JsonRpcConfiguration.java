package org.kurento.reactive.jsonrpc.internal.server.config;

import org.kurento.reactive.jsonrpc.JsonRpcHandler;
import org.kurento.reactive.jsonrpc.internal.server.PingWatchdogManager;
import org.kurento.reactive.jsonrpc.internal.server.ProtocolManager;
import org.kurento.reactive.jsonrpc.internal.server.ServerSession;
import org.kurento.reactive.jsonrpc.internal.server.SessionsManager;
import org.kurento.reactive.jsonrpc.internal.websocket.JsonRpcWebSocketHandler;
import org.kurento.reactive.jsonrpc.server.JsonRpcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public abstract class JsonRpcConfiguration implements JsonRpcConfigurer {

    @Autowired
    protected ApplicationContext ctx;

    private final List<JsonRpcConfigurer> configurers = Collections.singletonList(this);
    private DefaultJsonRpcHandlerRegistry instanceRegistry;

    private DefaultJsonRpcHandlerRegistry getJsonRpcHandlersRegistry() {
        if (instanceRegistry == null) {
            instanceRegistry = new DefaultJsonRpcHandlerRegistry();
            this.configurers.forEach(jsonRpcConfigurer -> jsonRpcConfigurer.registerJsonRpcHandlers(instanceRegistry));
        }
        return instanceRegistry;
    }

    @Bean
    HandlerMapping jsonRpcHandlerMapping() {
        Map<String, WebSocketHandler> urlMap = new LinkedHashMap<>();
        getJsonRpcHandlersRegistry().getRegistrations().forEach(registration -> {
            registration.getHandlerMap().forEach((handler, paths) ->
                putHandlersMappings(urlMap, handler, paths));
            registration.getPerSessionHandlerClassMap().forEach((handler, paths) -> putHandlersMappings(urlMap,
                    (JsonRpcHandler<?>) ctx.getBean("perSessionJsonRpcHandler", handler, null),
                    paths));
            registration.getPerSessionHandlerClassMap().forEach((handler, paths) ->
                putHandlersMappings(urlMap, (JsonRpcHandler<?>) ctx.getBean("perSessionJsonRpcHandler", null, handler), paths));
        });

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(urlMap);
        handlerMapping.setOrder(1);
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();

    }

    @Bean
    @Scope("prototype")
    public ProtocolManager protocolManager(JsonRpcHandler<?> handler) {
        return new ProtocolManager(handler, ctx.getBean(SessionsManager.class),this.pingWatchdogManager());
    }

    @Bean
    @Scope("prototype")
    public PingWatchdogManager pingWatchdogManager(){
        PingWatchdogManager.NativeSessionCloser nativeSessionCloser = transportId -> {
            ServerSession serverSession = ctx.getBean(SessionsManager.class).getByTransportId(transportId);
            if (serverSession != null) {
                serverSession.closeNativeSession("Close for not receive ping from client");
            }
        };
        return new PingWatchdogManager(ctx.getBean(TaskScheduler.class), nativeSessionCloser);
    }


    private void putHandlersMappings(Map<String, WebSocketHandler> urlMap, JsonRpcHandler<?> handler,
                                     List<String> paths) {
        JsonRpcWebSocketHandler rpcWebSocketHandler = new JsonRpcWebSocketHandler(protocolManager(handler));
        paths.forEach(path -> urlMap.put(path, rpcWebSocketHandler));
    }

}
