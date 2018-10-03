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

public class SwaggerMethodPathOperation {

    private List<String>                 tags;
    private String                       httpMethod;
    private String                       summary;
    private String                       description;
    private String                       operationId;
    private List<String>                 consumes;
    private List<String>                 produces;
    private List<SwaggerMethodParameter> parameters;
    private List<SwaggerMethodResponse>  responses;

    public SwaggerMethodPathOperation() {

        this.parameters = new ArrayList<>();
        this.responses = new ArrayList<>();
    }

    public List<String> getTags() {

        return tags;
    }

    public void setTags( List<String> tags ) {

        this.tags = tags;
    }

    public String getHttpMethod() {

        return httpMethod;
    }

    public void setHttpMethod( String httpMethod ) {

        this.httpMethod = httpMethod;
    }

    public String getSummary() {

        return summary;
    }

    public void setSummary( String summary ) {

        this.summary = summary;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription( String description ) {

        this.description = description;
    }

    public String getOperationId() {

        return operationId;
    }

    public void setOperationId( String operationId ) {

        this.operationId = operationId;
    }

    public List<String> getConsumes() {

        return consumes;
    }

    public void setConsumes( List<String> consumes ) {

        this.consumes = consumes;
    }

    public List<String> getProduces() {

        return produces;
    }

    public void setProduces( List<String> produces ) {

        this.produces = produces;
    }

    public List<SwaggerMethodParameter> getParameters() {

        return parameters;
    }

    public void setParameters( List<SwaggerMethodParameter> parameters ) {

        this.parameters = parameters;
    }

    public void addParameter( SwaggerMethodParameter swaggerMethodParameter ) {

        this.parameters.add(swaggerMethodParameter);

    }

    public List<SwaggerMethodResponse> getResponses() {

        return responses;
    }

    public void setResponses( List<SwaggerMethodResponse> responses ) {

        this.responses = responses;
    }

    public void addResponse( SwaggerMethodResponse response ) {

        this.responses.add(response);
    }

    public String toJson( boolean asStandaloneObject ) {

        StringBuilder sb = new StringBuilder();

        if (asStandaloneObject) {
            sb.append("{");
        }

        sb.append("\"")
          .append(httpMethod)
          .append("\"")
          .append(":")
          .append("{");

        if (tags != null && tags.size() > 0) {
            sb.append("\"")
              .append("tags")
              .append("\"")
              .append(":")
              .append("[");

            for (String tag : tags) {
                sb.append("\"").append(tag).append("\"").append(",");
            }

            sb.setLength(sb.length() - 1);

            sb.append("]").append(",");

        } else {
            sb.append("\"")
              .append("tags")
              .append("\"")
              .append(":")
              .append("[]")
              .append(",");
        }

        sb.append("\"")
          .append("summary")
          .append("\"")
          .append(":")
          .append("\"")
          .append(summary)
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
          .append("operationId")
          .append("\"")
          .append(":")
          .append("\"")
          .append(operationId)
          .append("\"")
          .append(",");

        if (consumes != null && consumes.size() > 0) {
            sb.append("\"")
              .append("consumes")
              .append("\"")
              .append(":")
              .append("[");
            for (String consume : consumes) {
                sb.append("\"").append(consume).append("\"").append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]")
              .append(",");

        } else {
            sb.append("\"")
              .append("consumes")
              .append("\"")
              .append(":")
              .append("[")
              .append("]")
              .append(",");
        }

        if (produces != null && produces.size() > 0) {
            sb.append("\"")
              .append("produces")
              .append("\"")
              .append(":")
              .append("[");
            for (String produce : produces) {
                sb.append("\"").append(produce).append("\"").append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]")
              .append(",");

        } else {
            sb.append("\"")
              .append("produces")
              .append("\"")
              .append(":")
              .append("[]")
              .append(",");
        }

        if (parameters != null && parameters.size() > 0) {
            sb.append("\"")
              .append("parameters")
              .append("\"")
              .append(":")
              .append("[");
            for (SwaggerMethodParameter parameter : parameters) {
                sb.append(parameter.toJson(true)).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]")
              .append(",");
        } else {
            sb.append("\"")
              .append("parameters")
              .append("\"")
              .append(":")
              .append("[]")
              .append(",");
        }

        if (responses != null && responses.size() > 0) {
            sb.append("\"")
              .append("responses")
              .append("\"")
              .append(":")
              .append("{");
            for (SwaggerMethodResponse response : responses) {
                sb.append(response.toJson(false)).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("}");
        } else {
            sb.append("\"")
              .append("responses")
              .append("\"")
              .append(":")
              .append("{}");
        }

        sb.append("}");

        if (asStandaloneObject) {
            sb.append("}");
        }

        return sb.toString();

    }

}
