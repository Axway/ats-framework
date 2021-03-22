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

/**
 * Matches a field in the database that is supposed to be containing 
 * a {@link Boolean} value to a value provided by the user at the time
 * the rule is created.
 */
public class DbBooleanFieldRule extends DbFieldsRule {

    private static final Logger log = LogManager.getLogger(DbBooleanFieldRule.class);

    /**
     * Constructor 
     * 
     * @param tableName
     * @param fieldName
     * @param expectedValue
     * @param ruleName
     * @param expectedResult
     */
    public DbBooleanFieldRule( String tableName,
                               String fieldName,
                               boolean expectedValue,
                               String ruleName,
                               boolean expectedResult ) {

        super(tableName, fieldName, expectedValue, ruleName, expectedResult);
    }

    /**
     * Constructor
     * 
     * @param fieldName
     * @param expectedValue
     * @param ruleName
     * @param expectedResult
     */
    public DbBooleanFieldRule( String fieldName,
                               Boolean expectedValue,
                               String ruleName,
                               boolean expectedResult ) {

        super(fieldName, expectedValue, ruleName, expectedResult);
    }

    @Override
    protected boolean performMatch(
                                    MetaData metaData ) throws RbvException {

        //this cast is safe, as isMatch has already checked the type of meta data
        DbMetaData dbMetaData = (DbMetaData) metaData;

        Boolean actual = null;
        Object actualValue = dbMetaData.getProperty(this.expectedMetaDataKey.toString());

        // check for null        
        if (this.expectedValue == null) {
            if (actualValue == null) {
                return true;
            }

            return false;
        }
        if (actualValue == null) {
            return false;
        }

        // check if the value is of type String
        if (actualValue instanceof String) {
            if ("0".equals(actualValue)) {
                actual = false;
            } else if ("1".equals(actualValue)) {
                actual = true;
            } else {
                throw new MetaDataIncorrectException("Meta data is incorrect. Received a String containing : "
                                                     + actualValue);
            }
        }

        // check if value is of type Number
        if (actualValue instanceof Number) {
            Number actualNumber = (Number) actualValue;

            if (actualNumber.byteValue() != actualNumber.doubleValue()) {
                // this would mean that the value has floating point digits which is wrong
                throw new MetaDataIncorrectException("Meta data is incorrect. Received a Number containing floating point digits: "
                                                     + actualValue);
            }

            if (actualNumber.byteValue() == 0) {
                actual = false;
            } else if (actualNumber.byteValue() == 1) {
                actual = true;
            } else {
                throw new MetaDataIncorrectException("Meta data is incorrect. Received a Number containing : "
                                                     + actualValue);
            }
        }

        // check if value is of type Number
        if (actualValue instanceof Boolean) {
            actual = (Boolean) actualValue;
        }

        boolean expected = ((Boolean) this.expectedValue).booleanValue();

        if (actual == null) {
            throw new MetaDataIncorrectException("Meta data is incorrect. Unexpected type of the value in the database : "
                                                 + actualValue.getClass().getCanonicalName());
        }

        if (expected == actual) {
            return true;
        }

        return false;
    }
}
