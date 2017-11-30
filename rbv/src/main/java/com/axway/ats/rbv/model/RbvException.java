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
package com.axway.ats.rbv.model;

import com.axway.ats.common.PublicAtsApi;

@SuppressWarnings( "serial")
@PublicAtsApi
public class RbvException extends RuntimeException {

    @PublicAtsApi
    public RbvException( String message ) {

        super(message);
    }

    @PublicAtsApi
    public RbvException( Throwable cause ) {

        super(cause);
    }

    @PublicAtsApi
    public RbvException( String message,
                         Throwable cause ) {

        super(message, cause);
    }
}
