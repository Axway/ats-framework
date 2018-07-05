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

public class SwaggerInfoContact {
    private String name;
    private String url;
    private String email;

    public SwaggerInfoContact() {

        super();
    }

    public SwaggerInfoContact( String name,
                    String url,
                    String email ) {

        super();

        this.name = name;
        this.url = url;
        this.email = email;
    }

    public String getName() {

        return name;
    }

    public void setName( String name ) {

        this.name = name;
    }

    public String getUrl() {

        return url;
    }

    public void setUrl( String url ) {

        this.url = url;
    }

    public String getEmail() {

        return email;
    }

    public void setEmail( String email ) {

        this.email = email;
    }

    public String toJson( boolean asStandaloneObject ) {

        StringBuilder sb = new StringBuilder();

        if (asStandaloneObject) {
            sb.append("{");
        }

        sb.append("\"")
          .append("contact")
          .append("\"")
          .append(":")
          .append("{");

        sb.append("\"")
          .append("name")
          .append("\"")
          .append(":")
          .append("\"")
          .append(name)
          .append("\"")
          .append(",");

        sb.append("\"")
          .append("url")
          .append("\"")
          .append(":")
          .append("\"")
          .append(url)
          .append("\"")
          .append(",");

        sb.append("\"")
          .append("email")
          .append("\"")
          .append(":")
          .append("\"")
          .append(email)
          .append("\"");

        sb.append("}");

        if (asStandaloneObject) {
            sb.append("}");
        }

        return sb.toString();

    }
}
