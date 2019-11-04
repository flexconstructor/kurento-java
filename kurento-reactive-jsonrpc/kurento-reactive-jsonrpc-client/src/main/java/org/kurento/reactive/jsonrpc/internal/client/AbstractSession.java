package org.kurento.reactive.jsonrpc.internal.client;

import org.kurento.reactive.jsonrpc.JsonUtils;
import org.kurento.reactive.jsonrpc.Session;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import reactor.core.publisher.Mono;

/**
 * Abstract implementation of {@link Session}.
 *
 * @param <R> data type.
 */
public abstract class AbstractSession<R> implements Session<R> {

    /**
     * Session ID.
     */
    private String sessionId;

    /**
     * Some request info.
     */
    private final Object registerInfo;

    /**
     * Is a new session.
     */
    private boolean newSession = true;

    /**
     * Constructs new instance of AbstractSession with given session ID and request info.
     *
     * @param sessionId    Session ID.
     * @param registerInfo Some request info.
     */
    public AbstractSession(String sessionId, Object registerInfo) {
        this.sessionId = sessionId;
        this.registerInfo = registerInfo;
    }

    /**
     * Returns request info
     *
     * @return request info.
     */
    @Override
    public Object getRegisterInfo() {
        return this.registerInfo;
    }

    /**
     * Returns session ID.
     *
     * @return session ID.
     */
    @Override
    public String getSessionId() {
        return this.sessionId;
    }

    /**
     * Sets session ID.
     *
     * @param sessionId {@link String} session ID.
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Returns true if is new Session.
     *
     * @return boolean.
     */
    @Override
    public boolean isNew() {
        return this.newSession;
    }

    /**
     * Sets new value for new session flag.
     *
     * @param newSession boolean.
     */
    public void setNew(boolean newSession) {
        this.newSession = newSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (sessionId == null ? 0 : sessionId.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractSession other = (AbstractSession) obj;
        if (sessionId == null) {
            return other.sessionId == null;
        } else return sessionId.equals(other.sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<R> sendRequest(String method, Class resultClass) {
        return this.sendRequest(method, Mono.empty(), resultClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<R> sendRequest(String method, Mono<R> paramsMono, Class<R> resultClass) {
        Mono requestMono = paramsMono.map(params -> {
            Request<R> request = new Request<>(null, method, (R) params);
            setIdIfNecessary(request);
            if (JsonUtils.INJECT_SESSION_ID) {
                request.setSessionId(sessionId);
            }
            return request;
        });
        return sendRequest(requestMono, resultClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> sendNotification(String method, Mono params) {
        return this.sendRequest(params.flatMap(param -> {
            Request request = new Request();
            request.setMethod(method);
            request.setParams(param);
            return request;
        }), (Class<R>) Void.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> sendNotification(String method) {
        return this.sendNotification(method, Mono.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Response<R>> sendRequest(Mono<Request<R>> request, Class<R> requestType) {
        return this.internalSendRequest(request, requestType);
    }

    /**
     * Sends request to client with given response type.
     *
     * @param requestMono {@link Mono<Request>} request.
     * @param requestType Type of {@link Response}.
     * @return {@link Mono<Response>}
     */
    public abstract Mono<Response<R>> internalSendRequest(Mono<Request<R>> requestMono, Class<R> requestType);


    private void setIdIfNecessary(Request<R> request) {
        if (request.getId() == null) {
            request.setId(Integer.valueOf(this.getSessionId()));
        }
    }
}
