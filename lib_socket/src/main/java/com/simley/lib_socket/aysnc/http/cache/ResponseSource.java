/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simley.lib_socket.aysnc.http.cache;

enum ResponseSource {

    /**
     * Return the response from the cache immediately.
     */
    CACHE,

    /**
     * Make a conditional request to the host, returning the cache response if
     * the cache is valid and the network response otherwise.
     */
    CONDITIONAL_CACHE,

    /**
     * Return the response from the network.
     */
    NETWORK;

    public boolean requiresConnection() {
        return this == CONDITIONAL_CACHE || this == NETWORK;
    }
}
