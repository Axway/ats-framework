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

public class SwaggerDefinitionProperty {

    private boolean required;

    private String  name;

    private String  type;

    private String  format;

    private String  example;

    private String  description;

    public SwaggerDefinitionProperty() {}

    public boolean isRequired() {

        return required;
    }

    public void setRequired( boolean required ) {

        this.required = required;
    }

    public String getName() {

        return name;
    }

    public void setName( String name ) {

        this.name = name;
    }

    public String getType() {

        return type;
    }

    public void setType( String type ) {

        this.type = type;
    }

    public String getFormat() {

        return format;
    }

    public void setFormat( String format ) {

        this.format = format;
    }

    public String getExample() {

        return example;
    }

    public void setExample( String example ) {

        this.example = example;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription( String description ) {

        this.description = description;
    }

    public SwaggerMethodParameter toQueryParameter() {

        SwaggerMethodParameter parameter = new SwaggerMethodParameter();
        parameter.setDefinitionName("");
        parameter.setDescription(description);
        parameter.setIn("query");
        parameter.setName(name);
        parameter.setRequired(required);
        parameter.setType(type);

        return parameter;
    }

    public String toJson( boolean asStandaloneObject ) {

        StringBuilder sb = new StringBuilder();

        if (asStandaloneObject) {
            sb.append("{");
        }

        sb.append("\"")
          .append(name)
          .append("\"")
          .append(":")
          .append("{");

        sb.append("\"")
          .append("type")
          .append("\"")
          .append(":")
          .append("\"")
          .append(type)
          .append("\"")
          .append(",");

        if (!StringUtils.isNullOrEmpty(format)) {
            sb.append("\"")
              .append("format")
              .append("\"")
              .append(":")
              .append("\"")
              .append(format)
              .append("\"")
              .append(",");
        }

        if (!StringUtils.isNullOrEmpty(example)) {
            if (type != null && (type.equalsIgnoreCase("object") || type.endsWith("[]"))) {
                sb.append("\"")
                  .append("example")
                  .append("\"")
                  .append(":")
                  .append(example)
                  .append(",");
            } else {
                sb.append("\"")
                  .append("example")
                  .append("\"")
                  .append(":")
                  .append("\"")
                  .append(example)
                  .append("\"")
                  .append(",");
            }

        }

        if (!StringUtils.isNullOrEmpty(description)) {
            sb.append("\"")
              .append("description")
              .append("\"")
              .append(":")
              .append("\"")
              .append(description)
              .append("\"");
        } else {
            // remove trailing comma
            sb.setLength(sb.length() - 1);
        }

        sb.append("}");

        if (asStandaloneObject) {
            sb.append("}");
        }

        return sb.toString();
    }

}
