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
package com.axway.ats.core.monitoring;

/**
 * Thrown whenever there is a request for an unsupported reading type
 */
@SuppressWarnings( "serial")
public class UnsupportedReadingException extends RuntimeException {

    /**
     * Constructor
     * @param readingName
     */
    public UnsupportedReadingException( String readingName ) {

        super("Reading '" + readingName + "' is not supported");
    }

}
