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

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.axway.ats.core.atsconfig.exceptions.AtsConfigurationException;
import com.axway.ats.core.utils.XmlUtils;

public class ApplicationInfo extends AbstractApplicationInfo {

    public ApplicationInfo( String alias,
                            Element applicationNode,
                            Map<String, String> defaultValues ) throws AtsConfigurationException {

        super( alias, applicationNode, defaultValues );
    }

    @Override
    protected void loadMoreInfo(
                                 Element applicationNode ) {

        Element statusCommandNode = getNoMoreThanOneChild( applicationNode, NODE_STATUS_COMMAND );
        if( statusCommandNode != null ) {
            statusCommandInfo.url = XmlUtils.getMandatoryAttribute( statusCommandNode, NODE_ATTRIBUTE_URL );
            statusCommandInfo.urlSearchToken = XmlUtils.getMandatoryAttribute( statusCommandNode,
                                                                               NODE_ATTRIBUTE_URL_SEARCH_TOKEN );
            statusCommandInfo.command = XmlUtils.getAttribute( statusCommandNode, NODE_ATTRIBUTE_COMMAND );
            statusCommandInfo.stdoutSearchToken = XmlUtils.getAttribute( statusCommandNode,
                                                                         NODE_ATTRIBUTE_STDOUT_SEARCH_TOKEN );
        }

        Element startCommandNode = getNoMoreThanOneChild( applicationNode, NODE_START_COMMAND );
        if( startCommandNode != null ) {
            startCommandInfo.command = XmlUtils.getAttribute( startCommandNode, NODE_ATTRIBUTE_COMMAND );
            startCommandInfo.stdoutSearchToken = XmlUtils.getAttribute( startCommandNode,
                                                                        NODE_ATTRIBUTE_STDOUT_SEARCH_TOKEN );
        }

        Element stopCommandNode = getNoMoreThanOneChild( applicationNode, NODE_STOP_COMMAND );
        if( stopCommandNode != null ) {
            stopCommandInfo.command = XmlUtils.getAttribute( stopCommandNode, NODE_ATTRIBUTE_COMMAND );
            stopCommandInfo.stdoutSearchToken = XmlUtils.getAttribute( stopCommandNode,
                                                                       NODE_ATTRIBUTE_STDOUT_SEARCH_TOKEN );
        }
    }

    private Element getNoMoreThanOneChild(
                                           Element element,
                                           String name ) {

        List<Element> children = XmlUtils.getChildrenByTagName( element, name );
        if( children.size() == 0 ) {
            return null;
        } else {
            return children.get( 0 );
        }
    }
}
