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
package com.axway.ats.agent.webapp.restservice.api.documentation.model;

import com.axway.ats.core.utils.StringUtils;

public class SwaggerMethodParameter {

    private String  in;

    private String  name;

    private String  description;

    private String  definitionName;

    private String  type;

    private boolean required;

    public String getType() {

        return type;
    }

    public void setType( String type ) {

        this.type = type;
    }

    public boolean isRequired() {

        return required;
    }

    public void setRequired( boolean required ) {

        this.required = required;
    }

    public String getIn() {

        return in;
    }

    public void setIn( String in ) {

        this.in = in;
    }

    public String getName() {

        return name;
    }

    public void setName( String name ) {

        this.name = name;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription( String description ) {

        this.description = description;
    }

    public String getDefinitionName() {

        return definitionName;
    }

    public void setDefinitionName( String definitionName ) {

        this.definitionName = definitionName;
    }

    public String toJson( boolean asStandaloneObject ) {

        StringBuilder sb = new StringBuilder();

        if (asStandaloneObject) {
            sb.append("{");
        }

        sb.append("\"")
          .append("in")
          .append("\"")
          .append(":")
          .append("\"")
          .append(in)
          .append("\"")
          .append(",");

        sb.append("\"")
          .append("name")
          .append("\"")
          .append(":")
          .append("\"")
          .append(name)
          .append("\"")
          .append(",");

        sb.append("\"")
          .append("description")
          .append("\"")
          .append(":")
          .append("\"")
          .append(description)
          .append("\"")
          .append(",");

        sb.append("\"")
          .append("required")
          .append("\"")
          .append(":")
          .append(required)
          .append(",");

        if (StringUtils.isNullOrEmpty(definitionName)) {
            sb.setLength(sb.length() - 1); // remove the trailing comma
        } else {
            sb.append("\"")
              .append("schema")
              .append("\"")
              .append(":")
              .append("{")
              .append("\"")
              .append("$ref")
              .append("\"")
              .append(":")
              .append("\"")
              .append("#/definitions/")
              .append(definitionName)
              .append("\"")
              .append("}");
        }

        if (asStandaloneObject) {
            sb.append("}");
        }

        return sb.toString();

    }

}
