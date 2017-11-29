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
/**
 * 
 */
package com.axway.ats.log.report.exceptions;

/**
 * This exception indicates an error while formatting the report 
 */
@SuppressWarnings("serial")
public class ReportFormatterException extends RuntimeException {

    /**
     * @param arg0
     */
    public ReportFormatterException( String arg0 ) {

        super( arg0 );
    }
}
