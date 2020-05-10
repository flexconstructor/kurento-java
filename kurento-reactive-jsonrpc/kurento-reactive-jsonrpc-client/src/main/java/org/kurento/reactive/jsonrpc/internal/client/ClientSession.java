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

import org.kurento.reactive.jsonrpc.JsonRpcException;
import org.kurento.reactive.jsonrpc.client.JsonRpcClient;
import org.kurento.reactive.jsonrpc.internal.JsonRpcRequestSender;
import org.kurento.reactive.jsonrpc.message.Response;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientSession extends AbstractSession {

    private JsonRpcRequestSender requestSender;
    private volatile ConcurrentMap<String, Object> attributes;

    public ClientSession(String sessionId, Object registerInfo,
                         JsonRpcRequestSender jsonRpcRequestSender) {
        super(sessionId, registerInfo);
        this.requestSender = jsonRpcRequestSender;
    }

    /**
     * Sends request to client with given response type.
     *
     * @param mono        {@link Mono <Request>} request.
     * @param requestType Type of {@link Response}.
     * @return {@link Mono<Response>}
     */
    @Override
    public Mono<Response> internalSendRequest(Mono mono, Class requestType) {
        // TODO: Check it!
        return this.requestSender.sendRequest(mono, requestType);
    }

    /**
     * Closes the session.
     *
     * @throws IOException it can trows exception.
     */
    @Override
    public Mono<Void> close()  {
        /*if (requestSender instanceof JsonRpcClient) {
            return ((JsonRpcClient) requestSender).close("Client session closed");
        }*/
        return ((JsonRpcClient) requestSender).close("Client session closed");
    }

    @Override
    public void setReconnectionTimeout(long millis) {
        throw new JsonRpcException("Reconnection timeout can't be configured in the client");
    }

    /**
     * Returns session attributes.
     *
     * @return {@link Map<String, Object>}.
     */
    @Override
    public Map<String, Object> getAttributes() {
        if (attributes == null) {
            synchronized (this) {
                if (attributes == null) {
                    attributes = new ConcurrentHashMap<>();
                }
            }
        }

        return attributes;
    }

  /*public ClientSession(String sessionId, Object registerInfo) {
    super(sessionId, registerInfo);
  }

  public void setRequestSender(JsonRpcRequestSender requestSender) {
    this.requestSender = requestSender;
  }

  @Override
  public void close() throws IOException {
    if (requestSender instanceof JsonRpcClient) {
      ((JsonRpcClient) requestSender).close();
    }
  }

  @Override
  public <R> R sendRequest(String method, Class<R> resultClass) throws IOException {
    return requestSender.sendRequest(method, resultClass);
  }

  @Override
  public <R> R sendRequest(String method, Object params, Class<R> resultClass) throws IOException {
    return requestSender.sendRequest(method, params, resultClass);
  }

  @Override
  public JsonElement sendRequest(String method) throws IOException {
    return requestSender.sendRequest(method);
  }

  @Override
  public JsonElement sendRequest(String method, Object params) throws IOException {
    return requestSender.sendRequest(method, params);
  }

  @Override
  public void sendNotification(String method, Object params) throws IOException {
    requestSender.sendNotification(method, params);
  }

  @Override
  public void sendNotification(String method) throws IOException {
    requestSender.sendNotification(method);
  }

  @Override
  public void sendRequest(String method, JsonObject params,
      Continuation<JsonElement> continuation) {
    requestSender.sendRequest(method, params, continuation);
  }

  @Override
  public void sendNotification(String method, Object params, Continuation<JsonElement> continuation)
      throws IOException {
    requestSender.sendNotification(method, params, continuation);
  }

  @Override
  public Response<JsonElement> sendRequest(Request<JsonObject> request) throws IOException {
    return requestSender.sendRequest(request);
  }

  @Override
  public void sendRequest(Request<JsonObject> request,
      Continuation<Response<JsonElement>> continuation) throws IOException {
    requestSender.sendRequest(request, continuation);
  }

  @Override
  public void sendRequestHonorId(Request<JsonObject> request,
      Continuation<Response<JsonElement>> continuation) throws IOException {
    requestSender.sendRequestHonorId(request, continuation);
  }

  @Override
  public Response<JsonElement> sendRequestHonorId(Request<JsonObject> request) throws IOException {
    return requestSender.sendRequestHonorId(request);
  }

  @Override
  public void setReconnectionTimeout(long millis) {
    throw new JsonRpcException("Reconnection timeout can't be configured in the client");
  }

  @Override
  public Map<String, Object> getAttributes() {
    if (attributes == null) {
      synchronized (this) {
        if (attributes == null) {
          attributes = new ConcurrentHashMap<>();
        }
      }
    }

    return attributes;
  }*/
}
