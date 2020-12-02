/*
 * Copyright 2019 Axway Software
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
package com.axway.ats.core.reflect;

final public class TestClassReflectionUtils {

    private static final String FINAL_STATIC_FIELD = "You cannot change me!";

    private final String        FINAL_FIELD        = "You cannot change me!";

    private static String       staticField        = "You cannot change me!";

    private int                 integerField       = 9;
    private Boolean             booleanField       = true;
    private Byte                byteField          = 127;
    private char                charField          = '*';

    public TestClassReflectionUtils( int integerField, Boolean booleanField, Byte byteField, char charField ) {

        this.integerField = integerField;
        this.booleanField = booleanField;
        setByteField(byteField);
        setCharField(charField);
    }

    private void setByteField( Byte byteField ) {

        this.byteField = byteField;
    }

    private void setCharField( char charField ) {

        this.charField = charField;
    }

    private TestClassReflectionUtils copy() {

        TestClassReflectionUtils copy = new TestClassReflectionUtils(integerField, booleanField, byteField, charField);

        return copy;

    }

    private boolean isEqual( TestClassReflectionUtils other ) {

        return integerField == other.integerField && booleanField == other.booleanField && byteField == other.byteField
               && charField == other.charField;

    }

}
