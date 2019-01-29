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
package com.axway.ats.common.dbaccess.snapshot;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.dbaccess.snapshot.equality.DatabaseEqualityState;
import com.axway.ats.common.systemproperties.AtsSystemProperties;

/**
 * Meta information about a table. Used when comparing databases.
 */
public class TableDescription {

    private static Logger       log              = Logger.getLogger(TableDescription.class);

    // snapshot this table belongs to
    private String              snapshotName;

    // table name
    private String              name;

    //table schema
    private String              schema;

    // primary key column
    private String              primaryKeyColumn = "";

    // description of all table columns
    private List<String>        columnDescriptions;

    // all table indexes
    // <index name, index attributes>
    private Map<String, String> indexes;

    public TableDescription() {

        columnDescriptions = new ArrayList<String>();
        indexes = new HashMap<>();
    }

    public String getSnapshotName() {

        return this.snapshotName;
    }

    public void setSnapshotName( String snapshotName ) {

        this.snapshotName = snapshotName;
    }

    public String getName() {

        return name;
    }

    public void setName( String name ) {

        this.name = name;
    }

    public String getSchema() {

        return schema;
    }

    public void setSchema( String schema ) {

        this.schema = schema;
    }

    public String getPrimaryKeyColumn() {

        return primaryKeyColumn;
    }

    public void setPrimaryKeyColumn( String primaryKeyColumn ) {

        if (primaryKeyColumn != null) {
            this.primaryKeyColumn = primaryKeyColumn;
        }
    }

    public Set<String> getColumnNames() {

        Set<String> columns = new HashSet<>();
        for (String description : columnDescriptions) {
            // we rely that the column description string starts with 'name=some_column,'
            columns.add(description.substring(description.indexOf("=") + 1,
                                              description.indexOf(",")));
        }
        return columns;
    }

    public List<String> getColumnDescriptions() {

        return columnDescriptions;
    }

    public void setColumnDescriptions( List<String> columnDescriptions ) {

        this.columnDescriptions = columnDescriptions;
    }

    public Map<String, String> getIndexes() {

        return indexes;
    }

    public void setIndexes( Map<String, String> indexes ) {

        this.indexes = indexes;
    }

    /**
     * Compares two instances of this table
     * 
     * @param that THAT instance
     * @param thisValuesList the values in THIS instance
     * @param thatValuesList the values in THAT instance
     * @param equality
     */
    public void compare( TableDescription that, List<String> thisValuesList, List<String> thatValuesList,
                         int thisNumberOfRows, int thatNumberOfRows, IndexNameMatcher nameComparator,
                         DatabaseEqualityState equality ) {

        boolean tablesAreSame = true;

        // check primary key column
        if (!this.primaryKeyColumn.equalsIgnoreCase(that.primaryKeyColumn)) {
            tablesAreSame = false;
            equality.addDifferentPrimaryKeys(this.snapshotName, that.snapshotName, this.primaryKeyColumn,
                                             that.primaryKeyColumn, name);
        }

        // check if indexes are the same
        checkIndexes(that, nameComparator, equality);

        // check if columns are the same
        boolean sameColumnNames = checkColumns(that, equality);

        // check the table content only if columns are same or the value lists are not initialized
        if (sameColumnNames) {
            if (thisValuesList != null && thatValuesList != null) {

                // check the table size
                if (thisValuesList.size() != thatValuesList.size()) {
                    tablesAreSame = false;
                    equality.addDifferentNumberOfRows(this.snapshotName, that.snapshotName,
                                                      thisValuesList.size(), thatValuesList.size(), name);
                }

                // check the table content
                removeSameRows(thisValuesList, thatValuesList);
                removeSameRows(thatValuesList, thisValuesList);

                // now if there are left rows, we report them as unexpected
                // differences
                for (String row : thisValuesList) {
                    tablesAreSame = false;
                    equality.addRowPresentInOneSnapshotOnly(this.snapshotName, name, row);
                }
                for (String row : thatValuesList) {
                    tablesAreSame = false;
                    equality.addRowPresentInOneSnapshotOnly(that.snapshotName, name, row);
                }
            }
        } else {
            log.warn("The content of table " + this.name
                     + " will not be checked because there is at least one column with name not present in both snapshots");
        }

        if (thisNumberOfRows != -1 && thisNumberOfRows != thatNumberOfRows) {
            // in some unusual cases, the user does not want to check the table content, but
            // wants to make sure the number of rows is same
            equality.addDifferentNumberOfRows(this.snapshotName, that.snapshotName, thisNumberOfRows,
                                              thatNumberOfRows, name);
        }

        if (tablesAreSame) {
            log.info("Same table: " + name);
        }
    }

