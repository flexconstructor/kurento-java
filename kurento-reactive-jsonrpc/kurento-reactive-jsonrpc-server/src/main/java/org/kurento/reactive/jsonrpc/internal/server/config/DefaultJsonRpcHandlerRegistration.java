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

import org.kurento.reactive.jsonrpc.JsonRpcHandler;
import org.kurento.reactive.jsonrpc.server.JsonRpcHandlerRegistration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;

public class DefaultJsonRpcHandlerRegistration implements JsonRpcHandlerRegistration {

    private final MultiValueMap<JsonRpcHandler<?>, String> handlerMap = new LinkedMultiValueMap<>();
    private final MultiValueMap<Class<? extends JsonRpcHandler<?>>, String> perSessionHandlerClassMap =
            new LinkedMultiValueMap<>();

    @Override
    public JsonRpcHandlerRegistration addHandler(JsonRpcHandler<?> handler, String... paths) {
        this.handlerMap.put(handler, Arrays.asList(paths));
        return this;
    }

    MultiValueMap<JsonRpcHandler<?>, String> getHandlerMap() {
        return handlerMap;
    }

    MultiValueMap<Class<? extends JsonRpcHandler<?>>, String> getPerSessionHandlerClassMap() {
        return perSessionHandlerClassMap;
    }

}
