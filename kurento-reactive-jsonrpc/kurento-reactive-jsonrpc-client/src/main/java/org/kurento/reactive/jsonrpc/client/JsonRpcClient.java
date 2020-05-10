package org.kurento.reactive.jsonrpc.client;

import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.reactive.jsonrpc.internal.JsonRpcRequestSender;
import org.kurento.reactive.jsonrpc.internal.client.ClientSession;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

public abstract class JsonRpcClient implements JsonRpcRequestSender {

    protected int idleTimeout = 300000;
    protected int heartbeatInterval = 0;
    protected int connectionTimeout =
            PropertiesManager.getProperty("jsonRpcClientWebSocket.connectionTimeout", 5000);
    protected String label = "";
    protected ClientSession session;

    /**
     * Sends JSON RPC request with method and result class parameters.
     *
     * @param method      String JSON RPC method name.
     * @param resultClass Result class.
     * @return Mono.
     */
    @Override
    public Mono sendRequest(String method, Class resultClass) {
        return null;
    }

    /**
     * Sends JSON RPC request with some parameters.
     *
     * @param method      String JSON RPC method name.
     * @param params      {@link Mono} parameters.
     * @param resultClass expected result class.
     * @return {@link Mono<Response>}.
     */
    @Override
    public Mono sendRequest(String method, Mono params, Class resultClass) {
        return null;
    }

    /**
     * Sends notification (any response is unexpected).
     *
     * @param method String JSON RPC method name.
     * @param params some parameters as Mono.
     * @return {@link Mono<Void>}
     */
    @Override
    public Mono<Void> sendNotification(String method, Mono params) {
        return null;
    }

    /**
     * Sends notification without any parameters.
     *
     * @param method String JSON RPC method name.
     * @return {@link Mono<Void>}
     */
    @Override
    public Mono<Void> sendNotification(String method) {
        return null;
    }

    /**
     * Sends {@link Mono<Request>} and expects response with given type.
     *
     * @param request     {@link Mono<Request>}
     * @param requestType class type of response.
     * @return {@link Mono< Response >}.
     */
    @Override
    public Mono<Response> sendRequest(Mono request, Class requestType) {
        return null;
    }

    /**
     * Closes the client with optional close message.
     *
     * @param closeMessage close message. It can be null.
     * @return {@link Mono<Void>}
     */
    public abstract Mono<Void> close(@Nullable String closeMessage);

    /**
     * Gets the connection timeout, in milliseconds, configured in the client.
     *
     * @return the timeout in milliseconds
     */
    public long getConnectionTimeout() {
        return this.connectionTimeout;
    }

    /**
     * Sets a connection timeout in milliseconds in the client. If after this timeout, the client
     * could not connect with the server, the {@link JsonRpcWSConnectionListener#connectionFailed()}
     * method will be invoked, and a {@link KurentoException} will be thrown.
     *
     * @param connectionTimeout the timeout in milliseconds
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Gets the idle timeout (i.e. the time after which the session is considered as idle if no
     * messages have been exchanged), in milliseconds, configured in the client.
     *
     * @return the timeout in milliseconds
     */
    public int getIdleTimeout() {
        return this.idleTimeout;
    }

    /**
     * Sets an idle timeout in milliseconds in the client. If after the configured time, no messages
     * have been exchanged between client and server, connection is reestablished automatically
     *
     * @param idleTimeout the timeout in milliseconds
     */
    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * Gets the configured heartbeat interval in milliseconds.
     *
     * @return the interval
     */
    public int getHeartbeatInterval() {
        return this.heartbeatInterval;
    }

    /**
     * Sets the heartbeat interval in milliseconds.
     *
     * @param interval in milliseconds
     */
    public void setHeartbeatInterval(int interval) {
        this.heartbeatInterval = interval;
    }
}
