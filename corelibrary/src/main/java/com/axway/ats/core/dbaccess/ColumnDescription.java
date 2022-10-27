/*
 * Copyright 2017-2022 Axway Software
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

/**
 * Holds the metadata for a column in the database
 */
public class ColumnDescription {

    private String   name;
    protected String type;
    private String[] subTypes;

    public ColumnDescription( String name,
                              String type ) {

        this.name = name;
        this.type = type.toLowerCase();
    }

    public ColumnDescription( String name,
                              String type,
                              String[] subTypes ) {

        this(name, type);
        this.subTypes = subTypes;
    }

    /**
     * Get the name of the column
     *
     * @return  name of the column
     */
    public String getName() {

        return name;
    }

    /**
     * Get the column type
     *
     * @return  the type as string
     */
    public String getType() {

        return type;
    }

    /**
     * Get the column sub-types. For example Set&lt;Int&gt; or Map&lt;Text,Int&gt;
     *
     * @return the sub-types
     */
    public String[] getSubsType() {

        return subTypes;
    }

    /**
     * Is the type of the column numeric
     *
     * @return  true if the type is numeric
     */
    public boolean isTypeNumeric() {

        return type.contains("int") || type.contains("decimal") || type.contains("numeric")
               || type.contains("real") || type.contains("double") || type.contains("bit");
    }

    /**
     * Is the type of the column a blob or one of its variant
     *
     * @return  true if the type is a blob variant
     */
    public boolean isTypeBinary() {

        // Note: checks might be DB provider specific, so in the future might be split into concrete provider classes
        // MsSQL - 'image' type is only MsSQL one; also there is no 'blob' type there
        // 'char binary' and 'varchar binary' are not considered

        return type.contains("blob") || type.contains("clob") || "binary".equals(type)
               || type.startsWith("binary(") || "varbinary".equals(type)
               || type.startsWith("varbinary(") || "image".equals(type);

    }

    /**
     * @return true if this is a BIT type of DB field
     */
    public boolean isTypeBit() {

        return type.contains("bit");
    }

    @Override
    public String toString() {

        return "ColumnDescription [name=" + name + ", type=" + type + "]";
    }

}
