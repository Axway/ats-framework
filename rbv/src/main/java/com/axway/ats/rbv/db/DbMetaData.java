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
package com.axway.ats.rbv.db;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;

public class DbMetaData extends MetaData {

    /**
     * Put a property in the meta data - the property
     * here is a value in a db record
     * 
     * @param dbColumn  the key, which is the representation of the DB column
     * @param value     the value of the DB field
     */
    public void putProperty(
                             DbMetaDataKey dbMetaDataKey,
                             Object value ) throws RbvException {

        putProperty( dbMetaDataKey.toString(), value );
    }

    /**
     * Get a property from the meta data - the property
     * here is a value in a db record
     * 
     * @param dbColumn  the key, which is the representation of the DB column
     * @return
     */
    public Object getProperty(
                               DbMetaDataKey dbMetaDataKey ) throws RbvException {

        return getProperty( dbMetaDataKey.toString() );
    }
}
