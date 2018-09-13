/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
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

package de.adorsys.aspsp.xs2a.spi.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum ContentType {

    XML("application/xml"),
    JSON("application/json"),
    TXT("text/plain"),
    EMPTY("*/*");

    private static Map<String, ContentType> container = new HashMap<>();

    static {
        Arrays.stream(values())
            .forEach(contentType -> container.put(contentType.getType(), contentType));
    }

    private String type;

    @JsonCreator
    ContentType(String type) {
        this.type = type;
    }

    @JsonValue
    public String getType() {
        return type;
    }

    @JsonIgnore
    public static Optional<ContentType> getByName(String name) {
        return Optional.ofNullable(container.get(name));
    }
}
