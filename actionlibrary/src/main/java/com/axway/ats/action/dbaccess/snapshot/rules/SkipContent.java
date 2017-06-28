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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotUtils;

/**
 * Defines if the table content is to be skipped.
 */
public class SkipContent extends SkipRule {

    // true when we need to remember the number of table rows
    private boolean rememberNumberOfRows;

    public SkipContent( String table, boolean rememberNumberOfRows ) {

        super( table );

        this.rememberNumberOfRows = rememberNumberOfRows;
    }

    public static SkipContent fromXmlNode( Element skipContentNode ) {

        return new SkipContent( skipContentNode.getAttribute( "table" ),
                                Boolean.parseBoolean( skipContentNode.getAttribute( DatabaseSnapshotUtils.ATTR_TABLE_NUMBER_ROWS ) ) );
    }

    public boolean isRememberNumberOfRows() {

        return rememberNumberOfRows;
    }

    public void setRememberNumberOfRows( boolean rememberNumberOfRows ) {

        this.rememberNumberOfRows = rememberNumberOfRows;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder( "Table " + table + ": " );

        if( rememberNumberOfRows ) {
            sb.append( "remember the number of rows" );
        } else {
            sb.append( "do not remember the number of rows" );
        }

        return sb.toString();
    }

    public void toXmlNode( Document dom, Element parentNode ) {

        Element skipContentNode = dom.createElement( DatabaseSnapshotUtils.NODE_SKIP_CONTENT );
        parentNode.appendChild( skipContentNode );
        skipContentNode.setAttribute( "table", table );
        skipContentNode.setAttribute( DatabaseSnapshotUtils.ATTR_TABLE_NUMBER_ROWS,
                                      String.valueOf( rememberNumberOfRows ) );
    }
}