    private void removeSameRows( List<String> someRows, List<String> otherRows ) {

        Iterator<String> someIt = someRows.iterator();
        while (someIt.hasNext()) {
            String someRow = someIt.next();

            Iterator<String> otherIt = otherRows.iterator();
            while (otherIt.hasNext()) {
                String otherRow = otherIt.next();
                if (someRow.equals(otherRow)) {
                    someIt.remove();
                    otherIt.remove();
                    break;
                }
            }
        }
    }

    private boolean checkColumns( TableDescription that, DatabaseEqualityState equality ) {

        // check if all columns from THIS snapshot are present in THAT
        for (String thisColumnDesc : this.columnDescriptions) {
            if (!that.columnDescriptions.contains(thisColumnDesc)) {
                equality.addColumnPresentInOneSnapshotOnly(this.snapshotName, name, thisColumnDesc);
            }
        }

        // check if all columns from THAT snapshot are present in THIS
        for (String thatColumnDesc : that.columnDescriptions) {
            if (!this.columnDescriptions.contains(thatColumnDesc)) {
                equality.addColumnPresentInOneSnapshotOnly(that.snapshotName, name, thatColumnDesc);
            }
        }

        return allColumnsHaveSameNames(that);
    }

    /**
     * @param that
     * @return whether all columns of one table have same names
     */
    private boolean allColumnsHaveSameNames( TableDescription that ) {

        Set<String> thisColunmNames = getColumnNames();
        Set<String> thatColunmNames = that.getColumnNames();

        for (String thisColunmName : thisColunmNames) {
            if (!thatColunmNames.contains(thisColunmName)) {
                return false;
            }
        }

        for (String thatColunmName : thatColunmNames) {
            if (!thisColunmNames.contains(thatColunmName)) {
                return false;
            }
        }

        return true;
    }

