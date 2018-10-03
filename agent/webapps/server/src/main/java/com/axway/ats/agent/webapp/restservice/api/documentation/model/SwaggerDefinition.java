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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SwaggerDefinition {

    private String                          name;
    private List<SwaggerDefinitionProperty> properties;

    public SwaggerDefinition() {

        this.properties = new ArrayList<>();
    }

    public String getName() {

        return name;
    }

    public void setName( String name ) {

        this.name = name;
    }

    public List<SwaggerDefinitionProperty> getProperties() {

        return properties;
    }

    public void setParameterProperties( List<SwaggerDefinitionProperty> properties ) {

        this.properties = properties;
    }

    public void addProperty( SwaggerDefinitionProperty property ) {

        this.properties.add(property);
    }

    public boolean equals( Object definition ) {

        SwaggerDefinition thatDef = (SwaggerDefinition) definition;

        return this.name.equals(thatDef.getName());

    }

    @Override
    public int hashCode() {

        return Objects.hash(name);
    }

    public String toJson( boolean asStandaloneObject ) {

        StringBuilder sb = new StringBuilder();

        if (asStandaloneObject) {
            sb.append("{");
        }

        if (properties.isEmpty()) {

            sb.append("\"")
              .append(name)
              .append("\"")
              .append(":")
              .append("{")
              .append("\"")
              .append("required")
              .append("\"")
              .append(": []")
              .append(",")
              .append("\"")
              .append("properties")
              .append("\"")
              .append(":")
              .append("{}")
              .append("}");

        } else {
            final String REQUIRED_PLACEHOLDER_TAG = "<_REQUIRED_PARAMETERS_>";
            sb.append("\"")
              .append(name)
              .append("\"")
              .append(":")
              .append("{")
              .append("\"")
              .append("required")
              .append("\"")
              .append(":" + REQUIRED_PLACEHOLDER_TAG + ",")
              .append("\"")
              .append("properties")
              .append("\"")
              .append(":")
              .append("{");
            String[] requiredProperties = new String[properties.size()];
            for (int i = 0; i < properties.size(); i++) {
                SwaggerDefinitionProperty property = properties.get(i);
                if (property.isRequired()) {
                    requiredProperties[i] = property.getName();
                }
                sb.append(property.toJson(false)).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("}");

            if (requiredProperties.length > 0) {
                StringBuilder requiredSb = new StringBuilder();
                requiredSb.append("[");
                for (String requiredProp : requiredProperties) {
                    requiredSb.append("\"")
                              .append(requiredProp)
                              .append("\"")
                              .append(",");
                }
                requiredSb.setLength(requiredSb.length() - 1);
                requiredSb.append("]");
                sb = new StringBuilder(sb.toString().replace(REQUIRED_PLACEHOLDER_TAG, requiredSb.toString()));
            } else {
                sb = new StringBuilder(sb.toString().replace(REQUIRED_PLACEHOLDER_TAG, "[]"));
            }
            
            sb.append("}");

        }

        if (asStandaloneObject) {
            sb.append("}");
        }

        return sb.toString();
    }

}
