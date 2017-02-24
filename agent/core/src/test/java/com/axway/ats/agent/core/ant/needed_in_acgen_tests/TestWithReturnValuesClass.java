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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.axway.ats.agent.core.ant.ActionJavadocExtractor;
import com.axway.ats.agent.core.model.Action;

/**
 * The actions of this class return different types
 */
public class TestWithReturnValuesClass {

    /**
     * This method returns array of int
     * @return int array
     */
    @Action(name = "TestWithReturnValuesClass call1")
    public int[] call1() {

        return new int[0];
    }

    ActionJavadocExtractor fakeMethod() {

        return null;
    }

    /**
     * returns {@link Map} with {@link String} keys and {@link TestType} values
     * @return Map with strings and testTypes values
     */
    @Action(name = "TestWithReturnValuesClass call2")
    public Map<String, TestType> call2() {

        return new HashMap<String, TestType>();
    }

    @Action(name = "TestWithReturnValuesClass call3")
    public List<Calendar> call3() {

        List<Calendar> list = new ArrayList<Calendar>();
        list.add( new GregorianCalendar() );
        return list;
    }

    @Action(name = "TestWithReturnValuesClass call4")
    public List<Integer> call4() {

        List<Integer> list = new ArrayList<Integer>();
        list.add( new Integer( 5 ) );
        return list;
    }

    @Action(name = "TestWithReturnValuesClass call5")
    public Long call5() {

        return new Long( 342l );
    }

    @Action(name = "TestWithReturnValuesClass call6")
    public String call6() {

        return "fasfdfd";
    }

    @Action(name = "TestWithReturnValuesClass call7")
    public Date call7() {

        return new Date();
    }

    @Action(name = "TestWithReturnValuesClass call8")
    public void call8() {

        System.out.println( "Test!" );
    }

    @Action(name = "TestWithReturnValuesClass call9")
    public Map<TestType, Locale> call9() {

        return new HashMap<TestType, Locale>();
    }

    @Action(name = "TestWithReturnValuesClass call10")
    public TestType[] call10() {

        return new TestType[0];
    }

    @Action(name = "TestWithReturnValuesClass getMyType")
    public TestType getMyType() {

        return new TestType();
    }

    @Action(name = "TestWithReturnValuesClass getMyType2")
    public TestType getMyType2() {

        return new TestType();
    }
}
