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
package com.axway.ats.rbv.imap;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.imap.ImapFolderSearchTerm;
import com.axway.ats.rbv.imap.ImapStorage;
import com.axway.ats.rbv.model.RbvException;

public class Test_ImapStorage extends BaseTest {

    private static String imapServer;
    private static String imapPass;
    private static String imapInboxFolder;
    private static String user = "test100";

    @BeforeClass
    public static void setUpTest_ImapStorage() throws IOException {

        Properties imapProps = new Properties();
        imapProps.load(Test_ImapStorage.class.getResourceAsStream("imapConfig.txt"));

        imapServer = imapProps.getProperty("imapServer");
        imapPass = imapProps.getProperty("imapPass");
        imapInboxFolder = imapProps.getProperty("imapInboxFolder");
    }

    @Test
    public void getFolder() throws RbvException {

        ImapStorage storage = new ImapStorage(imapServer);
        assertNotNull(storage.getFolder(new ImapFolderSearchTerm(user, imapPass)));
    }

    @Test
    public void getFolderByName() throws RbvException {

        ImapStorage storage = new ImapStorage(imapServer);
        assertNotNull(storage.getFolder(new ImapFolderSearchTerm(user, imapPass, imapInboxFolder)));
    }
}
