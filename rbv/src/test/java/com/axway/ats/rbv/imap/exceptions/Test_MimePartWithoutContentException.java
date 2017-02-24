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
package com.axway.ats.rbv.imap.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.axway.ats.action.objects.model.MimePartWithoutContentException;
import com.axway.ats.rbv.BaseTest;

public class Test_MimePartWithoutContentException extends BaseTest {

    @Test
    public void constructors() {

        MimePartWithoutContentException exception;

        exception = new MimePartWithoutContentException( "test" );
        assertEquals( "test", exception.getMessage() );
        assertNull( exception.getCause() );

        Exception helperException = new Exception();
        exception = new MimePartWithoutContentException( "test", helperException );
        assertEquals( "test", exception.getMessage() );
        assertEquals( helperException, exception.getCause() );

        exception = new MimePartWithoutContentException( helperException );
        assertEquals( "java.lang.Exception", exception.getMessage() );
        assertEquals( helperException, exception.getCause() );
    }
}
