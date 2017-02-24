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
package com.axway.ats.core.dbaccess;

public class OracleColumnDescription extends ColumnDescription {

    public OracleColumnDescription(
                                    String name, String type ) {
        super( name, type );
    }

    // for information about all data types in Oracle
    // https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#i16209
    // http://www.techonthenet.com/oracle/datatypes.php

    @Override
    public boolean isTypeBinary() {
        String normType = type.toLowerCase();
        return "binary".equals( normType ) || "varbinary".equals( normType )
               || "blob".equals( normType ) || normType.contains( "clob" )
               || normType.contains( "raw" );
    }

    @Override
    public boolean isTypeNumeric() {
        String normType = type.toLowerCase();
        return normType.contains( "int" ) || normType.contains( "number" )
               || normType.contains( "float" ) || normType.contains( "double" )
               || normType.contains( "dec" ) || normType.contains( "numeric" );
    }
}
