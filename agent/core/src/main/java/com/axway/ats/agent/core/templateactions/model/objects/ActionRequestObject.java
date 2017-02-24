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

import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HEADER_NAME_ATTRIBUTE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HEADER_VALUE_ATTRIBUTE;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_HEADER;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_REQUEST_METHOD;
import static com.axway.ats.common.agent.templateactions.TemplateActionsXmlDefinitions.TOKEN_HTTP_REQUEST_URL;

import org.w3c.dom.Node;

import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlReaderException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;
import com.axway.ats.agent.core.templateactions.model.XmlUtilities;
import com.axway.ats.agent.core.templateactions.model.matchers.HeaderMatcher;

/**
 * Currently used for keeping expected request (from template) and not the actual request (parameterized for sending over the wire)
 *
 */
public class ActionRequestObject extends AbstractActionObject {

    private String httpUrl;

    private String httpMethod;

    public ActionRequestObject( String actionsXml, ActionParser action ) throws XmlReaderException,
                                                                         XmlUtilitiesException,
                                                                         InvalidMatcherException {

        super( actionsXml, action );

        resolveHttpUrl( action.getActionNodeWithoutBody() );

        resolveHttpMethod( action.getActionNodeWithoutBody() );
    }

    @Override
    protected void resolveHttpHeaders( Node actionRequest ) throws XmlReaderException {

        for( Node httpHeaderNode : XmlUtilities.getChildrenNodes( actionRequest, TOKEN_HTTP_HEADER ) ) {
            // get header name
            String headerName = xmlUtilities.getNodeAttribute( httpHeaderNode, TOKEN_HEADER_NAME_ATTRIBUTE );
            // get header value and modify its value if user wants it
            String headerValue = xmlUtilities.getNodeAttribute( httpHeaderNode,
                                                                TOKEN_HEADER_VALUE_ATTRIBUTE );

            if( HeaderMatcher.TRANSFER_ENCODING_HEADER_NAME.equalsIgnoreCase( headerName )
                && ( "chunked".equalsIgnoreCase( headerValue ) ) ) {
                // we currently do not enforce chunked requests
                continue;
            }
            httpHeaders.add( new ActionHeader( headerName, headerValue ) );
        }
    }

    public String getHttpUrl() throws XmlUtilitiesException {

        return XmlUtilities.applyUserParameters( httpUrl );
    }

    public String getHttpMethod() {

        return httpMethod;
    }

    private void resolveHttpUrl( Node actionRequest ) throws XmlReaderException {

        Node httpUrlNode = XmlUtilities.getFirstChildNode( actionRequest, TOKEN_HTTP_REQUEST_URL );
        if( httpUrlNode == null ) {
            throw new XmlReaderException( actionsXmlName, "No " + TOKEN_HTTP_REQUEST_URL + " node" );
        } else {
            httpUrl = httpUrlNode.getTextContent();
        }
    }

    private void resolveHttpMethod( Node actionRequest ) throws XmlReaderException, XmlUtilitiesException {

        String httpRequestMethod = xmlUtilities.getNodeAttribute( actionRequest, TOKEN_HTTP_REQUEST_METHOD );
        if( httpRequestMethod == null ) {
            throw new XmlReaderException( actionsXmlName,
                                          "No " + TOKEN_HTTP_REQUEST_METHOD + " attribute in "
                                                          + xmlUtilities.xmlNodeToString( actionRequest )
                                                          + " node" );
        } else {
            httpMethod = httpRequestMethod;
        }
    }

}
