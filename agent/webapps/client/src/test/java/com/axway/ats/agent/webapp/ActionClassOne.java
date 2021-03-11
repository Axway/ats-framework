/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.agent.webapp;

import org.apache.logging.log4j.LogManager;

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.core.log.AtsLog4jLogger;
import com.axway.ats.core.validation.ValidationType;

public class ActionClassOne {
    
    static {
        AtsLog4jLogger.setLog4JConsoleLoggingOnly();
    }

    public static int    ACTION_VALUE    = 0;

    private static int[] VALID_CONSTANTS = { 1, 2, 3, 4, 5, 6, 7, 8 };

    @Action( name = "action 1")
    public void action1(
                         @Parameter( name = "valueToMatch") int value ) {

        ACTION_VALUE = value;
        LogManager.getLogger(ActionClassOne.class).info("Method action 1 has been executed");
    }

    @Action( name = "action array")
    public int action1(
                        @Parameter( name = "valueToMatch", validation = ValidationType.NUMBER_CONSTANT, args = { "VALID_CONSTANTS" }) int[] values ) {

        ACTION_VALUE = values[values.length - 1];
        LogManager.getLogger(ActionClassOne.class).info("Method action array has been executed");

        return ACTION_VALUE;
    }

    @Action( name = "action long")
    public long actionLong(
                            @Parameter( name = "valueToMatch") long value ) {

        return value;
    }

    @Action( name = "action double")
    public double actionDouble(
                                @Parameter( name = "valueToMatch") double value ) {

        return value;
    }

    @Action( name = "action float")
    public float actionFloat(
                              @Parameter( name = "valueToMatch") float value ) {

        return value;
    }

    @Action( name = "action boolean")
    public boolean actionBoolean(
                                  @Parameter( name = "valueToMatch") boolean value ) {

        return value;
    }

    @Action( name = "action short")
    public short actionShort(
                              @Parameter( name = "valueToMatch") short value ) {

        return value;
    }

    @Action( name = "action byte")
    public byte actionByte(
                            @Parameter( name = "valueToMatch") byte value ) {

        return value;
    }

    @Action( name = "action string")
    public String actionString(
                                @Parameter( name = "valueToMatch", validation = ValidationType.STRING_NOT_EMPTY) String value ) {

        return value;
    }

    @Action( name = "action wrong type")
    public ActionClassOne actionString(
                                        @Parameter( name = "valueToMatch") ActionClassOne value ) {

        return value;
    }
}
