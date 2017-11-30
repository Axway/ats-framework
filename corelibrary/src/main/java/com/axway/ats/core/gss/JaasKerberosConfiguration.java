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
package com.axway.ats.core.gss;

import java.io.IOException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * This class removes the need for a jaas.conf file to configure the 
 * com.sun.security.auth.module.Krb5LoginModule to be used for
 * JAAS login for Kerberos client (initiators).
 */
public class JaasKerberosConfiguration extends Configuration {
    private static final String       LOGIN_MODULE = "com.sun.security.auth.module.Krb5LoginModule";

    private AppConfigurationEntry[]   appConfigEntries;

    private String                    principalName;
    private CredentialCallbackHandler cb;

    private Map<String, String>       options      = new HashMap<String, String>();

    public JaasKerberosConfiguration( String principalName ) {

        this.principalName = principalName;
    }

    public void setPassword(
                             char[] password ) {

        cb = new CredentialCallbackHandler(principalName, password);
    }

    public void setKeytab(
                           String keytabFilename ) {

        options.put("useKeyTab", "true");
        options.put("keyTab", keytabFilename);
        options.put("doNotPrompt", "true");
    }

    public void initialize() {

        options.put("debug", "true");

        options.put("principal", principalName); // Ensure the correct TGT is used.
        options.put("refreshKrb5Config", "true");

        appConfigEntries = new AppConfigurationEntry[1];
        appConfigEntries[0] = new AppConfigurationEntry(LOGIN_MODULE,
                                                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                                                        options);

        Security.setProperty("login.configuration.provider", getClass().getName());

        // For each Kerberos client that the loads we 
        // need a separate instance of this class, it gets set here, so next call
        // on the LoginContext will use this instance.
        setConfiguration(this);
    }

    public AppConfigurationEntry[] getAppConfigurationEntry(
                                                             String arg0 ) {

        return appConfigEntries;
    }

    public CallbackHandler getCallbackHandler() {

        return cb;
    }
}

class CredentialCallbackHandler implements CallbackHandler {
    String username;
    char[] password;

    public CredentialCallbackHandler( String username,
                                      char[] password ) {

        this.username = username;
        this.password = password;
    }

    public void handle(
                        Callback[] callbacks ) throws IOException, UnsupportedCallbackException {

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback) callbacks[i];
                nc.setName(username);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callbacks[i];
                pc.setPassword(password);
            }
        }
    }
}
