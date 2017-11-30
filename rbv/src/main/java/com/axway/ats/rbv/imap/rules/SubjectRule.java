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
package com.axway.ats.rbv.imap.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.model.RbvException;

public class SubjectRule extends AbstractImapRule {

    public enum SubjectMatchMode {
        /**
         * Search for the string at the beginning of the header
         */
        LEFT,
        /**
         *  Search for the string at the end of the header
         */
        RIGHT,
        /**
        * Search for the string anywhere in the header
        */
        FIND,
        /**
         * Search for the string anywhere in the header
         */
        EQUALS,
        /**
        * Match the header using a regular expression
        */
        REGEX;
    }

    private String           expectedValue;
    private SubjectMatchMode matchMode;

    public SubjectRule( String expectedValue,
                        SubjectMatchMode matchMode,
                        String ruleName,
                        boolean expectedResult ) {

        this(new int[0], expectedValue, matchMode, ruleName, expectedResult);
    }

    public SubjectRule( int[] nestedPackagePath,
                        String expectedValue,
                        SubjectMatchMode matchMode,
                        String ruleName,
                        boolean expectedResult ) {

        super(ruleName, expectedResult, ImapMetaData.class);

        this.expectedValue = expectedValue;
        this.matchMode = matchMode;

        setNestedPackagePath(nestedPackagePath);
    }

    @Override
    protected boolean performMatch(
                                    MetaData metaData ) throws RbvException {

        //get the emailMessage
        //the meta data type check already passed, so it is safe to cast
        MimePackage emailMessage = getNeededMimePackage(metaData);

        //get the actual subject value
        String subjectValue;
        try {
            subjectValue = emailMessage.getSubject();
        } catch (PackageException pe) {
            throw new RbvException(pe);
        }

        //if there is no such header return false 
        boolean actualResult = false;

        if (subjectValue != null) {
            switch (matchMode) {
                case LEFT:
                    actualResult = subjectValue.startsWith(expectedValue);
                    break;
                case RIGHT:
                    actualResult = subjectValue.endsWith(expectedValue);
                    break;
                case EQUALS:
                    actualResult = subjectValue.equals(expectedValue);
                    break;
                case FIND:
                    actualResult = subjectValue.indexOf(expectedValue) >= 0;
                    break;
                case REGEX:
                    actualResult = Pattern.compile(expectedValue).matcher(subjectValue).find();
                    break;
            }
            log.info("Actual subject is '" + subjectValue + "'");
        } else {
            log.info("No subject was found");
        }

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        return "which expects subject '" + expectedValue + "'";
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(ImapMetaData.MIME_PACKAGE);
        return metaKeys;
    }
}
