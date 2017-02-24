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
package com.axway.ats.core.dbaccess.description;

import java.util.ArrayList;

public class DbDataColumnDescribtion {
    private String fieldName;

    private String type;

    private String isNull;

    private String key;

    private String defaultValue;

    private String extra;

    public DbDataColumnDescribtion( String fieldName,
                                    String type,
                                    String isNull,
                                    String key,
                                    String defaultValue,
                                    String extra ) {

        setFieldName( fieldName );
        setType( type );
        setIsNull( isNull );
        setKey( key );
        setDefaultValue( defaultValue );
        setExtra( extra );
    }

    public DbDataColumnDescribtion() {

        setFieldName( "" );
        setType( "" );
        setIsNull( "" );
        setKey( "" );
        setDefaultValue( "" );
        setExtra( "" );
    }

    public boolean equals(
                           DbDataColumnDescribtion column ) {

        boolean result = true;
        if( !column.getFieldName().equals( getFieldName() ) || !column.getType().equals( getType() )
            || !column.getIsNull().equals( getIsNull() ) || !column.getKey().equals( getKey() )
            || !column.getDefaultValue().equals( getDefaultValue() )
            || !column.getExtra().equals( getExtra() ) ) {
            result = false;
        }
        return result;
    }

    public ArrayList<String[]> getDifferences(
                                               DbDataColumnDescribtion column ) {

        ArrayList<String[]> differenFields = new ArrayList<String[]>();
        if( !column.getFieldName().equals( getFieldName() ) ) {
            differenFields.add( new String[]{ "Field", getFieldName(), column.getFieldName() } );
        }
        if( !column.getType().equals( getType() ) ) {
            differenFields.add( new String[]{ "Type", getType(), column.getType() } );
        }
        if( !column.getIsNull().equals( getIsNull() ) ) {
            differenFields.add( new String[]{ "Null", getIsNull(), column.getIsNull() } );
        }
        if( !column.getKey().equals( getKey() ) ) {
            differenFields.add( new String[]{ "Key", getKey(), column.getKey() } );
        }
        if( !column.getDefaultValue().equals( getDefaultValue() ) ) {
            differenFields.add( new String[]{ "Default", getDefaultValue(), column.getDefaultValue() } );
        }
        if( !column.getExtra().equals( getExtra() ) ) {
            differenFields.add( new String[]{ "Extra", getExtra(), column.getExtra() } );
        }
        return differenFields;
    }

    public String getDataAsString() {

        StringBuilder builder = new StringBuilder( "" );
        builder.append( "Field: " + getFieldName() );
        builder.append( " | " );
        builder.append( "Type: " + getType() );
        builder.append( " | " );
        builder.append( "Null: " + getIsNull() );
        builder.append( " | " );
        builder.append( "Key: " + getKey() );
        builder.append( " | " );
        builder.append( "Extra: " + getExtra() );

        return builder.toString();
    }

    //	public String toString() {
    //		StringBuilder builder = new StringBuilder("");
    //		builder.append("Field: "+getFieldName());
    //		
    //		builder.append("Type: "+getType());
    //		
    //		builder.append("Null: "+getIsNull());
    //		
    //		builder.append("Key: "+getKey());
    //		
    //		builder.append("Extra: "+getExtra());
    //		
    //        
    //		return builder.toString();
    //	}

    public String getDefaultValue() {

        return defaultValue;
    }

    public void setDefaultValue(
                                 String defaultValue ) {

        this.defaultValue = defaultValue;
    }

    public String getExtra() {

        return extra;
    }

    public void setExtra(
                          String extra ) {

        this.extra = extra;
    }

    public String getFieldName() {

        return fieldName;
    }

    public void setFieldName(
                              String fieldName ) {

        this.fieldName = fieldName;
    }

    public String getIsNull() {

        return isNull;
    }

    public void setIsNull(
                           String isNull ) {

        this.isNull = isNull;
    }

    public String getKey() {

        return key;
    }

    public void setKey(
                        String key ) {

        this.key = key;
    }

    public String getType() {

        return type;
    }

    public void setType(
                         String type ) {

        this.type = type;
    }
}
