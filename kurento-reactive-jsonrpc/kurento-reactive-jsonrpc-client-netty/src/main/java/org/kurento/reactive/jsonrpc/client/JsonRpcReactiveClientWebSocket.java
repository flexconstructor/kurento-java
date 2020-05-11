package org.kurento.reactive.jsonrpc.client;

import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;
import org.kurento.jsonrpc.message.Message;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

public class JsonRpcReactiveClientWebSocket extends AbstractReactiveJsonRpcClientWebSocket {

    private WebSocketClient client;

    private final UnicastProcessor<Message> unicastProcessor;

    public JsonRpcReactiveClientWebSocket(String url, JsonRpcWSConnectionListener connectionListener,
                                          UnicastProcessor<Message> unicastProcessor) {
        super(url, connectionListener);
        this.unicastProcessor = unicastProcessor;
    }

    @Override
    protected void sendTextMessage(Message jsonMessage) {
        this.unicastProcessor.add(jsonMessage);
    }

    @Override
    protected void closeNativeClient() {
        this.client = null;
    }

    @Override
    protected boolean isNativeClientConnected() {
        return this.client != null;
    }

    @Override
    protected Mono<Void> connectNativeClient() {
        this.client = new ReactorNettyWebSocketClient();
        return this.client.execute(this.uri, webSocketSession -> webSocketSession.send(unicastProcessor.next()
                .map(Message::toString).map(webSocketSession::textMessage))
                .thenMany(webSocketSession.receive()).map(WebSocketMessage::getPayloadAsText).log().then());
    }
}
