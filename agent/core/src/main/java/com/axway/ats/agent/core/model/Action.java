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
 * This annotation is used to declare a java method as an ATS Agent Action.
 * Note that all action names should be unique in the scope of the Agent component.
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.METHOD)
public @interface Action {

    /**
     * Friendly name of the action, issued by the client
     */
    String name() default "";

    /**
     * Used if the action performs a transfer of any kind
     * If this attribute is set, the action must return a result
     * of type long
     */
    String transferUnit() default "";

    /**
     * Specify if the action execution will be registered in the database.
     * <br>The default value is <b>true</b>
     * <br><br>When set to false:
     * <li>the action will be missing on the Test Explorer's <i>Performance actions</i> tab
     * <li>the action response time will not be included in the <i>Queue execution time</i>
     */
    boolean registerActionExecution() default true;

    /**
     * Specify if the action response time will be included in
     * the <i>Queue execution time</i> on the Test Explorer's <i>Performance actions</i> tab
     * <br>The default value is <b>true</b>
     * <br><br><b>Note:</b> this parameter has no effect when the {@link #registerActionExecution()} is false
     */
    boolean registerActionExecutionInQueueExecutionTime() default true;
}
