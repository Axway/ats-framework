/*
 * Copyright 2017 Axway Software
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
package com.axway.ats.agent.core.templateactions.model.matchers.mode;

public enum TemplateHeaderMatchMode {

    /**
     * Verify the header value contains the specified value
     */
    CONTAINS,

    /**
     * Verify the header value is exactly as specified
     */
    EQUALS,

    /**
     * Verify the header value is within some static numeric range
     */
    RANGE,

    /**
     * Verify the header value is within the specified offset from the expected value
     */
    RANGE_OFFSET,

    /**
     * Verify the header value is one of the values in a provided list
     */
    LIST,

    /**
     * Match the header value using a regular expression
     */
    REGEX,

    /**
     * Always matches the header. This is a way to mark this header to be skipped.
     */
    RANDOM,

    /**
     * The matched value will be used to set a user parameter.
     */
    EXTRACT;
}
