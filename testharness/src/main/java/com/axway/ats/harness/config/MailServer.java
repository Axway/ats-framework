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
package com.axway.ats.harness.config;

public class MailServer extends Box {

    private static final long serialVersionUID = 1L;

    private String            host;
    private String            defaultPass;

    /**
     * @return the host
     */
    public String getHost() {

        return host;
    }

    /**
     * @param host the host to set
     */
    void setHost(
                  String host ) {

        verifyNotNullNorEmptyParameter( "host", host );
        this.host = host;
    }

    /**
     * @return the defaultPass
     */
    public String getDefaultPass() {

        return defaultPass;
    }

    /**
     * @param defaultPass the defaultPass to set
     */
    void setDefaultPass(
                         String defaultPass ) {

        verifyNotNullNorEmptyParameter( "defaultPass", defaultPass );
        this.defaultPass = defaultPass;
    }

    @Override
    public MailServer newCopy() {

        MailServer newBox = new MailServer();

        newBox.host = this.host;
        newBox.defaultPass = this.defaultPass;

        newBox.properties = this.getNewProperties();

        return newBox;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append( "Mail Server: host = " + host );
        sb.append( ", dafault pass = " + defaultPass );
        sb.append( super.toString() );

        return sb.toString();
    }
}
