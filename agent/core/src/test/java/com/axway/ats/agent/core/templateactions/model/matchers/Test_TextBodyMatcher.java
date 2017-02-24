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
package com.axway.ats.agent.core.templateactions.model.matchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Test_TextBodyMatcher {

    @Test
    public void matchContains() throws Exception {

        TextBodyMatcher matcher = new TextBodyMatcher( "ABC", false );

        assertTrue( matcher.performMatch( null, "123 ABC 456" ) );
    }

    @Test
    public void matchDoesNotContain() throws Exception {

        TextBodyMatcher matcher = new TextBodyMatcher( "ABC", false, true );

        assertTrue( matcher.performMatch( null, "123 BBB 456" ) );
    }

    @Test
    public void matchContainsNegative() throws Exception {

        TextBodyMatcher matcher = new TextBodyMatcher( "ABC", false );

        assertFalse( matcher.performMatch( null, "123 AAA 456" ) );
    }

    @Test
    public void matchRegex() throws Exception {

        TextBodyMatcher matcher = new TextBodyMatcher( ".*ABC", true );

        assertTrue( matcher.performMatch( null, "123 ABC" ) );
    }

    @Test
    public void matchRegexNotToContain() throws Exception {

        TextBodyMatcher matcher = new TextBodyMatcher( ".*ABC", true, true );

        assertTrue( matcher.performMatch( null, "123 ABB" ) );
    }

    @Test
    public void matchRegexNegative() throws Exception {

        TextBodyMatcher matcher = new TextBodyMatcher( ".*ABC", true );

        assertFalse( matcher.performMatch( null, "123 ABB" ) );
    }

    @Test
    public void matchRegexNotToContainNegative() throws Exception {

        TextBodyMatcher matcher = new TextBodyMatcher( ".*ABC", true, true );

        assertFalse( matcher.performMatch( null, "123 ABCD" ) );
    }

}
