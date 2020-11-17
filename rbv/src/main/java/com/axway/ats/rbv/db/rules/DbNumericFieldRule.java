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
package com.axway.ats.rbv.db.rules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.db.DbMetaData;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

public class DbNumericFieldRule extends DbFieldsRule {

    private static final Logger log = LogManager.getLogger(DbNumericFieldRule.class);

    public DbNumericFieldRule( String tableName,
                               String fieldName,
                               Number expectedValue,
                               String ruleName,
                               boolean expectedResult ) {

        super(tableName, fieldName, expectedValue, ruleName, expectedResult);
    }

    public DbNumericFieldRule( String fieldName,
                               Number expectedValue,
                               String ruleName,
                               boolean expectedResult ) {

        super(fieldName, expectedValue, ruleName, expectedResult);
    }

    @Override
    protected boolean performMatch(
                                    MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        //this cast is safe, as isMatch has already checked the type of meta data            
        DbMetaData dbMetaData = (DbMetaData) metaData;

        Number actualValue;
        try {
            actualValue = (Number) dbMetaData.getProperty(expectedMetaDataKey.toString());
            log.info("Actual value is '" + actualValue + "'");
        } catch (ClassCastException cce) {
            throw new MetaDataIncorrectException("Meta data is incorrect - expected Number");
        }

        if (expectedValue == null) {
            return actualValue == null;
        } else {
            if (actualValue == null) {
                return false;
            }
        }

        if (!expectedValue.getClass().getName().equals(actualValue.getClass().getName())) {
            log.info("Type of expected value '" + expectedValue.getClass().getSimpleName()
                     + "' is different than type of actual value '" + actualValue.getClass().getSimpleName()
                     + "'");
            return false;
        }

        if (expectedValue.equals(actualValue)) {
            actualResult = true;
        }

        return actualResult;
    }
}
