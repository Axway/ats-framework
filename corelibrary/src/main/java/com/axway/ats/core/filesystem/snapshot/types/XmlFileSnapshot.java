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
package com.axway.ats.core.filesystem.snapshot.types;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.axway.ats.common.filesystem.snapshot.equality.FileSystemEqualityState;
import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;
import com.axway.ats.common.xml.XMLException;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;
import com.axway.ats.core.filesystem.snapshot.XmlNode;
import com.axway.ats.core.filesystem.snapshot.matchers.FindRules;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipXmlNodeMatcher;

/**
 * Compares the content of XML files
 */
public class XmlFileSnapshot extends ContentFileSnapshot {

    private static final Logger      log              = LogManager.getLogger(XmlFileSnapshot.class);

    private static final long        serialVersionUID = 1L;

    private List<SkipXmlNodeMatcher> matchers         = new ArrayList<>();

    public XmlFileSnapshot( SnapshotConfiguration configuration, String path, FindRules fileRule,
                            List<SkipXmlNodeMatcher> fileMatchers ) {
        super(configuration, path, fileRule);

        if (fileMatchers == null) {
            fileMatchers = new ArrayList<SkipXmlNodeMatcher>();
        }
        for (SkipXmlNodeMatcher matcher : fileMatchers) {
            this.matchers.add(matcher);
        }
    }

    XmlFileSnapshot( String path, long size, long timeModified, String md5, String permissions ) {
        super(path, size, timeModified, md5, permissions);
    }

    /**
     * Used to extend a regular file snapshot instance to the wider XML snapshot instance.
     * It adds all content check matchers
     * 
     * @param fileSnapshot the instance to extend
     * @return the extended instance
     */
    public XmlFileSnapshot getNewInstance( FileSnapshot fileSnapshot ) {

        XmlFileSnapshot instance = new XmlFileSnapshot(fileSnapshot.path, fileSnapshot.size,
                                                       fileSnapshot.timeModified, fileSnapshot.md5,
                                                       fileSnapshot.permissions);
        instance.matchers = this.matchers;

        return instance;
    }

    @Override
    public void compare( FileSnapshot that, FileSystemEqualityState equality, FileTrace fileTrace ) {

        // first compare the regular file attributes
        fileTrace = super.compareFileAttributes(that, fileTrace, true);

        // now compare the files content

        // load the files
        XmlNode thisXmlNode = loadXmlFile(equality.getFirstAtsAgent(), this.getPath());
        XmlNode thatXmlNode = loadXmlFile(equality.getSecondAtsAgent(), that.getPath());

        // remove matched nodes
        // we currently call all matchers on both files,
        // so it does not matter if a matcher is provided for first or second snapshot
        for (SkipXmlNodeMatcher matcher : this.matchers) {
            matcher.process(fileTrace.getFirstSnapshot(), thisXmlNode);
            matcher.process(fileTrace.getSecondSnapshot(), thatXmlNode);
        }
        for (SkipXmlNodeMatcher matcher : ((XmlFileSnapshot) that).matchers) {
            matcher.process(fileTrace.getSecondSnapshot(), thatXmlNode);
            matcher.process(fileTrace.getFirstSnapshot(), thisXmlNode);
        }

        // now compare the rest of the XML nodes
        compareNodes( ((XmlFileSnapshot) that).matchers, thisXmlNode, thatXmlNode, equality, fileTrace);
        ((XmlFileSnapshot) that).compareNodes(this.matchers, thisXmlNode, thatXmlNode, equality,
                                              fileTrace);

        getDifferentNodes(thisXmlNode, fileTrace, false);
        getDifferentNodes(thatXmlNode, fileTrace, true);

        if (fileTrace.hasDifferencies()) {
            // files are different
            equality.addDifference(fileTrace);
        } else {
            log.debug("Same files: " + this.getPath() + " and " + that.getPath());
        }
    }

    private void compareNodes( List<SkipXmlNodeMatcher> thatMatchers, XmlNode thisXmlNode,
                               XmlNode thatXmlNode, FileSystemEqualityState equality, FileTrace fileTrace ) {

        for (XmlNode thisChild : thisXmlNode.getChildren()) {
            for (XmlNode thatChild : thatXmlNode.getChildren()) {
                if (!thatChild.isChecked()) {
                    if (thisChild.getSignature("")
                                 .trim()
                                 .equalsIgnoreCase(thatChild.getSignature("").trim())) {
                        // nodes with same signature found, check if they have same content

                        thisChild.setChecked();
                        thatChild.setChecked();

                        if (thisChild.getContent("")
                                     .trim()
                                     .equalsIgnoreCase(thatChild.getContent("").trim())) {
                            // nodes and their sub-nodes are same, do not need to dig any deeper
                            thisChild.setCheckedIncludingChildren();
                            thatChild.setCheckedIncludingChildren();
                        } else {
                            // nodes with same signature but different content, dig deeper
                            compareNodes(thatMatchers, thisChild, thatChild, equality, fileTrace);
                        }

                        // go check next children
                        break;
                    } else {
                        // nodes with different signature, go check next children
                    }
                }
            }
        }
    }

    private void getDifferentNodes( XmlNode srcNode, FileTrace fileTrace, boolean areSnapshotsReversed ) {

        for (XmlNode child : srcNode.getChildren()) {
            if (child.isChecked()) {
                if (child.getDifferenceDescription() != null) {
                    fileTrace.addDifference(child.getDifferenceDescription(),
                                            "\n\t" + child.getThisDifferenceValue(),
                                            child.getThatDifferenceValue());
                } else {
                    getDifferentNodes(child, fileTrace, areSnapshotsReversed);
                }
            } else {
                if (areSnapshotsReversed) {
                    fileTrace.addDifference("Presence of XML node " + child.getFullSignature(""),
                                            "\n\t" + "NO", "YES");
                } else {
                    fileTrace.addDifference("Presence of XML node " + child.getFullSignature(""),
                                            "\n\t" + "YES", "NO");
                }
            }
        }
    }

    private XmlNode loadXmlFile( String agent, String filePath ) {

        // load the file as a String
        String fileContent = loadFileContent(agent, filePath);

        try {
            SAXReader reader = new SAXReader();

            // following code prevents dom4j from running DTD XML validation
            reader.setEntityResolver(new EntityResolver() {

                @Override
                public InputSource resolveEntity( String publicId, String systemId ) throws SAXException,
                                                                                     IOException {

                    return new InputSource(new StringReader(""));
                }
            });

            // load XML document
            Document xmlDocument = reader.read(new StringReader(fileContent));
            return new XmlNode(null, xmlDocument.getRootElement());
        } catch (XMLException | DocumentException e) {
            throw new XMLException("Error parsing XML file: " + filePath, e);
        }
    }

    public String getFileType() {

        return "xml file";
    }
}
