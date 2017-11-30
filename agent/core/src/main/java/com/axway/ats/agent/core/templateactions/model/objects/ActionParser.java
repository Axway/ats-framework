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
package com.axway.ats.agent.core.templateactions.model.objects;

import org.w3c.dom.Node;

import com.axway.ats.agent.core.templateactions.exceptions.XmlReaderException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;
import com.axway.ats.agent.core.templateactions.model.XmlUtilities;
import com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions;

/**
 * Container for request/response data into XML Node and optionally if needed - the HTTP body
 * Needed body data formats are parsed lazily on demand bytes-> ActionMessage->XML String ->XML Node
 */
public class ActionParser {

    private String       contentType;

    private Node         actionNodeWithoutBody;
    private XmlUtilities xmlUtilities;

    // Full Plain HTTP body in bytes. Currently used when a user wants to get the body content as a plain text.
    private byte[]       bodyBytes;

    /**
     * Parses XML nodes of request/response.
     * <em>Note:</em>Used for templates loaded from disk
     * @param requestOrResponseAction XML contents
     * @param isRequest is request (true) or response (false) contents
     * @throws XmlUtilitiesException
     * @throws XmlReaderException
     */
    public ActionParser( String requestOrResponseAction, boolean isRequest ) throws XmlUtilitiesException,
                                                                             XmlReaderException {

        // plain HTTP request/response
        actionNodeWithoutBody = XmlUtilities.getFirstChildNode(XmlUtilities.stringToXmlDocumentObj(requestOrResponseAction),
                                                               isRequest
                                                                         ? TemplateActionsXmlDefinitions.TOKEN_HTTP_REQUEST
                                                                         : TemplateActionsXmlDefinitions.TOKEN_HTTP_RESPONSE);
    }

    /**
     * Keeps in XML Node main items needed only.<br />
     * <em>Note:</em> Used for actual responses received over the wire
     * @param actionNodeWithoutBody the main items needed for processing
     * @param bodyBytes the response body bytes
     */
    public ActionParser( Node actionNodeWithoutBody, byte[] bodyBytes ) {

        this.actionNodeWithoutBody = actionNodeWithoutBody;
        this.bodyBytes = bodyBytes;
    }

    /**
     *
     * @return action xml node without body content node
     */
    public Node getActionNodeWithoutBody() {

        return actionNodeWithoutBody;
    }

    /**
     *
     * @return the body content as {@link String}
     */
    public String getBodyContentAsString() {

        return new String(bodyBytes);
    }

    /**
     *
     * @return the body content as byte array
     */
    public byte[] getNonHttpBody() {

        return bodyBytes;
    }

    /**
     * Clean references to facilitate faster garbage collection
     */
    public void cleanupMembers() {

        bodyBytes = null;
    }

    /**
     *
     * @return the Content-Type header value
     */
    public String getContentType() {

        return contentType;
    }

    /**
     *
     * @param contentType the Content-Type header value
     */
    public void setContentType( String contentType ) {

        this.contentType = contentType;
    }

    private XmlUtilities getXmlUtilities() {

        if (xmlUtilities == null) {
            xmlUtilities = new XmlUtilities();
        }
        return xmlUtilities;
    }

}
