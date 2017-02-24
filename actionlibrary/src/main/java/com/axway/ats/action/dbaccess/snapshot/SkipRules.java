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
package com.axway.ats.action.dbaccess.snapshot;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Defines what to skip when comparing tables
 */
public class SkipRules {

    private Set<String> columnsToSkip;

    public SkipRules() {

        columnsToSkip = new HashSet<String>();
    }

    public SkipRules( String... columnsToSkip ) {

        this.columnsToSkip = new HashSet<String>();
        for( String column : columnsToSkip ) {
            this.columnsToSkip.add( column );
        }
    }

    public boolean isSkipWholeTable() {

        return columnsToSkip.size() == 0;
    }

    public void addColumnToSkip(
                                 String columnName ) {

        columnsToSkip.add( columnName );
    }

    public Set<String> getColumnsToSkip() {

        return columnsToSkip;
    }

    @Override
    public String toString() {

        if( isSkipWholeTable() ) {
            return "Skip whole table";
        } else {
            StringBuilder sb = new StringBuilder( "skip columns: " );
            for( String column : columnsToSkip ) {
                sb.append( column );
                sb.append( ", " );
            }
            return super.toString();
        }
    }

    public void toXmlNode(
                           Document dom,
                           Element skipRuleNode ) {

        // append columns to skip
        for( String column : columnsToSkip ) {
            Element columnNode = dom.createElement( "column" );
            columnNode.setTextContent( column );
            skipRuleNode.appendChild( columnNode );
        }
    }
}
