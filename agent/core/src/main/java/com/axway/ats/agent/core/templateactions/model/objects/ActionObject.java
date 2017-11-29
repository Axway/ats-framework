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

import com.axway.ats.agent.core.templateactions.exceptions.InvalidMatcherException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlReaderException;
import com.axway.ats.agent.core.templateactions.exceptions.XmlUtilitiesException;

/**
 * Keep single request/response pair.
 * Used in parsing templates from disk only, i.e. does not include actual responses for example.
 */
public class ActionObject {

    private ActionRequestObject  request;

    private ActionResponseObject response;

    public ActionObject( String actionsXml,
                         String actionRequest,
                         String actionResponse ) throws XmlReaderException, XmlUtilitiesException,
                                                InvalidMatcherException {

        request = new ActionRequestObject( actionsXml, new ActionParser( actionRequest, true ) );

        response = new ActionResponseObject( actionsXml, new ActionParser( actionResponse, false ) );
    }

    public ActionRequestObject getRequest() {

        return request;
    }

    public ActionResponseObject getResponse() {

        return response;
    }
}
