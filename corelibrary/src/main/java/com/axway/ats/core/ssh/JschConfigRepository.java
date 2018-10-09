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
package com.axway.ats.core.ssh;

import java.util.Map;
import java.util.Map.Entry;

import com.jcraft.jsch.ConfigRepository;

public class JschConfigRepository implements ConfigRepository {

    private String              host;
    private String              userName;
    private int                 port;
    // some optional configuration properties
    private Map<String, String> configurationProperties;

    public JschConfigRepository( String host, String userName, int port, Map<String, String> configurationProperties ) {

        this.host = host;
        this.userName = userName;
        this.port = port;
        this.configurationProperties = configurationProperties;
    }

    @Override
    public Config getConfig( String host ) {

        return new JschDefaultConfig();
    }

    class JschDefaultConfig implements Config {

        @Override
        public String getHostname() {
            return host;
        }

        @Override
        public String getUser() {
            return userName;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public String getValue( String key ) {
            String value = null;
            for (Entry<String, String> entry : configurationProperties.entrySet()) {
                String aKey = null;
                boolean isGlobalKey = false;
                boolean isSessionKey = false;
                if (entry.getKey().startsWith("global.")) {
                    isGlobalKey = true;
                    aKey = entry.getKey().split("\\.")[1];
                } else if (entry.getKey().startsWith("session.")) {
                    isSessionKey = true;
                    aKey = entry.getKey().split("\\.")[1];
                } else {
                    aKey = entry.getKey();
                }

                if (aKey.equals(key)) {
                    if (isGlobalKey) {
                        value = configurationProperties.get("global." + key);
                    } else if (isSessionKey) {
                        value = configurationProperties.get("session." + key);
                    } else {
                        value = configurationProperties.get(key);
                    }
                }
            }
            return value;
        }

        @Override
        public String[] getValues( String key ) {
            String value = null;
            for (Entry<String, String> entry : configurationProperties.entrySet()) {
                String aKey = null;
                boolean isGlobalKey = false;
                boolean isSessionKey = false;
                if (entry.getKey().startsWith("global.") || entry.getKey().startsWith("session.")) {
                    aKey = entry.getKey().split("\\.")[1];
                } else {
                    aKey = entry.getKey();
                }

                if (aKey.equals(key)) {
                    if (isGlobalKey) {
                        value = configurationProperties.get("global." + key);
                    } else if (isSessionKey) {
                        value = configurationProperties.get("session." + key);
                    } else {
                        value = configurationProperties.get(key);
                    }
                }
            }

            if (value == null) {
                return new String[]{};
            } else {
                return new String[]{ value };
            }
        }
    }
}
