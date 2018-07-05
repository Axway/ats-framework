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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.axway.ats.agent.webapp.restservice.api.documentation.exceptions.SwaggerDocumentationException;

public class SwaggerDocument {

    public static final String     SWAGGER_DOCUMENTATION_VERSION = "2.0";

    private SwaggerInfo            info;
    private List<SwaggerTag>       tags;
    private List<String>           schemes;
    private Set<SwaggerMethodPath> paths;
    private String                 host;
    private String                 basePath;
    private Set<SwaggerDefinition> definitions;

    public SwaggerDocument() {

        tags = new ArrayList<>();
        schemes = new ArrayList<>();
        schemes.add("http"); // this is hard-coded since we are only interested in that (HTTP) scheme
        paths = new HashSet<>();
        definitions = new HashSet<>();
    }

    public String getHost() {

        return host;
    }

    public void setHost( String host ) {

        this.host = host;
    }

    public String getBasePath() {

        return basePath;
    }

    public void setBasePath( String basePath ) {

        this.basePath = basePath;
    }

    public List<String> getSchemes() {

        return schemes;
    }

    public Set<SwaggerMethodPath> getPaths() {

        return paths;
    }

    public void setPaths( Set<SwaggerMethodPath> paths ) {

        this.paths = paths;
    }

    public void addPath( SwaggerMethodPath path ) {

        paths.add(path);
    }

    public SwaggerInfo getInfo() {

        return info;
    }

    public void setInfo( SwaggerInfo info ) {

        this.info = info;
    }

    public List<SwaggerTag> getTags() {

        return tags;
    }

    public void setTags( List<SwaggerTag> tags ) {

        this.tags = tags;
    }

    public void addTag( SwaggerTag swaggerTag ) {

        tags.add(swaggerTag);
    }

    public Set<SwaggerDefinition> getDefinitions() {

        return definitions;
    }

    public void setDefinitions( Set<SwaggerDefinition> definitions ) {

        this.definitions = definitions;
    }

    public void addDefinition( SwaggerDefinition definition ) {

        this.definitions.add(definition);
    }

    /**
     * See if path, pointing to the provided URL already exists in that document
     * @param string 
     * */
    public boolean hasPath( String classUrl, String url ) {

        for (SwaggerMethodPath path : paths) {
            if (path.getClassUrl().equals(classUrl) && path.getUrl().equals(url)) {
                return true;
            }
        }

        return false;

    }

    public SwaggerMethodPath getPath( String classUrl, String url ) {

        for (SwaggerMethodPath path : paths) {
            if (path.getClassUrl().equals(classUrl) && path.getUrl().equals(url)) {
                return path;
            }
        }
        throw new SwaggerDocumentationException("No method path from class with url '" + classUrl + "', pointing to '"
                                                + url + "' exists");
    }

    public String toJson() {

        StringBuilder sb = new StringBuilder();

        sb.append("{");

        // add swagger documentation version
        sb.append("\"")
          .append("swagger")
          .append("\"")
          .append(":")
          .append("\"")
          .append(SWAGGER_DOCUMENTATION_VERSION)
          .append("\"")
          .append(",");

        // add SwaggerInfo
        sb.append(info.toJson(false)).append(",");

        // add host
        sb.append("\"")
          .append("host")
          .append("\"")
          .append(":")
          .append("\"")
          .append(host)
          .append("\"")
          .append(",");

        // add basePath
        sb.append("\"")
          .append("basePath")
          .append("\"")
          .append(":")
          .append("\"")
          .append(basePath)
          .append("\"")
          .append(",");

        // add tags

        if (tags != null && tags.size() > 0) {
            sb.append("\"")
              .append("tags")
              .append("\"")
              .append(": [");

            for (SwaggerTag tag : tags) {
                sb.append(tag.toJson(true)).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("],");

        } else {
            sb.append("\"")
              .append("tags")
              .append("\"")
              .append(":")
              .append("[]")
              .append(",");
        }

        // add schemes
        if (schemes != null && schemes.size() > 0) {
            sb.append("\"").append("schemes").append("\"").append(":").append("[");
            for (String scheme : schemes) {
                sb.append("\"").append(scheme).append("\"").append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]").append(",");
        } else {
            sb.append("\"").append("schemes").append("\"").append(":").append("[]").append(",");
        }

        // add paths
        if (paths != null && paths.size() > 0) {
            sb.append("\"").append("paths").append("\"").append(": {");
            for (SwaggerMethodPath path : paths) {
                sb.append(path.toJson(false)).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("}").append(",");
        } else {
            sb.append("\"").append("paths").append("\"").append(":").append("{}").append(",");
        }

        // add definitions
        sb.append("\"")
          .append("definitions")
          .append("\"")
          .append(":")
          .append("{");

        if (definitions.size() > 0) {
            for (SwaggerDefinition definition : definitions) {
                sb.append(definition.toJson(false)).append(",");
            }
            sb.setLength(sb.length() - 1);
        }

        sb.append("}");

        sb.append("}");

        return sb.toString();
    }

}
