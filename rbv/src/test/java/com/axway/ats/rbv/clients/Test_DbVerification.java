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
package com.axway.ats.rbv.clients;

import java.util.Calendar;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.core.dbaccess.DbProvider;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.clients.DbVerification;
import com.axway.ats.rbv.db.DbSearchTerm;
import com.axway.ats.rbv.db.MockDbEncryptor;
import com.axway.ats.rbv.db.MockDbProvider;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvVerificationException;

public class Test_DbVerification extends BaseTest {

    private static DbProvider provider;

    @BeforeClass
    public static void setUpTest_DbVerification() {

        provider = new MockDbProvider();
    }

    @Test
    public void clearRulesTest() throws RbvException {

        DbVerification dbVerification = new DbVerification(new DbSearchTerm(""), provider);
        try {
            //adding rule with wrong value and expecting RBVException exception 
            dbVerification.checkFieldValueEquals("", "key1", "wrongValue");
            dbVerification.verifyDbDataExists();
            Assert.fail();
        } catch (RbvException e) {
            //this is the expected behavior
        }

        //clear all rules
        dbVerification.clearRules();
        //adding rule with existing value
        dbVerification.checkFieldValueEquals("", "key1", "value00");
        dbVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueEqualsStringPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueEquals("", "key1", "value10");
        fsVerification.verifyDbDataExists();
    }

    @Test( expected = RbvVerificationException.class)
    public void checkFieldValueEqualsStringNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueEquals("", "key1", "value");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueDoesNotEqualStringPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueDoesNotEqual("", "key1", "value");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueDoesNotEqualStringNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueDoesNotEqual("", "key1", "value1");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueEqualsEncryptedStringPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        // the mock encryptor changes all letters to capital case 
        fsVerification.setDbEncryptor(new MockDbEncryptor());
        fsVerification.checkFieldValueEquals("", "key1", "VALUE00");
        fsVerification.verifyDbDataExists();
    }

    @Test( expected = RbvVerificationException.class)
    public void checkFieldValueEqualsEncryptedStringNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        // the mock encryptor changes all letters to capital case 
        fsVerification.setDbEncryptor(new MockDbEncryptor());
        fsVerification.checkFieldValueEquals("", "key1", "VaLUE00");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkDencryptorIsUnsetPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueEquals("asd", "key0", "value00");

        // the mock encryptor changes all letters to capital case
        fsVerification.setDbEncryptor(new MockDbEncryptor());
        fsVerification.checkFieldValueEquals("", "key1", "VALUE00");
        // remove the encryptor
        fsVerification.setDbEncryptor(null);

        fsVerification.checkFieldValueEquals("", "key2", "value00");
        fsVerification.verifyDbDataExists();
    }

    @Test( expected = RbvVerificationException.class)
    public void checkDencryptorIsUnsetNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueEquals("asd", "key0", "value00");

        // the mock encryptor changes all letters to capital case
        fsVerification.setDbEncryptor(new MockDbEncryptor());
        fsVerification.checkFieldValueEquals("", "key1", "VALUE00");
        // did not remove the encryptor, this will result in error in the next check

        fsVerification.checkFieldValueEquals("", "key2", "value00");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueEqualsNumericPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueEquals("numeric", "key", 0);
        fsVerification.verifyDbDataExists();
    }

    @Test( expected = RbvVerificationException.class)
    public void checkFieldValueEqualsNumericNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueEquals("numeric", "key", 23);
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueDoesNotEqualNumericPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueDoesNotEqual("numeric", "key", 23);
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueDoesNotEqualNumericNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueDoesNotEqual("numeric", "key", 0);
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueEqualsBinaryPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueEquals("binary", "key", new byte[]{ 1, 2, 0 });
        fsVerification.verifyDbDataExists();
    }

    @Test( expected = RbvVerificationException.class)
    public void checkFieldValueEqualsBinaryNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueEquals("binary", "key", new byte[]{ 1 });
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueDoesNotEqualBinaryPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueDoesNotEqual("binary", "key", new byte[]{ 1, 2, 23 });
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueDoesNotEqualBinaryNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueDoesNotEqual("binary", "key", new byte[]{ 1, 2, 0 });
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueRegexPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueRegex("", "key1", "val.*");
        fsVerification.verifyDbDataExists();
    }

    @Test( expected = RbvVerificationException.class)
    public void checkFieldValueRegexNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueRegex("", "key1", "val1.*");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueRegexDoesNotMatchPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueRegexDoesNotMatch("", "key1", "val1.*");
        fsVerification.verifyDbDataExists();
    }

    @Test( expected = RbvVerificationException.class)
    public void checkFieldValueRegexDoesNotMatchNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueRegexDoesNotMatch("", "key1", "val.*");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueContainsPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueContains("", "key1", "val");
        fsVerification.verifyDbDataExists();
    }

    @Test( expected = RbvVerificationException.class)
    public void checkFieldValueContainsNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueContains("", "key1", "val1");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueDoesNotContainPositive() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueDoesNotContain("", "key1", "val1");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueDoesNotContainNegative() throws RbvException {

        DbVerification fsVerification = new DbVerification(new DbSearchTerm(""), provider);

        fsVerification.checkFieldValueDoesNotContain("", "key1", "val1");
        fsVerification.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueBooleanPositive() throws RbvException {

        DbVerification verificator = new DbVerification(new DbSearchTerm(""), provider);

        verificator.checkFieldValueEquals("boolean", "keyNumber", true);
        verificator.checkFieldValueEquals("boolean", "keyString", false);
        verificator.verifyDbDataExists();
    }

    @Test
    public void checkFieldValueDatePositive() throws RbvException {

        DbVerification verificator = new DbVerification(new DbSearchTerm(""), provider);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        verificator.checkFieldValueEquals("Date", "today", calendar.getTime());
        verificator.verifyDbDataExists();
    }
}
