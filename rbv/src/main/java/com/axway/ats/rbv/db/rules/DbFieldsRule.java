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

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.rbv.db.DbMetaData;
import com.axway.ats.rbv.db.DbMetaDataKey;
import com.axway.ats.rbv.rules.AbstractRule;

public abstract class DbFieldsRule extends AbstractRule {

    protected DbMetaDataKey expectedMetaDataKey;
    protected Object        expectedValue;

    protected DbFieldsRule( String tableName,
                            String fieldName,
                            Object expectedValue,
                            String ruleName,
                            boolean expectedResult ) {

        super( ruleName, expectedResult, DbMetaData.class );
        this.expectedMetaDataKey = new DbMetaDataKey( tableName, fieldName );
        this.expectedValue = expectedValue;
    }

    protected DbFieldsRule( String fieldName,
                            Object expectedValue,
                            String ruleName,
                            boolean expectedResult ) {

        super( ruleName, expectedResult, DbMetaData.class );
        this.expectedMetaDataKey = new DbMetaDataKey( fieldName, 0 );
        this.expectedValue = expectedValue;
    }

    /**
     * @return returns a {@link List} of {@link String} values, representing the
     *         keys of each single unit of data that the rule, or any other nested
     *         in it rules, verifies
     */
    public final List<String> getMetaDataKeys() {

        List<String> keys = new ArrayList<String>();
        keys.add( this.expectedMetaDataKey.toString() );
        return keys;
    }

    @Override
    protected String getRuleDescription() {

        StringBuilder ruleDescription = new StringBuilder();

        ruleDescription.append( "on table '" + expectedMetaDataKey.getTableName() + "'," );
        ruleDescription.append( " column '" + expectedMetaDataKey.getColumnName() + "', " );
        ruleDescription.append( " expected value '" + expectedValue + "'" );

        return ruleDescription.toString();
    }
}
