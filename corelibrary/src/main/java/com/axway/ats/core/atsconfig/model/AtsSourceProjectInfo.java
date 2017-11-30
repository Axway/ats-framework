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
package com.axway.ats.core.atsconfig.model;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.axway.ats.core.atsconfig.AtsProjectConfiguration;
import com.axway.ats.core.atsconfig.exceptions.AtsConfigurationException;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.XmlUtils;

public class AtsSourceProjectInfo {

    private String         home;
    private String         agentZip;

    private List<PathInfo> paths = new ArrayList<PathInfo>();

    private Element        sourceNode;

    public AtsSourceProjectInfo( Element sourceNode ) throws AtsConfigurationException {

        this.sourceNode = sourceNode;

        try {
            this.home = IoUtils.normalizeDirPath(XmlUtils.getAttribute(sourceNode,
                                                                       AtsProjectConfiguration.NODE_ATTRIBUTE_HOME,
                                                                       ""));

            // agent zip file
            List<Element> agentZipElements = XmlUtils.getChildrenByTagName(sourceNode, "agentZip");
            if (agentZipElements != null && !agentZipElements.isEmpty()) {
                this.agentZip = XmlUtils.getMandatoryAttribute(agentZipElements.get(0),
                                                               AtsProjectConfiguration.NODE_ATTRIBUTE_PATH);
                // check if agent zip comes from a remote location
                if (!this.agentZip.toLowerCase().startsWith("http://")) {
                    boolean absolute = XmlUtils.getBooleanAttribute(agentZipElements.get(0), "absolute",
                                                                    false);
                    if (!absolute) {
                        this.agentZip = this.home + this.agentZip;
                    }
                }
            }

            // folder sources
            for (Element folderPathNode : XmlUtils.getChildrenByTagName(sourceNode,
                                                                        AtsProjectConfiguration.NODE_ATTRIBUTE_FOLDER)) {
                PathInfo folderPath = new PathInfo(folderPathNode, false, this.home);
                folderPath.loadInternalFilesMap();
                this.paths.add(folderPath);
            }
            // file sources
            for (Element filePathNode : XmlUtils.getChildrenByTagName(sourceNode,
                                                                      AtsProjectConfiguration.NODE_ATTRIBUTE_FILE)) {
                this.paths.add(new PathInfo(filePathNode, true, this.home));
            }
        } catch (AtsConfigurationException e) {
            throw new AtsConfigurationException("Error instantiating " + getClass().getSimpleName()
                                                + " with from XML", e);
        }
    }

    public String getHome() {

        return this.home;
    }

    public void setHome( String home ) {

        this.home = home;
        XmlUtils.setAttribute(sourceNode, AtsProjectConfiguration.NODE_ATTRIBUTE_HOME, this.home);
    }

    public String getAgentZip() {

        return this.agentZip;
    }

    public void setAgentZip( String agentZip ) {

        this.agentZip = agentZip;
        XmlUtils.setAttribute(sourceNode, "agentZip", this.agentZip);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("Source Project at ");
        sb.append(home);

        for (PathInfo path : paths) {
            sb.append("\n " + path.toString());
        }

        return sb.toString();
    }

    public String findFile( String fileName ) {

        for (PathInfo path : paths) {
            if (!path.isFile() && path.getInternalFiles().containsKey(fileName)) {

                return path.getInternalFiles().get(fileName);
            }
            if (path.isFile() && path.getPath().endsWith(fileName)) {

                char fileSeparator = path.getPath().charAt(path.getPath().length() - fileName.length() - 1);
                if (fileSeparator == '\\' || fileSeparator == '/') {

                    return path.getPath();
                }
            }
        }
        return null;
    }
}
