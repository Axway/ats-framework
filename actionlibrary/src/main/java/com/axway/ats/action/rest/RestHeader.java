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
package com.axway.ats.action.rest;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.action.exceptions.RestException;
import com.axway.ats.action.http.HttpHeader;
import com.axway.ats.common.PublicAtsApi;

/**
 * A representation of a REST header
 */
@PublicAtsApi
public class RestHeader extends HttpHeader {

    public RestHeader( String name,
                       List<String> values ) {

        super( name, values );
    }

    /**
     * @param index the index of the header value
     * @return as String the header values pointed by the provided index
     */
    @PublicAtsApi
    public String getValue(
                            int index ) {

        if( values == null ) {
            return null;
        }

        if( values.size() > index ) {
            return values.get( index );
        } else {
            throw new RestException( "Invalid header value index " + index );
        }
    }

    static RestHeader constructRESTHeader(
                                           String name,
                                           List<Object> objects ) {

        List<String> values = new ArrayList<String>();
        for( Object object : objects ) {
            values.add( object.toString() );
        }

        return new RestHeader( name, values );
    }
}
