/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.reactive.jsonrpc.internal.client;

import org.kurento.reactive.jsonrpc.JsonUtils;
import org.kurento.reactive.jsonrpc.Session;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

public abstract class AbstractSession<R> implements Session<R> {

    private String sessionId;
    private Object registerInfo;
    private boolean newSession = true;

    public AbstractSession(String sessionId, Object registerInfo) {
        this.sessionId = sessionId;
        this.registerInfo = registerInfo;
    }

    @Override
    public Object getRegisterInfo() {
        return registerInfo;
    }


    @Override
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public boolean isNew() {
        return newSession;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void setReconnectionTimeout(long millis) {

    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    public void setNew(boolean newSession) {
        this.newSession = newSession;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (sessionId == null ? 0 : sessionId.hashCode());
        return result;
    }

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

    @Override
    public Mono<R> sendRequest(String method, Class resultClass) {
        return this.sendRequest(method, Mono.empty(), resultClass);
    }

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


    @Override
    public Mono<Void> sendNotification(String method, Mono params) {
        return null;
    }

    @Override
    public Mono<Void> sendNotification(String method) {
        return this.sendNotification(method, Mono.empty());
    }

    @Override
    public Mono<Response<R>> sendRequest(Mono<Request<R>> request, Class<R> requestType) {
        return this.internalSendRequest(request, requestType);
    }

    public abstract Mono<Response<R>> internalSendRequest(Mono<Request<R>> requestMono, Class<R> requestType);


    private void setIdIfNecessary(Request<R> request) {
        if (request.getId() == null) {
            request.setId(Integer.valueOf(this.getSessionId()));
        }
    }

}
