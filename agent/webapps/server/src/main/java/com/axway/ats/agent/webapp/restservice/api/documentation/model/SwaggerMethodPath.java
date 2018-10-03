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

public class SwaggerMethodPath {

    private String                           classUrl;
    private String                           url;
    private List<SwaggerMethodPathOperation> operations;

    public SwaggerMethodPath() {

        this.operations = new ArrayList<>();

    }

    public List<SwaggerMethodPathOperation> getOperations() {

        return operations;
    }

    public void setOperations( List<SwaggerMethodPathOperation> operations ) {

        this.operations = operations;
    }

    public void addOperation( SwaggerMethodPathOperation operation ) {

        this.operations.add(operation);
    }

    public String getClassUrl() {

        return classUrl;
    }

    public void setClassUrl( String classUrl ) {

        this.classUrl = classUrl;
    }

    public String getUrl() {

        return url;
    }

    public void setUrl( String url ) {

        this.url = url;
    }

    public String toJson( boolean asStandaloneObject ) {

        StringBuilder sb = new StringBuilder();

        if (asStandaloneObject) {
            sb.append("{");
        }

        String finalUrl = null;

        if (!classUrl.startsWith("/")) {
            finalUrl = "/" + classUrl;
        } else {
            finalUrl = classUrl;
        }

        if (classUrl.endsWith("/")) {
            if (url.startsWith("/")) {
                finalUrl += url.substring(1, url.length());
            } else {
                finalUrl += url;
            }
        } else {
            if (url.startsWith("/")) {
                finalUrl += url;
            } else {
                finalUrl += "/" + url;
            }
        }

        sb.append("\"")
          .append(finalUrl)
          .append("\"")
          .append(":")
          .append("{");

        if (operations != null && operations.size() > 0) {
            for (SwaggerMethodPathOperation operation : operations) {
                sb.append(operation.toJson(false)).append(",");
            }
            sb.setLength(sb.length() - 1);
        }

        sb.append("}");

        if (asStandaloneObject) {
            sb.append("}");
        }

        return sb.toString();

    }

}
