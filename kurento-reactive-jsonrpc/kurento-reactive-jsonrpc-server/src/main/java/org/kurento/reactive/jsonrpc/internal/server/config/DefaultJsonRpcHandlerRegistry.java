/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kurento.reactive.jsonrpc.internal.server.config;

import com.google.gson.JsonObject;
import org.kurento.reactive.jsonrpc.JsonRpcHandler;
import org.kurento.reactive.jsonrpc.server.JsonRpcHandlerRegistration;
import org.kurento.reactive.jsonrpc.server.JsonRpcHandlerRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link JsonRpcHandlerRegistry}.
 */
public class DefaultJsonRpcHandlerRegistry implements JsonRpcHandlerRegistry {

    /**
     * List of JSON RPC handler registrations.
     */
    private final List<DefaultJsonRpcHandlerRegistration> registrations = new ArrayList<>();

    /**
     * Adds new instance of {@link JsonRpcHandler} by path.
     *
     * @param webSocketHandler {@link JsonRpcHandler} instance.
     * @param paths            path.
     * @return
     */
    @Override
    public JsonRpcHandlerRegistration addHandler(JsonRpcHandler<JsonObject> webSocketHandler,
                                                 String... paths) {

        DefaultJsonRpcHandlerRegistration registration = new DefaultJsonRpcHandlerRegistration();
        registration.addHandler(webSocketHandler, paths);
        this.registrations.add(registration);
        return registration;
    }

    /**
     * Returns list of registrations.
     *
     * @return {@link List<DefaultJsonRpcHandlerRegistration>}
     */
    List<DefaultJsonRpcHandlerRegistration> getRegistrations() {
        return registrations;
    }
}
