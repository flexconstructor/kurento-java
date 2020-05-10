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

package org.kurento.reactive.jsonrpc;

import org.kurento.reactive.jsonrpc.message.Response;
import org.kurento.reactive.jsonrpc.message.ResponseError;
import reactor.core.publisher.Mono;

import java.io.IOException;

public interface Transaction {

  Mono<Void> sendResponseObject(Response<? extends Object> response);

  Mono<Void> sendVoidResponse();

  Mono<Void> sendResponse(Object result);

  Mono<Void> sendError(int code, String type, String data);

  Mono<Void> sendError(int code, String type, String message, String data);

  Mono<Void> sendError(Throwable e);

  Session getSession();

  void startAsync();

  boolean isNotification();

  Mono<Void> sendError(ResponseError error) throws IOException;

}
