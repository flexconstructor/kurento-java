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
import org.kurento.reactive.jsonrpc.client.RequestAlreadyRespondedException;
import org.kurento.reactive.jsonrpc.message.Message;
import org.kurento.reactive.jsonrpc.message.Request;
import org.kurento.reactive.jsonrpc.message.Response;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.kurento.reactive.jsonrpc.JsonUtils.INJECT_SESSION_ID;

public class TransactionImpl extends AbstractTransaction {

    public interface ResponseSender {
        Mono<Void> sendResponse(Message message);

        Mono<Void> sendPingResponse(Message message);
    }

    private final AtomicBoolean responded = new AtomicBoolean(false);
    private final ResponseSender responseSender;

    public TransactionImpl(Session session, Request<?> request, ResponseSender responseSender) {
        super(session, request);
        this.responseSender = responseSender;
    }

    public boolean setRespondedIfNot() {
        return responded.compareAndSet(false, true);
    }

    protected Mono<Void> internalSendResponse(Response<? extends Object> response) {

        boolean notResponded = setRespondedIfNot();

        if (notResponded) {

            if (response.getSessionId() == null && INJECT_SESSION_ID) {
                response.setSessionId(session.getSessionId());
            }

            if (response.getId() == null) {
                response.setId(request.getId());
            }

            return responseSender.sendResponse(response);

        } else {
            throw new RequestAlreadyRespondedException("This request has already been responded");
        }
    }

}
