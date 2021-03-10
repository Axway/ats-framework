/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.log.autodb.model;

import java.sql.SQLException;

import org.apache.logging.log4j.core.Layout;

import com.axway.ats.log.autodb.exceptions.LoggingException;
import com.axway.ats.log.autodb.logqueue.LogEventRequest;

/**
 * All classes that process a logging event should implement this interface
 */
public interface EventRequestProcessor {

    /**
     * Process a single event request
     * 
     * @param eventRequest the request
     * @throws LoggingException on error
     * @throws SQLException 
     */
    public void processEventRequest(
                                     LogEventRequest eventRequest ) throws LoggingException;

    /**
     * Set the layout for logging messages
     * 
     * @param layout the layout
     */
    public void setLayout(
                           Layout layout );

}
