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
package com.axway.ats.agent.core.ant.needed_in_acgen_tests;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.axway.ats.agent.core.ant.ActionJavadocExtractor;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;

/**
 * The actions of this class return different types
 */
public class TestWithInputValuesClass {

    @Action(name = "TestWithInputValuesClass call1")
    public void call1(
                       @Parameter(name = "arg1") int[] arg1 ) {

    }

    ActionJavadocExtractor fakeMethod() {

        return null;
    }

    @Action(name = "TestWithInputValuesClass call2")
    public void call2(
                       @Parameter(name = "arg1") Map<String, TestType> arg1 ) {

    }

    @Action(name = "TestWithInputValuesClass call3")
    public void call3(
                       @Parameter(name = "arg1") List<Calendar> arg1 ) {

    }

    @Action(name = "TestWithInputValuesClass call4")
    public void call4(
                       @Parameter(name = "arg1") List<Integer> arg1 ) {

    }

    @Action(name = "TestWithInputValuesClass call5")
    public void call5(
                       @Parameter(name = "arg1") Long arg1 ) {

    }

    @Action(name = "TestWithInputValuesClass call6")
    public void call6(
                       @Parameter(name = "arg1") String arg1 ) {

    }

    @Action(name = "TestWithInputValuesClass call7")
    public void call7(
                       @Parameter(name = "arg1") Date arg1 ) {

    }

    @Action(name = "TestWithInputValuesClass call9")
    public void call9(
                       @Parameter(name = "arg1") Map<TestType, Locale> arg1,
                       @Parameter(name = "arg1") TestType[] arg2,
                       @Parameter(name = "arg1") TestType arg3,
                       @Parameter(name = "arg1") Set<String> arg4 ) {

    }

    @Action(name = "TestWithInputValuesClass getMyType2")
    public void getMyType2(
                            @Parameter(name = "arg1") TestType arg1 ) {

    }
}