    private void checkIndexes( TableDescription that, IndexNameMatcher nameComparator,
                               DatabaseEqualityState equality ) {

        final Set<String> thisNames = this.indexes.keySet();
        final Set<String> thatNames = that.indexes.keySet();

        // check if all indexes from THIS snapshot are present in THAT
        for (String thisName : thisNames) {
            String equivalentThatName = null;
            Properties thisIndexProperties = toProperties(this.indexes.get(thisName), ", ");
            Properties thatIndexProperties = null;
            for (String thatName : thatNames) {
                thatIndexProperties = toProperties(that.indexes.get(thatName), ", ");

                // check if both indexes are on the same column in the table
                String thisIndexColumnName = thisIndexProperties.getProperty("COLUMN_NAME");
                if (thisIndexColumnName == null) {
                    thisIndexColumnName = thisIndexProperties.getProperty("column_name");
                }

                String thatIndexColumnName = thatIndexProperties.getProperty("COLUMN_NAME");
                if (thatIndexColumnName == null) {
                    thatIndexColumnName = thatIndexProperties.getProperty("column_name");
                }

                if (!thisIndexColumnName.equals(thatIndexColumnName)) {
                    continue;
                }

                String thisIndexName = thisIndexProperties.getProperty("INDEX_NAME");
                if (thisIndexName == null) {
                    thisIndexName = thisIndexProperties.getProperty("index_name");
                    if (thisIndexName == null) {
                        // MySQL specific case
                        // thisName = <index uid="COLUMN_NAME=<some_value>, INDEX_NAME=<some_value>">
                        thisIndexName = thisName.split(", ")[1].split("=")[1];
                    }
                }

                String thatIndexName = thatIndexProperties.getProperty("INDEX_NAME");
                if (thatIndexName == null) {
                    thatIndexName = thatIndexProperties.getProperty("index_name");
                    if (thatIndexName == null) {
                        // MySQL specific case
                        // thatName = <index uid="COLUMN_NAME=<some_value>, INDEX_NAME=<some_value>">
                        thatIndexName = thatName.split(", ")[1].split("=")[1];
                    }
                }

                if (nameComparator.isSame(name,
                                          thisIndexName,
                                          propertiesToMap(thisIndexProperties),
                                          thatIndexName,
                                          propertiesToMap(thatIndexProperties))) {
                    equivalentThatName = thatName;
                    break;
                }
            }

            if (equivalentThatName == null) {
                // no index with same name
                equality.addIndexPresentInOneSnapshotOnly(this.snapshotName, name, thisName,
                                                          this.indexes.get(thisName));
            } else {
                // there is an index with same name, now check there attributes
                if (!checkIndexesByAttributes(thisIndexProperties,
                                              thatIndexProperties)) {
                    //if (!this.indexes.get(thisName).equals(that.indexes.get(equivalentThatName))) {
                    // index with same name, has different attributes
                    equality.addIndexPresentInOneSnapshotOnly(this.snapshotName, name, thisName,
                                                              this.indexes.get(thisName));
                }
            }
        }

        // check if all indexes from THAT snapshot are present in THIS
        for (String thatName : thatNames) {
            String equivalentThisName = null;
            Properties thatIndexProperties = toProperties(that.indexes.get(thatName), ", ");
            Properties thisIndexProperties = null;
            for (String thisName : thisNames) {
                thisIndexProperties = toProperties(this.indexes.get(thisName), ", ");

                // check if both indexes are on the same column in the table
                String thisIndexColumnName = thisIndexProperties.getProperty("COLUMN_NAME");
                if (thisIndexColumnName == null) {
                    thisIndexColumnName = thisIndexProperties.getProperty("column_name");
                }

                String thatIndexColumnName = thatIndexProperties.getProperty("COLUMN_NAME");
                if (thatIndexColumnName == null) {
                    thatIndexColumnName = thatIndexProperties.getProperty("column_name");
                }

                if (!thisIndexColumnName.equals(thatIndexColumnName)) {
                    continue;
                }

                String thisIndexName = thisIndexProperties.getProperty("INDEX_NAME");
                if (thisIndexName == null) {
                    thisIndexName = thisIndexProperties.getProperty("index_name");
                    if (thisIndexName == null) {
                        // MySQL specific case
                        // thisName = <index uid="COLUMN_NAME=<some_value>, INDEX_NAME=<some_value>">
                        thisIndexName = thisName.split(", ")[1].split("=")[1];
                    }
                }

                String thatIndexName = thatIndexProperties.getProperty("INDEX_NAME");
                if (thatIndexName == null) {
                    thatIndexName = thatIndexProperties.getProperty("index_name");
                    if (thatIndexName == null) {
                        // MySQL specific case
                        // thatName = <index uid="COLUMN_NAME=<some_value>, INDEX_NAME=<some_value>">
                        thatIndexName = thatName.split(", ")[1].split("=")[1];
                    }
                }

                if (nameComparator.isSame(name,
                                          thisIndexName,
                                          propertiesToMap(thisIndexProperties),
                                          thatIndexName,
                                          propertiesToMap(thatIndexProperties))) {
                    equivalentThisName = thisName;
                    break;
                }
            }

            if (equivalentThisName == null) {
                // no index with same name
                equality.addIndexPresentInOneSnapshotOnly(that.snapshotName, name, thatName,
                                                          that.indexes.get(thatName));
            } else {
                // there is an index with same name, now check there attributes
                if (!checkIndexesByAttributes(thisIndexProperties,
                                              thatIndexProperties)) {
                    //if (!this.indexes.get(thisName).equals(that.indexes.get(equivalentThatName))) {
                    // index with same name, has different attributes
                    equality.addIndexPresentInOneSnapshotOnly(that.snapshotName, name, thatName,
                                                              that.indexes.get(thatName));
                }
            }
        }
    }

