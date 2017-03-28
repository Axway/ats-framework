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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.mail.internet.MimePart;

import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.PackageLoader;
import com.axway.ats.action.objects.model.PackageHeader;

/**
 * Test parsing of mails with text/rfc822-headers content type
 */
public class Test_Rfc822Headers extends BaseTest {

    /**
     * Test is parsing mail with no nested parts. It get the plain/text part on level 1.
     *  
     * - multipart/mixed;
     *   - text/plain
     *   - application/octet-stream
     *   - message/rfc822
     */
    @Test
    public void parse1Reg2Att() throws Exception {

        String mailMessagePath = Test_MimePackage.class.getResource( "RFC822-headers-1_Regular_2_Attachments.eml" )
                                                       .getPath();

        MimePackage mimeMessage = PackageLoader.loadMimePackageFromFile( mailMessagePath );
        assertEquals( 1, mimeMessage.getRegularPartCount() );
        assertEquals( 1, mimeMessage.getAttachmentPartCount() );

        // first attachment exists and is parsed
        MimePart part = mimeMessage.getPart( 0, true );
        assertTrue( part != null );

        // one regular part with text
        assertTrue( mimeMessage.getPlainTextBody()
                               .startsWith( "This report relates to a message you sent" ) );

        // nested MimePackage - the RFC822-headers
        MimePackage nestedPackWithHeadersOnly = mimeMessage.getNeededMimePackage( new int[]{ 0 } );
        List<PackageHeader> headers = nestedPackWithHeadersOnly.getAllHeaders();
        assertTrue( headers.size() == 31 );

        /* For test debugging
        int i = 0;
        for( PackageHeader packageHeader : headers ) {
            log.info("header[" + i + "] name: [" +  packageHeader.getName()
                               +"] value: [" +  packageHeader.getValue() + "]");
            i++;
        }
        */
    }

    /**
     * Test getting text body (plain or html) where text/plain part is located on 2nd level:
     * 
     * - multipart/mixed;
     *   - multipart/alternative;
     *     - text/plain
     *     - text/html
     *   - text/rfc822-headers
     */
    @Test
    public void parse2Reg0Att() throws Exception {

        String mailMessagePath = Test_MimePackage.class.getResource( "RFC822-headers-Mailgate-Notification_2_Regular_0_Attachments.eml" )
                                                       .getPath();

        MimePackage mimeMessage = PackageLoader.loadMimePackageFromFile( mailMessagePath );
        assertEquals( 2, mimeMessage.getRegularPartCount() );
        assertEquals( 0, mimeMessage.getAttachmentPartCount() );

        assertNotNull( mimeMessage.getPlainTextBody() );
        assertNotNull( mimeMessage.getHtmlTextBody() );
    }

    /**
     * Test getting text body (plain or html) not possible, because text/plain part is located on 3nd level,
     *  and on 2nd level are located unknown parts:
     *  
     * - multipart/mixed;
     *    - multipart/mixed;
     *      - multipart/alternative;
     *          - text/plain
     *          - text/html
     *    - multipart/alternative;
     *          - text/myPlain
     *          - text/myHTML
     */
    @Test
    public void parseNestedNoBody() throws Exception {

        String mailMessagePath = Test_MimePackage.class.getResource( "nestedMessagesNoBody.eml" ).getPath();

        MimePackage mimeMessage = PackageLoader.loadMimePackageFromFile( mailMessagePath );
        assertEquals( 2, mimeMessage.getRegularPartCount() );
        assertEquals( 0, mimeMessage.getAttachmentPartCount() );

        assertNull( mimeMessage.getPlainTextBody() ); // exists but nested 3 levels.
        assertNull( mimeMessage.getHtmlTextBody() ); // exists but nested 3 levels.
    }

    /**
     * Test getting text body (plain or html) where text/plain part is missing and text/html on 2nd level:
     * 
     * - multipart/mixed;
     *   - multipart/alternative;
     *     - text/html
     *   - text/rfc822-headers
     */
    @Test
    public void parseNestedNoPlain() throws Exception {

        String mailMessagePath = Test_MimePackage.class.getResource( "nestedMessagesNoPlain.eml" ).getPath();

        MimePackage mimeMessage = PackageLoader.loadMimePackageFromFile( mailMessagePath );
        assertEquals( 1, mimeMessage.getRegularPartCount() );
        assertEquals( 0, mimeMessage.getAttachmentPartCount() );

        assertNull( mimeMessage.getPlainTextBody() ); // is missing
        assertNotNull( mimeMessage.getHtmlTextBody() ); // exist on level 2 and it will be parsed.
    }

    /**
     * Test is parsing mail with nested parts, the part 'multipart/alternative' is located before 'text/plain'
     * to test that level 1 in the mail is parsed first.
     *  
     * - multipart/mixed;
     *      - multipart/alternative;
     *          - text/plain
     *          - text/html
     *      - message/rfc822
     *      - text/plain
     */
    @Test
    public void getTextBody_HTMLBodyIsLocatedAfterAlternativePart() throws Exception {

        String mailMessagePath = Test_MimePackage.class.getResource( "textBody_HTMLBodyIsLocatedAfterAlternativePart.eml" )
                                                       .getPath();

        MimePackage mimeMessage = PackageLoader.loadMimePackageFromFile( mailMessagePath );
        assertEquals( 3, mimeMessage.getRegularPartCount() );
        assertEquals( 1, mimeMessage.getAttachmentPartCount() );

        assertEquals( "plain text on 1st level", mimeMessage.getPlainTextBody().trim() );
        assertEquals( true, mimeMessage.getHtmlTextBody().contains( "test2" ) );
    }
}
