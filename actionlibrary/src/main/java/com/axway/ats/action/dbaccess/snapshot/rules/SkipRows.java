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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotUtils;

/**
 * Defines some rows to be skipped per table
 */
public class SkipRows extends SkipRule {

    private static final Logger log = LogManager.getLogger(SkipRows.class);

    // < table column, cell value >
    private Map<String, String> skipExpressions;

    public SkipRows( String table ) {

        super(table);

        this.skipExpressions = new HashMap<String, String>();
    }

    public SkipRows( String table, String column, String value ) {

        this(table);

        this.skipExpressions.put(column, value);
    }

    public void addRowToSkip( String column, String value ) {

        this.skipExpressions.put(column, value);
    }

    public void addRowsToSkip( Map<String, String> newSkipExpressions ) {

        for (String column : newSkipExpressions.keySet()) {
            String skipValue = this.skipExpressions.get(column);
            String newSkipValue = newSkipExpressions.get(column);
            if (skipValue != null && skipValue.equals(newSkipValue)) {
                log.debug("Same skip row expression '" + skipValue + " is provided for column " + column
                          + " in table " + table);
            }

            this.skipExpressions.put(column, newSkipValue);
        }
    }

    public Map<String, String> getSkipExpressions() {

        return skipExpressions;
    }

    public boolean skipRow( String rowValues ) {

        for (String column : skipExpressions.keySet()) {

            String rowValueToSkip = skipExpressions.get(column);
            if (!rowValueToSkip.endsWith("?")) {
                rowValueToSkip += "?";
            }
            String[] matches = rowValues.split(
                                               // case not sensitively
                                               "(?i)"
                                               // find the column name
                                               + column
                                               // and the '=' delimiter  
                                               + "="
                                               // now enable again case sensitiveness
                                               + "(?-i)"
                                               // so the user provided regular expression is executed as provided
                                               + rowValueToSkip);
            if (matches.length > 1) {
                return true;
            }
        }
        return false;
    }

    public static SkipRows fromXmlNode( Element skipRowsNode ) {

        SkipRows skipRows = new SkipRows(skipRowsNode.getAttribute(DatabaseSnapshotUtils.ATTR_TABLE_NAME));

        List<Element> expressionNodes = DatabaseSnapshotUtils.getChildrenByTagName(skipRowsNode,
                                                                                   "expression");
        for (Element expressionNode : expressionNodes) {
            skipRows.skipExpressions.put(expressionNode.getAttribute("column"),
                                         expressionNode.getAttribute("value"));
        }

        return skipRows;
    }

    public void toXmlNode( Document dom, Element parentNode ) {

        Element skipRowsNode = dom.createElement(DatabaseSnapshotUtils.NODE_SKIP_ROWS);
        parentNode.appendChild(skipRowsNode);
        skipRowsNode.setAttribute("table", table);

        // append info about rows to skip
        for (String column : skipExpressions.keySet()) {
            Element expressionNode = dom.createElement("expression");
            expressionNode.setAttribute("column", column);
            expressionNode.setAttribute("value", skipExpressions.get(column));
            skipRowsNode.appendChild(expressionNode);
        }
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("Table " + table + ": ");

        for (String column : skipExpressions.keySet()) {
            sb.append("\nskip row where '" + column + "' matches '" + skipExpressions.get(column) + "'");
        }

        return sb.toString();
    }
}
