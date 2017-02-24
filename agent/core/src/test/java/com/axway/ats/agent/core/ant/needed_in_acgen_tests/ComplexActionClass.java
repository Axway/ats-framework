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

public class ComplexActionClass {

    @SuppressWarnings("unused")
    private TestType                   someType;

    /**
     * comment for INT_CONSTANT
     */
    public static final int            INT_CONSTANT                                      = 1;

    /** comment for STRING_CONSTANT
     */

    public static final String         STRING_CONSTANT                                   = "one";

    /** comment for NOT_VALID_CONSTANT_DUE_TO_MISSING_PUBLIC_MODIFIER     */
    static final int                   NOT_VALID_CONSTANT_DUE_TO_MISSING_PUBLIC_MODIFIER = 2;
    /**comment for NOT_VALID_CONSTANT_DUE_TO_MISSING_STATIC_MODIFIER*/
    public final int                   NOT_VALID_CONSTANT_DUE_TO_MISSING_STATIC_MODIFIER = 3;

    /**comment for //*  /* //**  ////**  //**  NOT_VALID_CONSTANT_DUE_TO_MISSING_FINAL_MODIFIER*/
    public static int                  NOT_VALID_CONSTANT_DUE_TO_MISSING_FINAL_MODIFIER  = 4;

    //
    // comment for INT_CONSTANTS
    //
    public static final int[]          INT_CONSTANTS                                     = { 1,
            2,
            3,
            4,
            5,
            6,
            7,
            8                                                                           };
    // comment for STRING_CONSTANTS
    public static final String[]       STRING_CONSTANTS                                  = { "one",
            "one",
            "two",
            "three",
            "four"                                                                      };

    /*
     * sedas
     * dsa *
     * dsa /*
     * ds
     * ad
     * sadsadsada
     */
    public static final SomeTestEnum   ENUM_CONSTANT_ONE                                 = SomeTestEnum.ONE;
    public static final SomeTestEnum   ENUM_CONSTANT_TWO                                 = SomeTestEnum.TWO;

    public static final SomeTestEnum[] ENUM_CONSTANTS                                    = { SomeTestEnum.ONE,
            SomeTestEnum.TWO,
            SomeTestEnum.THREE                                                          };

    // my fake threads
    @SuppressWarnings("unused")
    private MyInnerThread              myInnerThread                                     = new MyInnerThread();
    @SuppressWarnings("unused")
    private MyOuterThread              myOuterThread                                     = new MyOuterThread();

    @SuppressWarnings("unused")
    @Action(name = "Complex Action Class privateAction")
    private void privateAction() {

    }

    @Action(name = "Complex Action Class protectedAction")
    protected String protectedAction() {

        return null;
    }

    @Action(name = "Complex Action Class packageScopeAction")
    protected int packageScopeAction() {

        return 0;
    }

    public byte[] publicMethodNotAnAction() {

        return new byte[]{};
    }

    //    @Action(name = "Complex Action Class action1")
    //    public void action1(
    //                         @Parameter(name = "firstEnumParameter") SomeTestEnum firstEnumParameter,
    //                         @Parameter(name = "secondEnumParameter") SomeTestEnum secondEnumParameter ) {
    //
    //    }

    /**
     * comment for action2
     */
    @Action(name = "Complex Action Class action2")
    public void action2() {

    }

    /** * //*  /* //**  ////**  //**  comment for action3 */
    @Action(name = "Complex Action Class action3")
    public void action3() throws RuntimeException {

    }

    /**
     * comment for action4
     */
    @Action(name = "Complex Action Class action4")
    public void action4() throws Exception {

    }

    @Action(name = "Complex Action Class action5")
    /**
    * comment for action5
    * THIS COMMENT IS SKIPPED
    */
    public void action5() {

    }

    /**
     * comment for action4
    3232*/
    @Action(name = "Complex Action Class action6")
    public void action6() {

    }

    /**
     * This method returns array of int
     * @return int array
     */

    @Action(name = "Complex Action Class call1")
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
    @Action(name = "Complex Action Class call2")
    public Map<String, TestType> call2() {

        return new HashMap<String, TestType>();
    }

    @Action(name = "Complex Action Class call3")
    public List<Calendar> call3() {

        List<Calendar> list = new ArrayList<Calendar>();
        list.add( new GregorianCalendar() );
        return list;
    }

    @Action(name = "Complex Action Class call4")
    public List<Integer> call4() {

        List<Integer> list = new ArrayList<Integer>();
        list.add( new Integer( 5 ) );
        return list;
    }

    @Action(name = "Complex Action Class call5")
    public Long call5() {

        return new Long( 342l );
    }

    @Action(name = "Complex Action Class call6")
    public String call6() {

        return "fasfdfd";
    }

    @Action(name = "Complex Action Class call7")
    public Date call7() {

        return new Date();
    }

    @Action(name = "Complex Action Class call8")
    public void call8() {

        System.out.println( "Test!" );
    }

    @Action(name = "Complex Action Class call9")
    public Map<TestType, Locale> call9() {

        return new HashMap<TestType, Locale>();
    }

    @Action(name = "Complex Action Class call10")
    public TestType[] call10() {

        return new TestType[0];
    }

    @Action(name = "Complex Action Class getMyType")
    public TestType getMyType() {

        return new TestType();
    }

    @Action(name = "Complex Action Class getMyType2")
    public TestType getMyType2() {

        return new TestType();
    }

    class MyInnerThread implements Runnable {

        /*
         * (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {

        }
    }
}

class MyOuterThread implements Runnable {

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

    }
}
