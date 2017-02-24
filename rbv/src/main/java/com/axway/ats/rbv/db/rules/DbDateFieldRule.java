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

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.db.DbMetaData;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

/**
 * Matches the {@link Date} specified to the {@link Date} in the database
 */
public class DbDateFieldRule extends DbFieldsRule {

    /**
     * Specifies the match relation of the two {@link Date} fields
     */
    public enum MatchRelation {
        /** Before the date specified */
        BEFORE_DATE,
        /** After the date specified */
        AFTER_DATE,
        /** {@link Timestamp} match of the date specified */
        EXACT
    }

    private String        actualValuePattern;
    private MatchRelation relation;

    /**
     * Constructor
     * 
     * @param tableName
     * @param fieldName
     * @param expectedValue
     * @param relation
     * @param actualValuePattern
     * @param ruleName
     * @param expectedResult
     */
    public DbDateFieldRule( String tableName,
                            String fieldName,
                            String expectedValue,
                            MatchRelation relation,
                            String actualValuePattern,
                            String ruleName,
                            boolean expectedResult ) {

        super( tableName, fieldName, expectedValue, ruleName, expectedResult );

        this.actualValuePattern = actualValuePattern;
        this.relation = relation;
    }

    /**
     * Constructor
     * 
     * @param fieldName
     * @param expectedValue
     * @param relation
     * @param actualValuePattern
     * @param ruleName
     * @param expectedResult
     */
    public DbDateFieldRule( String fieldName,
                            String expectedValue,
                            MatchRelation relation,
                            String actualValuePattern,
                            String ruleName,
                            boolean expectedResult ) {

        super( fieldName, expectedValue, ruleName, expectedResult );

        this.actualValuePattern = actualValuePattern;
        this.relation = relation;
    }

    /**
     * Constructor
     * 
     * @param tableName
     * @param fieldName
     * @param expectedValue
     * @param relation
     * @param ruleName
     * @param expectedResult
     */
    public DbDateFieldRule( String tableName,
                            String fieldName,
                            Date expectedValue,
                            MatchRelation relation,
                            String ruleName,
                            boolean expectedResult ) {

        super( tableName, fieldName, expectedValue, ruleName, expectedResult );

        this.relation = relation;
    }

    /**
     * Constructor
     * 
     * @param fieldName
     * @param expectedValue
     * @param relation
     * @param ruleName
     * @param expectedResult
     */
    public DbDateFieldRule( String fieldName,
                            Date expectedValue,
                            MatchRelation relation,
                            String ruleName,
                            boolean expectedResult ) {

        super( fieldName, expectedValue, ruleName, expectedResult );

        this.relation = relation;
    }

    @Override
    protected boolean performMatch(
                                    MetaData metaData ) throws RbvException {

        //this cast is safe, as isMatch has already checked the type of meta data            
        DbMetaData dbMetaData = ( DbMetaData ) metaData;

        Object actualValue = dbMetaData.getProperty( this.expectedMetaDataKey.toString() );

        // check for null
        if( this.expectedValue == null ) {
            if( actualValue == null ) {
                return true;
            }
            return false;
        }
        // expected value is not null, but if actual is null then we have a negative match
        if( actualValue == null ) {
            return false;
        }

        if( expectedValue instanceof String ) {
            return checkString( dbMetaData );
        }  
        return checkDate( dbMetaData );

    }

    private boolean checkString(
                                 DbMetaData dbMetaData ) throws RbvException {

        String actualValue;
        try {
            actualValue = ( String ) dbMetaData.getProperty( this.expectedMetaDataKey.toString() );
        } catch( ClassCastException cce ) {
            throw new MetaDataIncorrectException( "Meta data is incorrect - expected String" );
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat( this.actualValuePattern );
            sdf.setTimeZone( new SimpleTimeZone( 0, "GMT" ) );

            Date expectedDate = new Date( Long.parseLong( ( String ) this.expectedValue ) * 1000 );
            Date actualDate = sdf.parse( actualValue );

            switch( this.relation ){
                case BEFORE_DATE: return expectedDate.before( actualDate );
                case AFTER_DATE: return expectedDate.after( actualDate );
                case EXACT: return expectedDate.compareTo( actualDate ) == 0;
                default:
                    throw new RbvException( "No implementation for MatchRelation '" + this.relation.toString()
                                            + "' in DbDateFieldMatchingTerm" );
            }

        } catch( NumberFormatException nfe ) {
            throw new RbvException( "Expected value '" + this.expectedValue
                                    + "' cannot be converted to UNIX timestamp" );
        } catch( ParseException pe ) {
            //we've already checked the expected value in isValid(), so this exception can be thrown
            //only if the actual value is incorrect
            throw new RbvException( "Actual value '" + actualValue + "' cannot be converted to Date" );
        }
    }

    private boolean checkDate(
                               DbMetaData dbMetaData ) throws RbvException {

        Timestamp actual;
        try {
            actual = ( Timestamp ) dbMetaData.getProperty( this.expectedMetaDataKey );
        } catch( ClassCastException cce ) {
            throw new MetaDataIncorrectException( "Meta data is incorrect - expected Timestamp" );
        }
        long timestamp = ( ( Date ) this.expectedValue ).getTime();
        Timestamp expected = new Timestamp( timestamp );

        switch( this.relation ){
            case BEFORE_DATE: return actual.compareTo( expected ) <= 0;
            case AFTER_DATE: return actual.compareTo( expected ) >= 0;
            case EXACT: return actual.compareTo( expected ) == 0;
            default:
                throw new RbvException( "No implementation for MatchRelation '" + this.relation.toString()
                                        + "' in DbDateFieldMatchingTerm" );
        }
    }

    @Override
    public String getRuleDescription() {

        StringBuilder ruleDescription = new StringBuilder();

        ruleDescription.append( "on table '" + this.expectedMetaDataKey.getTableName() + "'," );
        ruleDescription.append( " column '" + this.expectedMetaDataKey.getColumnName() + "', " );
        ruleDescription.append( " expected value '" + this.expectedValue + "', " );
        ruleDescription.append( " with match relation '" + this.relation.toString() + "'" );

        return ruleDescription.toString();
    }
}
