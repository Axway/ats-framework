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

import com.axway.ats.action.objects.MimePackage;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

public class ImapMetaData extends MetaData {

    /** MIME_PACKAGE {@link MetaData} descriptor */
    public static final String MIME_PACKAGE = "MIME_PACKAGE";

    public ImapMetaData( MimePackage mimePackage ) throws RbvException {

        super();

        putProperty(MIME_PACKAGE, mimePackage);
    }

    public MimePackage getMimePackage() throws RbvException {

        MimePackage mimePackage = null;
        try {
            mimePackage = (MimePackage) getProperty(MIME_PACKAGE);
        } catch (ClassCastException cce) {
            throw new MetaDataIncorrectException("Mime message is not of correct type");
        }

        if (mimePackage == null) {
            throw new MetaDataIncorrectException("Meta data does not include mime message");
        }

        return mimePackage;
    }
}
