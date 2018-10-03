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

public class SwaggerMethodResponse {

    private int    code;

    private String description;

    private String definitionLink;

    public SwaggerMethodResponse() {

    }

    public int getCode() {

        return code;
    }

    public void setCode( int code ) {

        this.code = code;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription( String description ) {

        this.description = description;
    }

    public String getDefinitionLink() {

        return definitionLink;
    }

    public void setDefinitionLink( String definitionLink ) {

        this.definitionLink = definitionLink;
    }

    public String toJson( boolean asStandaloneObject ) {

        StringBuilder sb = new StringBuilder();

        if (asStandaloneObject) {
            sb.append("{");
        }

        sb.append("\"")
          .append(code)
          .append("\"")
          .append(":")
          .append("{")
          .append("\"")
          .append("description")
          .append("\"")
          .append(":")
          .append("\"")
          .append(description)
          .append("\"")
          .append(",")
          .append("\"")
          .append("schema")
          .append("\"")
          .append(":")
          .append("{")
          .append("\"")
          .append("$ref")
          .append("\"")
          .append(":")
          .append("\"")
          .append("#definitions/")
          .append(definitionLink)
          .append("\"")
          .append("}")
          .append("}");

        if (asStandaloneObject) {
            sb.append("}");
        }

        return sb.toString();

    }
}