    /**
     * Create {@link java.util.Properties} object from String
     * @param inputString the String, containing the properties in format key1=valDELIMITERkey2=val
     * @param delimiter the delimiter used to separate each property
     * */
    private Properties toProperties( String inputString, String delimiter ) {

        inputString = inputString.replace(delimiter, AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(inputString));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load java.util.Properties from java.lang.String using delimiter '"
                                       + delimiter + "'", e);
        }
        return properties;
    }
    
    private Map<String, String> propertiesToMap(Properties properties) {
        
        /* Since there can only be String(s) and primitives as a property value
           the returned map is of type Map<String, String>
        */
        Map<String, String> map = new HashMap<String, String>();
        
        for(Entry<Object, Object> entry : properties.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        
        return map;
        
    }

    /**
     * Compare DB table indexes by attributes
     * @param thisIndex full string representation of an table index from the first snapshot
     * @param thatIndex full string representation of an table index from the second snapshot
     * @return <strong>true</strong> if indexes are the same, <strong>false</strong> otherwise
     * */
    private boolean checkIndexesByAttributes( Properties thisIndexProps, Properties thatIndexProps ) {

        try {

            // remove the indexes' names since they were already checked via IndexNameMatcher
            thisIndexProps.remove("INDEX_NAME");
            thisIndexProps.remove("index_name");
            thatIndexProps.remove("INDEX_NAME");
            thatIndexProps.remove("index_name");

            // since there may be multiple indexes for one table
            // check that both indexes that are compared here are on the same column
            String thisIndexColumnName = null;
            String thatIndexColumnName = null;

            if (thisIndexProps.containsKey("COLUMN_NAME")) {
                thisIndexColumnName = thisIndexProps.getProperty("COLUMN_NAME");
            } else if (thisIndexProps.containsKey("column_name")) {
                thisIndexColumnName = thisIndexProps.getProperty("column_name");
            }

            if (thatIndexProps.containsKey("COLUMN_NAME")) {
                thatIndexColumnName = thatIndexProps.getProperty("COLUMN_NAME");
            } else if (thatIndexProps.containsKey("column_name")) {
                thatIndexColumnName = thatIndexProps.getProperty("column_name");
            }

            if (thisIndexColumnName
                                   .equals(thatIndexColumnName)) {
                StringWriter sw1 = new StringWriter();
                thisIndexProps.list(new PrintWriter(sw1));

                StringWriter sw2 = new StringWriter();
                thatIndexProps.list(new PrintWriter(sw2));

                return sw1.toString().equals(sw2.toString());
            } else {
                return false;
            }

        } catch (Exception e) {
            log.error("Error while comparing db table indexes", e);
            return false;
        }
    }

    @Override
    public String toString() {

        StringBuilder description = new StringBuilder();
        description.append("Table ");
        description.append(name);

        description.append(";\nPrimary key: ");
        description.append(primaryKeyColumn);

        description.append(";\nColumns: ");
        for (String colum : getColumnNames()) {
            description.append(colum);
            description.append(", ");
        }
        return description.substring(0, description.length() - 2);
    }

    public void toXmlNode( Document dom, Element tableNode ) {

        tableNode.setAttribute(DatabaseSnapshotUtils.ATTR_TABLE_NAME, name);
        tableNode.setAttribute(DatabaseSnapshotUtils.ATTR_TABLE_PRIMARY_KEY, primaryKeyColumn);

        // append column descriptions
        Element columnDescriptionsNode = dom.createElement(DatabaseSnapshotUtils.NODE_COLUMN_DESCRIPTIONS);
        tableNode.appendChild(columnDescriptionsNode);

        for (String columnDescription : columnDescriptions) {
            Element columnDescriptionNode = dom.createElement(DatabaseSnapshotUtils.NODE_COLUMN_DESCRIPTION);
            columnDescriptionNode.setTextContent(columnDescription);
            columnDescriptionsNode.appendChild(columnDescriptionNode);
        }

        // append indexes
        if (indexes.keySet().size() > 0) {
            Element indexesNode = dom.createElement(DatabaseSnapshotUtils.NODE_INDEXES);
            tableNode.appendChild(indexesNode);

            for (String indexName : indexes.keySet()) {
                Element indexNode = dom.createElement(DatabaseSnapshotUtils.NODE_INDEX);
                indexNode.setAttribute(DatabaseSnapshotUtils.ATTR_NODE_INDEX_UID, indexName);
                indexNode.setTextContent(indexes.get(indexName));
                indexesNode.appendChild(indexNode);
            }
        }
    }

    public static TableDescription fromXmlNode( String snapshotName, Element tableNode ) {

        TableDescription instance = new TableDescription();

        // add basic table info
        instance.setSnapshotName(snapshotName);
        instance.setName(tableNode.getAttribute(DatabaseSnapshotUtils.ATTR_TABLE_NAME));
        instance.setPrimaryKeyColumn(tableNode.getAttribute(DatabaseSnapshotUtils.ATTR_TABLE_PRIMARY_KEY));

        // add column descriptions
        List<Element> columnDescriptionsListNodes = DatabaseSnapshotUtils.getChildrenByTagName(tableNode,
                                                                                               DatabaseSnapshotUtils.NODE_COLUMN_DESCRIPTIONS);
        if (columnDescriptionsListNodes.size() != 1) {
            throw new DatabaseSnapshotException("Bad dabase snapshot backup file. Table with name '"
                                                + tableNode.getAttribute(DatabaseSnapshotUtils.ATTR_TABLE_NAME)
                                                + "' must have 1 '"
                                                + DatabaseSnapshotUtils.NODE_COLUMN_DESCRIPTIONS
                                                + "' subnode, but it has"
                                                + columnDescriptionsListNodes.size());
        }
        List<Element> columnDescriptionNodes = DatabaseSnapshotUtils.getChildrenByTagName(columnDescriptionsListNodes.get(0),
                                                                                          DatabaseSnapshotUtils.NODE_COLUMN_DESCRIPTION);
        List<String> columnDescriptions = new ArrayList<>();
        for (Element columnDescriptionNode : columnDescriptionNodes) {
            columnDescriptions.add(columnDescriptionNode.getTextContent());
        }
        instance.setColumnDescriptions(columnDescriptions);

        // add indexes
        List<Element> indexesListNodes = DatabaseSnapshotUtils.getChildrenByTagName(tableNode,
                                                                                    DatabaseSnapshotUtils.NODE_INDEXES);
        if (indexesListNodes.size() > 1) {
            throw new DatabaseSnapshotException("Bad dabase snapshot backup file. Table with name '"
                                                + tableNode.getAttribute(DatabaseSnapshotUtils.ATTR_TABLE_NAME)
                                                + "' must have 0 or 1 '" + DatabaseSnapshotUtils.NODE_INDEXES
                                                + "' subnode, but it has" + indexesListNodes.size());
        }
        if (indexesListNodes.size() == 1) {
            List<Element> indexNodes = DatabaseSnapshotUtils.getChildrenByTagName(indexesListNodes.get(0),
                                                                                  DatabaseSnapshotUtils.NODE_INDEX);
            Map<String, String> indexes = new HashMap<>();
            for (Element indexNode : indexNodes) {
                indexes.put(indexNode.getAttribute(DatabaseSnapshotUtils.ATTR_NODE_INDEX_UID),
                            indexNode.getTextContent());
            }
            instance.setIndexes(indexes);
        }

        return instance;
    }
}
