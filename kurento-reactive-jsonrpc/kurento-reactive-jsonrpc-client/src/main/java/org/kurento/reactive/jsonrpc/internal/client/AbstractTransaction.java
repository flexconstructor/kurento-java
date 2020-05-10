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

import org.kurento.reactive.jsonrpc.Session;
import org.kurento.reactive.jsonrpc.Transaction;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.message.ResponseError;
import reactor.core.publisher.Mono;

public abstract class AbstractTransaction implements Transaction {

    protected final Session session;
    protected boolean async;
    protected final Request<?> request;

    public AbstractTransaction(Session session, Request<?> request) {
        super();
        this.session = session;
        this.request = request;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public void startAsync() {
        async = true;
    }

    public boolean isAsync() {
        return async;
    }

    @Override
    public Mono<Void> sendResponse(Object result) {
        return internalSendResponse(new Response<>(request.getId(), result));
    }

    @Override
    public Mono<Void> sendError(int code, String type, String data) {
        return internalSendResponse(new Response<>(request.getId(), new ResponseError(code, type, data)));
    }

    @Override
    public Mono<Void> sendError(int code, String type, String message, String data) {
        return internalSendResponse(
                new Response<>(request.getId(), new ResponseError(code, type, message, data)));
    }

    @Override
    public Mono<Void> sendError(Throwable e) {
        ResponseError error = ResponseError.newFromException(e);
        return internalSendResponse(new Response<>(request.getId(), error));
    }

    @Override
    public Mono<Void> sendVoidResponse() {
        return sendResponse(null);
    }

    @Override
    public Mono<Void> sendError(ResponseError error) {
        return internalSendResponse(new Response<>(request.getId(), error));
    }

    @Override
    public Mono<Void> sendResponseObject(Response<? extends Object> response) {
        return internalSendResponse(response);
    }

    @Override
    public boolean isNotification() {
        return request.getId() == null;
    }

    protected abstract Mono<Void> internalSendResponse(Response<? extends Object> response);

}
