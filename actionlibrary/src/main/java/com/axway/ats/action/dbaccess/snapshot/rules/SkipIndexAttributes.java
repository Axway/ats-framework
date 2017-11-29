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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotUtils;;

/**
 * Defines some index attributes to be skipped for a particular table
 */
public class SkipIndexAttributes extends SkipRule {

    // <index name, <index attribute names>>
    private Map<String, List<String>> attributesPerIndex;

    public SkipIndexAttributes( String table ) {

        super( table );

        attributesPerIndex = new HashMap<>();
    }

    public List<String> getAttributesToSkip( String index ) {

        return attributesPerIndex.get( index );
    }

    public void setAttributeToSkip( String index, String attribute ) {

        List<String> attributes = attributesPerIndex.get( index );
        if( attributes == null ) {
            attributes = new ArrayList<>();
            attributesPerIndex.put( index, attributes );
        }
        attributes.add( attribute );
    }

    public static SkipIndexAttributes fromXmlNode( Element skipColumnNode ) {

        SkipIndexAttributes skipIndexAttributes = new SkipIndexAttributes( skipColumnNode.getAttribute( DatabaseSnapshotUtils.ATT_SKIP_RULE_TABLE ) );

        List<Element> indexNodes = DatabaseSnapshotUtils.getChildrenByTagName( skipColumnNode, "INDEX" );
        for( Element indexNode : indexNodes ) {
            String indexName = indexNode.getAttribute( "name" );
            String indexAttributes = indexNode.getTextContent();
            for( String indexAttribute : indexAttributes.split( "," ) ) {
                if( indexAttribute.trim().length() > 0 ) {
                    skipIndexAttributes.setAttributeToSkip( indexName, indexAttribute.trim() );
                }
            }
        }

        return skipIndexAttributes;
    }

    public void toXmlNode( Document dom, Element parentNode ) {

        Element skipIndexAttributesNode = dom.createElement( DatabaseSnapshotUtils.NODE_SKIP_INDEX_ATTRIBUTES );
        parentNode.appendChild( skipIndexAttributesNode );
        skipIndexAttributesNode.setAttribute( "table", table );

        for( String index : attributesPerIndex.keySet() ) {
            Element indexAttributesNode = dom.createElement( "INDEX" );
            skipIndexAttributesNode.appendChild( indexAttributesNode );
            indexAttributesNode.setAttribute( "name", index );

            StringBuilder sb = new StringBuilder();
            boolean firstTime = true;
            for( String attribute : attributesPerIndex.get( index ) ) {
                if( firstTime ) {
                    firstTime = false;
                } else {
                    sb.append( "," );
                }
                sb.append( attribute );
            }
            indexAttributesNode.setTextContent( sb.toString() );
        }
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder( "Table " + table + ": " );

        for( String indexName : attributesPerIndex.keySet() ) {
            sb.append( "\nskip attributes for index " );
            sb.append( indexName );
            sb.append( ": " );
            boolean firstTime = true;
            for( String attributName : attributesPerIndex.get( indexName ) ) {
                if( firstTime ) {
                    firstTime = false;
                } else {
                    sb.append( "," );
                }
                sb.append( attributName );
            }
        }

        return sb.toString();
    }
}
