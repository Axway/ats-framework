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
package com.axway.ats.agent.core.templateactions.model;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axway.ats.agent.core.templateactions.exceptions.XmlReaderException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;
import com.axway.ats.agent.core.templateactions.model.objects.ActionHeader;
import com.axway.ats.agent.core.templateactions.model.objects.ActionObject;
import com.axway.ats.agent.core.templateactions.model.objects.ActionResponseObject;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;

/**
 * A Reader of a template action XML file
 */
public class XmlReader {

    private String                                 actionsXml;

    private List<ActionObject>                     actionNodes;

    private static Map<String, List<ActionObject>> actionNodesMap          = new HashMap<String, List<ActionObject>>();

    private int                                    iActionNodes;

    private static final int                       AVERAGE_XML_LINE_LENGTH = 150;

    public XmlReader( String actionsXml ) throws XmlReaderException, XmlUtilitiesException {

        this.actionsXml = actionsXml;

        loadXmlFile();
    }

    /**
     * Move to the next action. Return true if there was another action to move to.
     *
     * @return <code>true</code> if there was another action to move to
     */
    public boolean goToNextAction() {

        if (iActionNodes < actionNodes.size() - 1) {
            iActionNodes++;
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return if we are working with the last action
     */
    public boolean isLastAction() {

        return iActionNodes == actionNodes.size() - 1;
    }

    public ActionResponseObject getResponse() {

        return actionNodes.get(iActionNodes).getResponse();
    }

    public String getRequestHttpUrl() throws XmlReaderException, XmlUtilitiesException {

        return actionNodes.get(iActionNodes).getRequest().getHttpUrl();
    }

    public String getRequestHttpMethod() throws XmlReaderException {

        return actionNodes.get(iActionNodes).getRequest().getHttpMethod();
    }

    public List<ActionHeader> getRequestHttpHeaders() throws XmlUtilitiesException {

        return actionNodes.get(iActionNodes).getRequest().getHttpHeaders();
    }

    public String getRequestResourceFile() {

        return actionNodes.get(iActionNodes).getRequest().getResourceFile();
    }

    public boolean hasParamsInRequestResourceFile() {

        return actionNodes.get(iActionNodes).getRequest().hasParamsInResourceFile();
    }

    private void loadXmlFile() throws XmlReaderException, XmlUtilitiesException {

        //FIXME: performance: different actionsXml files could be loaded concurrently but extensive synchronization is needed
        synchronized (actionNodesMap) {

            if (!actionNodesMap.containsKey(actionsXml)) {

                // load the document
                BufferedReader br = null;
                FileChunkReader fileChunkReader = null;
                try {

                    // load the action nodes and add them to the map
                    actionNodes = new ArrayList<ActionObject>();

                    br = new BufferedReader(new InputStreamReader(new FileInputStream(actionsXml)));
                    fileChunkReader = new FileChunkReader();

                    StringBuilder request = new StringBuilder(1000);
                    //if the response doesn't contain parameters we will keep it without the response body part
                    StringBuilder responseWoBodyBuilder = new StringBuilder(1000);
                    String response = null;
                    boolean inRequest = false;
                    boolean inResponse = false;
                    boolean hasParametersInResponse = false;

                    int currentLineNumber = 0;
                    int startLineMarker = 0;
                    String line;
                    while ( (line = br.readLine()) != null) {

                        currentLineNumber++;
                        if (line.contains("<HTTP_ACTION>")) {

                            inResponse = false;
                            inRequest = false;
                            continue;
                        } else if (line.contains("</HTTP_ACTION>")) {

                            actionNodes.add(new ActionObject(actionsXml, request.toString(), response));
                            continue;
                        } else if (line.contains("<HTTP_REQUEST ") || line.contains("<HTTP_REQUEST>")) {
                            // the normal case is "<HTTP_REQUEST ", 
                            // but we also handle here the "<HTTP_REQUEST>" in case the HTTP method attribute is missing
                            // sometime later an appropriate exception will be thrown

                            // clear old request data
                            request.delete(0, request.length());
                            inRequest = true;
                        } else if (line.contains("</HTTP_REQUEST>")) {

                            request.append(line + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                            inRequest = false;
                            continue;
                        } else if (line.contains("<HTTP_RESPONSE>")) {

                            startLineMarker = currentLineNumber;
                            // clear old response data
                            responseWoBodyBuilder.delete(0, responseWoBodyBuilder.length());
                            responseWoBodyBuilder.append(line + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                            inResponse = true;
                            continue;
                        } else if (line.contains("</HTTP_RESPONSE>")) {

                            responseWoBodyBuilder.append(line + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                            // optimization - do not store body if there are no variables in the body
                            if (hasParametersInResponse) {
                                response = fileChunkReader.readChunk(startLineMarker, currentLineNumber);
                            } else {
                                response = responseWoBodyBuilder.toString();
                            }
                            inResponse = false;
                            hasParametersInResponse = false;
                            continue;
                        }

                        if (inRequest) {

                            request.append(line + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                        } else if (inResponse) {

                            // search for parameters in the response
                            if (!hasParametersInResponse && line.contains("${")
                                && line.matches(".*\\$\\{.+\\}.*")) {

                                hasParametersInResponse = true;
                            }
                            // collect the response data without the response body
                            if (line.contains("<HTTP_HEADER ") || line.contains("<HTTP_RESOURCE_FILE")
                                || line.contains("<HTTP_RESPONSE_RESULT>")) {

                                responseWoBodyBuilder.append(line
                                                             + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                            }
                        }
                    }
                    actionNodesMap.put(actionsXml, actionNodes);

                } catch (Exception e) {
                    throw new XmlReaderException(actionsXml, e);
                } finally {
                    IoUtils.closeStream(fileChunkReader);
                    IoUtils.closeStream(br);
                }

            } else {

                actionNodes = actionNodesMap.get(actionsXml);
            }
            iActionNodes = -1;
        }
    }

    class FileChunkReader implements Closeable {

        private int            currentLine = 1;
        private BufferedReader reader;

        public FileChunkReader() throws FileNotFoundException {

            this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(actionsXml)));
        }

        public String readChunk( int fromLine, int toLine ) {

            StringBuilder sb = new StringBuilder( (toLine - fromLine) * AVERAGE_XML_LINE_LENGTH);
            try {
                String line;
                while ( (line = reader.readLine()) != null) {

                    if (currentLine >= fromLine) {

                        sb.append(line + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                    }
                    if (currentLine++ >= toLine) {
                        break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to get a chunk of data from file: " + actionsXml, e);
            }
            return sb.toString();
        }

        @Override
        public void close() throws IOException {

            IoUtils.closeStream(reader);
        }
    }
}
