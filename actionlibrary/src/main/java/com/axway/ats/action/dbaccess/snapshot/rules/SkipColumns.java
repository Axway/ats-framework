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
package com.axway.ats.action.dbaccess.snapshot.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotUtils;

/**
 * Defines some table columns to be skipped
 */
public class SkipColumns extends SkipRule {

    // the columns to skipped
    // if list is empty -> the whole table is skipped
    private Set<String> columnsToSkip;

    public SkipColumns( String table ) {

        super( table );

        columnsToSkip = new HashSet<String>();
    }

    public SkipColumns( String table, String... columnsToSkip ) {

        this( table );

        for( String column : columnsToSkip ) {
            this.columnsToSkip.add( column );
        }
    }

    public static SkipColumns fromXmlNode( Element skipColumnNode ) {

        SkipColumns skipColumns = new SkipColumns( skipColumnNode.getAttribute( DatabaseSnapshotUtils.ATTR_TABLE_NAME ) );

        List<Element> columnNodes = DatabaseSnapshotUtils.getChildrenByTagName( skipColumnNode, "column" );
        for( Element columnNode : columnNodes ) {
            skipColumns.columnsToSkip.add( columnNode.getTextContent() );
        }

        return skipColumns;
    }

    /**
     * @return whether the whole table content is to be skipped
     */
    public boolean isSkipWholeTable() {

        return columnsToSkip.size() == 0;
    }

    /**
     * Check if some column is to be skipped.
     * The search must be case not sensitive
     * 
     * @param column
     * @return
     */
    public boolean isSkipColumn( String column ) {

        for( String columnToSkip : columnsToSkip ) {
            if( columnToSkip.equalsIgnoreCase( column ) ) {
                return true;
            }
        }

        return false;
    }

    public void addColumnToSkip( String columnName ) {

        columnsToSkip.add( columnName );
    }

    public Set<String> getColumnsToSkip() {

        return columnsToSkip;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder( "Table " + table + ": " );

        if( isSkipWholeTable() ) {
            sb.append( "skip whole table" );
        } else {
            sb.append( "skip columns: " );
            for( String column : columnsToSkip ) {
                sb.append( column );
                sb.append( ", " );
            }
        }

        return sb.toString();
    }

    public void toXmlNode( Document dom, Element parentNode ) {

        Element skipColumnsNode = dom.createElement( DatabaseSnapshotUtils.NODE_SKIP_COLUMNS );
        parentNode.appendChild( skipColumnsNode );
        skipColumnsNode.setAttribute( "table", table );

        // append columns to skip
        for( String column : columnsToSkip ) {
            Element columnNode = dom.createElement( "column" );
            columnNode.setTextContent( column );
            skipColumnsNode.appendChild( columnNode );
        }
    }
}