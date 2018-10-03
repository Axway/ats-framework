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

public class SwaggerInfo {

    private String             description;
    private String             version;
    private String             title;
    private SwaggerInfoContact contact;

    public SwaggerInfo() {

        super();
    }

    public SwaggerInfo( String description,
                        String version,
                        String title,
                        SwaggerInfoContact contact ) {

        super();

        this.description = description;
        this.version = version;
        this.title = title;
        this.contact = contact;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription( String description ) {

        this.description = description;
    }

    public String getVersion() {

        return version;
    }

    public void setVersion( String version ) {

        this.version = version;
    }

    public String getTitle() {

        return title;
    }

    public void setTitle( String title ) {

        this.title = title;
    }

    public SwaggerInfoContact getContact() {

        return contact;
    }

    public void setContact( SwaggerInfoContact contact ) {

        this.contact = contact;
    }

    public String toJson( boolean asStandaloneObject ) {

        StringBuilder sb = new StringBuilder();

        if (asStandaloneObject) {
            sb.append("{");
        }

        sb.append("\"")
          .append("info")
          .append("\"")
          .append(":")
          .append("{");

        sb.append("\"")
          .append("description")
          .append("\"")
          .append(":")
          .append("\"")
          .append(description)
          .append("\"")
          .append(",");

        sb.append("\"")
          .append("version")
          .append("\"")
          .append(":")
          .append("\"")
          .append(version)
          .append("\"")
          .append(",");

        sb.append("\"")
          .append("title")
          .append("\"")
          .append(":")
          .append("\"")
          .append(title)
          .append("\"")
          .append(",");

        sb.append(contact.toJson(false));
        
        sb.append("}");

        if (asStandaloneObject) {
            sb.append("}");
        }

        return sb.toString();

    }
}
