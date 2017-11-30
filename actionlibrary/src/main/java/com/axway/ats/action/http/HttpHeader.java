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
package com.axway.ats.action.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.axway.ats.common.PublicAtsApi;

/**
 * A representation of a HTTP header
 */
@PublicAtsApi
public class HttpHeader {

    protected String       name;
    protected List<String> values;

    /**
     * Construct object with given header name and corresponding values
     * @param name header name
     * @param values either String array or String with comma delimited values if they are multiple
     */
    public HttpHeader( String name,
                       Object values ) {

        this.name = name;

        if (values.getClass().isArray()) {
            // expect array of String
            this.values = Arrays.asList((String[]) values);
        } else {
            // expect String or String with ',' inside which divides multiple values
            this.values = Arrays.asList( ((String) values).split(","));
        }
    }

    /**
     * Construct new header pair with only single value
     * @param name header name
     * @param value string value
     */
    @PublicAtsApi
    public HttpHeader( String name,
                       String value ) {

        this.name = name;
        this.values = new ArrayList<String>();
        this.values.add(value);
    }

    /**
     * Construct new header pair with several values
     * @param name header name
     * @param value list of values
     */
    @PublicAtsApi
    public HttpHeader( String name,
                       List<String> values ) {

        this.name = name;
        this.values = values;
    }

    /**
     * Get HTTP header name. Note that according to HTTP 1.1 spec.
     * it should be compared insensitively.
     * @return the header name
     */
    @PublicAtsApi
    public String getKey() {

        return name;
    }

    /**
     * @return the header value as a String
     */
    @PublicAtsApi
    public String getValue() {

        if (values == null) {
            return null;
        }

        if (values.size() > 0) {
            return values.get(0);
        } else {
            return null;
        }
    }

    /**
     * @param index the index of the header value
     * @return as String the header values pointed by the provided index
     */
    @PublicAtsApi
    public String getValue(
                            int index ) {

        if (values == null) {
            return null;
        }

        if (values.size() > index) {
            return values.get(index);
        } else {
            throw new HttpException("Invalid header value index " + index);
        }
    }

    /**
     * @return the header values as a list of String
     */
    @PublicAtsApi
    public List<String> getValues() {

        return values;
    }

    public void addValue(
                          String value ) {

        if (values == null) {
            values = new ArrayList<String>();
        }
        values.add(value);
    }
}
