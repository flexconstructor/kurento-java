/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.reactive.jsonrpc.client;

import reactor.core.publisher.Mono;

public interface JsonRpcWSConnectionListener {

  /**
   * Method invoked when the JsonRpcWS client successfully connects to the server
   */
  Mono<Void> connected();

  /**
   * Method invoked when the JsonRpcWS client could not connect to the server. This method can be
   * invoked also if a reconnection is needed.
   */
  Mono<Void> connectionFailed();

  /**
   * Method invoked when the JsonRpcWS client connection with the server is interrupted
   */
  Mono<Void> disconnected();

  /**
   * Method invoked when the JsonRpcWS client is reconnected to a server
   */
  Mono<Void> reconnected(boolean sameServer);

  /**
   * Method invoked when the JsonRpcWS client is disconnected from a server and it is trying to
   * reconnect
   */
  Mono<Void> reconnecting();

}
