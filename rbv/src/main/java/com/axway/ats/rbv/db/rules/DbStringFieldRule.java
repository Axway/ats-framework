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

import java.util.regex.Pattern;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.db.DbEncryptor;
import com.axway.ats.rbv.db.DbMetaData;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.NoSuchMetaDataKeyException;
import com.axway.ats.rbv.model.RbvException;

public class DbStringFieldRule extends DbFieldsRule {

    public enum MatchRelation {
        EQUALS, CONTAINS, REGEX_MATCH
    }

    protected MatchRelation relation;

    // A custom encryption provider interface.
    private DbEncryptor     dbEncryptor;

    public DbStringFieldRule( String tableName,
                              String fieldName,
                              String expectedValue,
                              MatchRelation relation,
                              String ruleName,
                              boolean expectedResult ) {

        super(tableName, fieldName, expectedValue, ruleName, expectedResult);
        this.relation = relation;
    }

    public DbStringFieldRule( String fieldName,
                              String expectedValue,
                              MatchRelation relation,
                              String ruleName,
                              boolean expectedResult ) {

        super(fieldName, expectedValue, ruleName, expectedResult);
        this.relation = relation;
    }

    public void setDbEncryptor(
                                DbEncryptor dbEncryptor ) {

        this.dbEncryptor = dbEncryptor;
    }

    @Override
    protected boolean performMatch(
                                    MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        //this cast is safe, as isMatch has already checked the type of meta data
        DbMetaData dbMetaData = (DbMetaData) metaData;

        String actualValue;
        try {
            actualValue = (String) dbMetaData.getProperty(expectedMetaDataKey.toString());
            if (dbEncryptor != null) {
                actualValue = dbEncryptor.decrypt(actualValue);
            }
            log.info("Actual value is '" + actualValue + "'");
        } catch (NoSuchMetaDataKeyException nsmdke) {
            log.warn(nsmdke.getMessage());
            return false;
        } catch (ClassCastException cce) {
            throw new MetaDataIncorrectException("Meta data is incorrect - expected String");
        }

        if (expectedValue == null) {
            return actualValue == null;
        } else {
            if (actualValue == null) {
                return false;
            }
        }

        if (relation == MatchRelation.EQUALS) {
            actualResult = expectedValue.equals(actualValue);
        } else if (relation == MatchRelation.CONTAINS) {
            actualResult = actualValue.contains((String) expectedValue);
        } else if (relation == MatchRelation.REGEX_MATCH) {
            Pattern regexPattern = Pattern.compile((String) expectedValue);
            actualResult = regexPattern.matcher(actualValue).matches();
        } else {
            throw new RbvException("No implementation for MatchRelation '" + relation.toString()
                                   + "' in DbStringFieldMatchingTerm");
        }

        return actualResult;
    }

    @Override
    public String getRuleDescription() {

        StringBuilder ruleDescription = new StringBuilder();

        ruleDescription.append("on table '" + expectedMetaDataKey.getTableName() + "',");
        ruleDescription.append(" column '" + expectedMetaDataKey.getColumnName() + "',");
        if (!getExpectedResult()) {
            ruleDescription.append(" not");
        }
        ruleDescription.append(" expected value '" + expectedValue + "',");
        ruleDescription.append(" with match relation '" + relation.toString() + "'");

        return ruleDescription.toString();
    }
}
