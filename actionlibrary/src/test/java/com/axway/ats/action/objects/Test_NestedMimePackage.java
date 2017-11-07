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
package com.axway.ats.action.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.BaseTest;
import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.NoSuchMimePackageException;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.core.utils.IoUtils;

public class Test_NestedMimePackage extends BaseTest {

    private MimePackage         emailMessage;
    private static final String MAIL_MESSAGE_PATH = Test_NestedMimePackage.class.getResource( "nestedMessages.msg" )
                                                                                .getPath();

    // path to nested messages
    private static final int[] NESTED_1          = new int[]{ 0 };
    private static final int[] NESTED_2          = new int[]{ 1 };
    private static final int[] NESTED_2_1        = new int[]{ 1, 0 };                    // nested message has a message nested into it
    private static final int[] NESTED_3          = new int[]{ 2 };

    @Before
    public void setUp() throws Exception {

        loadNestedMessage();
    }

    @Test
    public void getSubject() throws Exception {

        // first level nested messages
        assertEquals( "subject", getNestedMessage( NESTED_1 ).getSubject() );
        assertEquals( "1 nested message, 1 txt attachment", getNestedMessage( NESTED_2 ).getSubject() );

        assertEquals( "2 attachments, 0 nested messages", getNestedMessage( NESTED_3 ).getSubject() );

        // second level nested messages
        assertEquals( "some subject", getNestedMessage( NESTED_2_1 ).getSubject() );
    }

    @Test
    public void getSubjectCharset() throws Exception {

        assertNull( getNestedMessage( NESTED_2_1 ).getSubjectCharset() );

        //set the subject with a specific encoding
        getNestedMessage( NESTED_2_1 ).setSubject( "Subject", "ISO-8859-1" );
        assertEquals( "ISO-8859-1", getNestedMessage( NESTED_2_1 ).getSubjectCharset() );
    }

    @Test
    public void getHeader() throws Exception {

        String headerName = "To";
        int index = 0;

        assertEquals( "test53@automator.domain",
                      getNestedMessage( NESTED_1 ).getHeader( headerName, index ) );

        assertEquals( "test553@automator.domain",
                      getNestedMessage( NESTED_2_1 ).getHeader( headerName, index ) );

        assertEquals( "1284982177797",
                      getNestedMessage( NESTED_2_1 ).getHeader( "Automation-Message-Tag", index ) );
    }

    @Test
    public void getSender() throws Exception {

        getNestedMessage( NESTED_2_1 ).setSender( "test1@test.com" );
        getNestedMessage( NESTED_2_1 ).setSenderName( "test1" );
        assertEquals( "test1 <test1@test.com>", getNestedMessage( NESTED_2_1 ).getSender() );
    }

    @Test
    public void getSenderAddress() throws Exception {

        String sender = "test1@test.com";

        getNestedMessage( NESTED_2_1 ).setSender( sender );
        getNestedMessage( NESTED_2_1 ).setSenderName( "test1" );
        assertEquals( sender, getNestedMessage( NESTED_2_1 ).getSenderAddress() );
    }

    @Test
    public void getAttachmentPartCount() throws Exception {

        assertEquals( 0, getNestedMessage( NESTED_1 ).getAttachmentPartCount() );
        assertEquals( 2, getNestedMessage( NESTED_2 ).getAttachmentPartCount() );
        assertEquals( 0, getNestedMessage( NESTED_2_1 ).getAttachmentPartCount() );
    }

    @Test
    public void setNestedPathLevel() throws Exception {

        int initialNestedLevel = ActionLibraryConfigurator.getInstance().getMimePackageMaxNestedLevel();
        try {
            // allow 2 level nesting
            ActionLibraryConfigurator.getInstance().setMimePackageMaxNestedLevel( 2 );
            loadNestedMessage();

            // verify first and second level nested messages are loaded
            getNestedMessage( NESTED_1 );
            getNestedMessage( NESTED_2 );
            getNestedMessage( NESTED_3 );
            getNestedMessage( NESTED_2_1 );

            // allow 1 level nesting
            ActionLibraryConfigurator.getInstance().setMimePackageMaxNestedLevel( 1 );
            loadNestedMessage();

            // verify first level nested messages are loaded
            getNestedMessage( NESTED_1 );
            getNestedMessage( NESTED_2 );
            getNestedMessage( NESTED_3 );

            // second level nested messages must not be available, so error occurs
            try {
                getNestedMessage( NESTED_2_1 );
                assertTrue( "second level nested message has been loaded while it should not be", false );
            } catch( NoSuchMimePackageException e ) {
                // this is the expected behavior
            }
        } finally {
            ActionLibraryConfigurator.getInstance().setMimePackageMaxNestedLevel( initialNestedLevel );
        }
    }

    private MimePackage getNestedMessage(
                                          int[] packagePath ) throws NoSuchMimePackageException {

        return emailMessage.getNeededMimePackage( packagePath );
    }

    private void loadNestedMessage() throws PackageException, FileNotFoundException {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream( MAIL_MESSAGE_PATH );
            emailMessage = new MimePackage( fis );
        } finally {
            IoUtils.closeStream( fis );
        }
    }
}
