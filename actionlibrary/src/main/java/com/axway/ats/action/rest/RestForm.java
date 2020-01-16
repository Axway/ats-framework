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
package com.axway.ats.action.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.Form;

import com.axway.ats.common.PublicAtsApi;

/**
* A representation of a REST form
*/
@PublicAtsApi
public class RestForm {

    private Map<String, String> parameters;

    @PublicAtsApi
    public RestForm() {

        parameters = new HashMap<String, String>();
    }

    /**
     * Add form parameters.<br><br>
     *
     * <b>Note:</b> For convenience this method returns the form itself,
     * so you can write code like new RESTForm().addParameter("username", "Will").addParameter("userpass", "Smith")
     *
     * @param parameterName the parameter name
     * @param parameterValue the parameter value
     * @return this form itself
     */
    @PublicAtsApi
    public RestForm addParameter(
                                  String parameterName,
                                  String parameterValue ) {

        parameters.put(parameterName, parameterValue);
        return this;
    }

    Form getForm() {

        Form form = new Form();
        for (Entry<String, String> pramEntry : parameters.entrySet()) {
            form.param(pramEntry.getKey(), pramEntry.getValue());
        }

        return form;
    }
}
