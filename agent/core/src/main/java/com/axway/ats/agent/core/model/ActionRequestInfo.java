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
package com.axway.ats.agent.core.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation is used to declare additional information about execution of an ATS Agent Action.</p>
 * <p><strong>This annotation is for internal use only. Do not use it when creating your own actions.</strong></p>
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.METHOD)
public @interface ActionRequestInfo {

    /**
     * The URL ( relative to the ATS agent REST API ) that will serve the action request on the agent
     * */
    String requestUrl();

    /**
     * The HTTP method that will be used when the request is sent to the agent
     */
    String requestMethod();

}
