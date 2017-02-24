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

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;

public class ActionClassWithConstants {

    public static final int            INT_CONSTANT                                      = 1;
    public static final String         STRING_CONSTANT                                   = "one";
    static final int                   NOT_VALID_CONSTANT_DUE_TO_MISSING_PUBLIC_MODIFIER = 2;
    public final int                   NOT_VALID_CONSTANT_DUE_TO_MISSING_STATIC_MODIFIER = 3;
    public static int                  NOT_VALID_CONSTANT_DUE_TO_MISSING_FINAL_MODIFIER  = 4;

    public static final int[]          INT_CONSTANTS                                     = { 1,
            2,
            3,
            4,
            5,
            6,
            7,
            8                                                                           };
    public static final String[]       STRING_CONSTANTS                                  = { "one",
            "one",
            "two",
            "three",
            "four"                                                                      };

    public static final SomeTestEnum   ENUM_CONSTANT_ONE                                 = SomeTestEnum.ONE;
    public static final SomeTestEnum   ENUM_CONSTANT_TWO                                 = SomeTestEnum.TWO;

    public static final SomeTestEnum[] ENUM_CONSTANTS                                    = { SomeTestEnum.ONE,
            SomeTestEnum.TWO,
            SomeTestEnum.THREE                                                          };

    @Action(name = "Action Class With Constants Action With Enum Parameters")
    public void actionWithEnumParameters(
                                          @Parameter(name = "firstEnumParameter") SomeTestEnum firstEnumParameter,
                                          @Parameter(name = "secondEnumParameter") SomeTestEnum secondEnumParameter ) {

    }

}
