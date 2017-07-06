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
package com.axway.ats.core.filesystem.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.w3c.dom.Node;

public class XmlNode {

    // the internal node instance
    private Element             node;

    private String              name;                 // node name
    private Map<String, String> attributes;           // node attributes
    private String              value;                // node value

    private XmlNode             parent;               // parent node
    private List<XmlNode>       children;             // children nodes

    // whether this node was already checked(compared)
    private boolean             checked;

    private String              differenceDescription;
    private String              thisDifferenceValue;
    private String              thatDifferenceValue;

    public Element getnode() {

        return node;
    }

    public XmlNode( XmlNode parent, Element node ) {

        this.node = node;
        this.name = node.getName();

        this.attributes = new TreeMap<>();
        for( int i = 0; i < node.attributes().size(); i++ ) {
            Attribute att = node.attribute( i );
            this.attributes.put( att.getName(), att.getValue() );
        }

        this.value = this.node.getTextTrim();

        this.parent = parent;

        this.children = new ArrayList<>();
        List<Element> childrenElements = this.node.elements();
        for( Element child : childrenElements ) {
            if( child.getNodeType() == Node.ELEMENT_NODE ) {
                this.children.add( new XmlNode( this, child ) );
            }
        }
    }

    public Map<String, String> getAttributes() {

        return attributes;
    }

    public String getValue() {

        return value;
    }

    public List<XmlNode> getChildren() {

        return this.children;
    }

    /**
     * Return the signature of this node and its parents
     * @param indent
     * @return
     */
    public String getFullSignature( String indent ) {

        List<String> signatures = new ArrayList<>();

        // cycle through all its parents
        XmlNode _parent = parent;
        while( _parent != null ) {
            signatures.add( _parent.getSignature( "" ) );
            _parent = _parent.parent;
        }

        indent = "\t";
        StringBuilder sb = new StringBuilder();
        for( int i = signatures.size() - 1; i >= 0; i-- ) {
            sb.append( "\n" + indent + signatures.get( i ) );
            indent += "\t";
        }

        sb.append( "\n" + this.getSignature( indent ) );
        if( attributes.size() != 0 ) {
            // if there are no attributes, then the node value is already added as part of the signature
            sb.append( this.getValue() );
        }
        sb.append( "</" + this.name + ">" );

        return sb.toString();
    }

    /**
     * We use "signature" in order to distinguish 2 nodes to be compared. 
     * 
     * The node "signature" contains:
     * - its name and attributes
     *   or
     * - its name and value (this is when has no attributes)
     * 
     * The node children are not included.
     *  
     * @param indent
     * @return
     */
    public String getSignature( String indent ) {

        StringBuilder sb = new StringBuilder();

        sb.append( indent + "<" + name );
        // the signature contains either the node attributes
        for( Map.Entry<String, String> attribute : attributes.entrySet() ) {
            sb.append( " " + attribute.getKey() + "=\"" + attribute.getValue() + "\"" );
        }
        sb.append( ">" );

        // or the node value
        if( attributes.size() == 0 && value.length() > 0 ) {
            sb.append( value );
        }

        return sb.toString();
    }

    public String getContent( String indent ) {

        StringBuilder sb = new StringBuilder();
        sb.append( "\n" + this.getSignature( indent ) );

        boolean hasChildren = false;
        for( XmlNode child : getChildren() ) {
            hasChildren = true;
            //sb.append( child.getSignature( indent ) );
            sb.append( child.getContent( indent + "\t" ) );
        }

        if( hasChildren ) {
            sb.append( "\n" + indent );
        }
        sb.append( "</" + name + ">" );

        return sb.toString();
    }

    /**
     * If some node is to be skipped, we do it in this method
     * 
     * @param matchedNode
     */
    public void removeChild( Element matchedNode ) {

        // remove the matched node
        matchedNode.detach();

        // now we have to regenerate info that might have been affected
        this.value = this.node.getTextTrim();

        this.children.clear();
        List<Element> childrenElements = this.node.elements();
        for( Element child : childrenElements ) {
            if( child.getNodeType() == Node.ELEMENT_NODE ) {
                this.children.add( new XmlNode( this, child ) );
            }
        }
    }

    @Override
    public String toString() {

        String indent = "";
        StringBuilder sb = new StringBuilder();

        sb.append( getSignature( indent ) );

        List<XmlNode> children = getChildren();
        if( children.size() > 0 ) {
            for( XmlNode child : children ) {
                sb.append( child.getContent( indent + "\t" ) );
            }
            sb.append( "\n" + indent );
            sb.append( "</" + name + ">" );
        } else {
            sb.append( value );
            sb.append( "</" + name + ">" );
        }

        return sb.toString();
    }

    public boolean isChecked() {

        return this.checked;
    }

    public void setChecked() {

        this.checked = true;
    }

    public void setCheckedIncludingChildren() {

        this.checked = true;

        for( XmlNode child : getChildren() ) {
            child.setCheckedIncludingChildren();
        }
    }

    public String getDifferenceDescription() {

        return differenceDescription;
    }

    public String getThisDifferenceValue() {

        return thisDifferenceValue;
    }

    public String getThatDifferenceValue() {

        return thatDifferenceValue;
    }

    public void setDifference( String description, String thisValue, String thatValue ) {

        this.differenceDescription = description;
        this.thisDifferenceValue = thisValue;
        this.thatDifferenceValue = thatValue;
    }
}
