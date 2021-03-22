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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimePart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.imap.ImapMetaData;
import com.axway.ats.rbv.model.RbvException;

public class StringInMimePartRule extends AbstractImapRule {

    private static Logger       log               = LogManager.getLogger(StringInMimePartRule.class);

    private String              expectedValue;
    private boolean             isValueRegularExpression;
    private int                 partIndex;
    private boolean             isPartAttachment;

    private static final int    PART_MAIN_MESSAGE = -1;
    private static final String TEXT_MIME_TYPE_LC = "text";                                      // lower case

    public StringInMimePartRule( String expectedValue,
                                 boolean isValueRegularExpression,
                                 String ruleName,
                                 boolean expectedResult ) {

        this(new int[0],
             expectedValue,
             isValueRegularExpression,
             PART_MAIN_MESSAGE,
             false,
             ruleName,
             expectedResult);
    }

    public StringInMimePartRule( String expectedValue,
                                 boolean isValueRegularExpression,
                                 int partIndex,
                                 boolean isPartAttachment,
                                 String ruleName,
                                 boolean expectedResult ) {

        this(new int[0],
             expectedValue,
             isValueRegularExpression,
             partIndex,
             isPartAttachment,
             ruleName,
             expectedResult);
    }

    public StringInMimePartRule( int[] nestedPackagePath,
                                 String expectedValue,
                                 boolean isValueRegularExpression,
                                 String ruleName,
                                 boolean expectedResult ) {

        this(nestedPackagePath,
             expectedValue,
             isValueRegularExpression,
             PART_MAIN_MESSAGE,
             false,
             ruleName,
             expectedResult);
    }

    public StringInMimePartRule( int[] nestedPackagePath,
                                 String expectedValue,
                                 boolean isValueRegularExpression,
                                 int partIndex,
                                 boolean isPartAttachment,
                                 String ruleName,
                                 boolean expectedResult ) {

        super(ruleName, expectedResult, ImapMetaData.class);

        this.expectedValue = expectedValue;
        this.isValueRegularExpression = isValueRegularExpression;
        this.partIndex = partIndex;
        this.isPartAttachment = isPartAttachment;

        setNestedPackagePath(nestedPackagePath);
    }

    @Override
    public boolean performMatch(
                                 MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        //get the emailMessage
        //the meta data type check already passed, so it is safe to cast
        MimePackage emailMessage = getNeededMimePackage(metaData);

        InputStream actualPartDataStream = null;
        BufferedInputStream bufferedStream = null;
        try {
            List<MimePart> partsToCheck = new ArrayList<MimePart>();
            if (partIndex == PART_MAIN_MESSAGE) {
                partsToCheck = emailMessage.getMimeParts();
            } else {
                partsToCheck.add(emailMessage.getPart(partIndex, isPartAttachment));
            }

            for (MimePart partToCheck : partsToCheck) {
                Object partContent = partToCheck.getContent();
                ContentType partContentType = new ContentType(partToCheck.getContentType());

                //skip if no content
                if (partContent == null) {
                    log.debug("MIME part does not have any content");
                    continue;
                }

                String partContentAsString;
                if (partContent instanceof String) {
                    //directly read the content of the part
                    partContentAsString = (String) partContent;
                } else if (partContent instanceof InputStream
                           && partContentType.getBaseType().toLowerCase().startsWith(TEXT_MIME_TYPE_LC)) {

                    actualPartDataStream = (InputStream) partContent; // to be closed in finally
                    //get the charset of the part - default to us-ascii
                    String charset = partContentType.getParameter("charset");
                    if (charset == null) {
                        charset = "us-ascii";
                    }

                    //read stream by large chunks to minimize memory fragmentation
                    int bufLen = 4096;
                    byte[] buffer = new byte[bufLen];
                    StringBuffer dataStringBuffer = new StringBuffer();
                    bufferedStream = new BufferedInputStream(actualPartDataStream);

                    int numBytesRead = bufLen;
                    while (numBytesRead == bufLen) {
                        numBytesRead = bufferedStream.read(buffer, 0, bufLen);

                        if (numBytesRead != -1) {
                            dataStringBuffer.append(new String(buffer, 0, numBytesRead, charset));
                        } else {
                            //we've reached end of stream
                            break;
                        }
                    }

                    partContentAsString = dataStringBuffer.toString();
                } else {
                    log.debug("Skipping MIME part as it is binary");
                    continue;
                }

                if (isValueRegularExpression) {
                    actualResult = Pattern.compile(expectedValue).matcher(partContentAsString).find();
                } else {
                    actualResult = partContentAsString.indexOf(expectedValue) >= 0;
                }

                //if actual result is true, we don't need to
                //continue anymore
                if (actualResult) {
                    break;
                }
            }

            return actualResult;
        } catch (MessagingException me) {
            throw new RbvException(me);
        } catch (PackageException pe) {
            throw new RbvException(pe);
        } catch (IOException ioe) {
            throw new RbvException(ioe);
        } finally {
            IoUtils.closeStream(actualPartDataStream);
            IoUtils.closeStream(bufferedStream);
        }
    }

    @Override
    protected String getRuleDescription() {

        return "which expects the string '" + expectedValue + "' in MIME part at position " + partIndex
               + " (" + (isPartAttachment
                                          ? "attachment"
                                          : "regular")
               + ")";
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(ImapMetaData.MIME_PACKAGE);
        return metaKeys;
    }
}
