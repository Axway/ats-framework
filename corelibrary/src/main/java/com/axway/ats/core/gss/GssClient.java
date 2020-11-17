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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.io.HexDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

/**
 * This class is used for SPNEGO authentication available in the {@link com.axway.ats.action.http.HTTPClient} class.
 * This models the Kerberos client and manages the logging in to the Kerberos KDC to acquire the TGT. It also
 * performs the client-server authentication.
 */
public class GssClient {
    private String              clientPrincipalName;
    private String              clientPrincipalPassword;
    private File                clientKeytab;
    // The initiator subject. This object will hold the TGT
    // and all service tickets in its private credentials cache.    
    protected Subject           subject;

    private static final Logger log = LogManager.getLogger(GssClient.class);

    public GssClient( String clientPrincipalName, String clientPrincipalPassword, File keytab,
                      File krb5ConfFile ) {

        System.setProperty("java.security.krb5.conf", krb5ConfFile.toString());
        this.clientPrincipalName = clientPrincipalName;
        this.clientPrincipalPassword = clientPrincipalPassword;
        this.clientKeytab = keytab;
    }

    public String getName() {

        return clientPrincipalName;
    }

    /**
     * Login to Kerberos KDC and accquire a TGT.
     * 
     * @throws GSSException
     */
    private void loginViaJAAS() throws GSSException {

        try {
            JaasKerberosConfiguration config = createGssKerberosConfiguration();
            if (clientPrincipalPassword != null) {
                config.setPassword(clientPrincipalPassword.toCharArray());
            }
            if (clientKeytab != null) {
                config.setKeytab(clientKeytab.toString());
            }
            config.initialize();

            LoginContext loginContext = null;
            if (config.getCallbackHandler() != null) {
                loginContext = new LoginContext("other", config.getCallbackHandler());
            } else {
                loginContext = new LoginContext("other");
            }

            loginContext.login();

            // Subject will be populated with the Kerberos Principal name and the TGT.
            // Krb5LoginModule obtains a TGT (KerberosTicket) for the user either from the KDC
            // or from an existing ticket cache, and stores this TGT in the private credentials
            // set of a Subject             
            subject = loginContext.getSubject();

            log.debug("Logged in successfully as subject=\n" + subject.toString());

        } catch (LoginException e) {
            log.error(e);
            throw new GSSException(GSSException.DEFECTIVE_CREDENTIAL,
                                   GSSException.BAD_STATUS,
                                   "Kerberos client '" + clientPrincipalName
                                                            + "' failed to login to KDC. Error: "
                                                            + e.getMessage());
        }
    }

    public JaasKerberosConfiguration createGssKerberosConfiguration() {

        return new JaasKerberosConfiguration(clientPrincipalName);
    }

    /**
     * Called when SPNEGO client-service authentication is taking place.
     * 
     * @param context
     * @param negotiationToken
     * @return
     * @throws GSSException
     */
    public byte[] negotiate( GSSContext context, byte[] negotiationToken ) throws GSSException {

        if (subject == null) {
            loginViaJAAS(); // throw GSSException if fail to login
        }
        // If we do not have the service ticket it will be retrieved
        // from the TGS on a call to initSecContext().
        NegotiateContextAction negotiationAction = new NegotiateContextAction(context, negotiationToken);
        // Run the negotiation as the initiator
        // The service ticket will then be cached in the Subject's
        // private credentials, as the subject.
        negotiationToken = (byte[]) Subject.doAs(subject, negotiationAction);
        if (negotiationAction.getGSSException() != null) {
            throw negotiationAction.getGSSException();
        }

        return negotiationToken;
    }

    /**
     * Action to call initSecContext() for initiator side of context negotiation.
     * Run as the initiator Subject so that any service tickets are cached in 
     * the subject's private credentials. 
     */
    class NegotiateContextAction implements PrivilegedAction {
        private GSSContext   context;
        private byte[]       negotiationToken;
        private GSSException exception;

        public NegotiateContextAction( GSSContext context, byte[] negotiationToken ) {

            this.context = context;
            this.negotiationToken = negotiationToken;
        }

        public Object run() {

            try {
                // If we do not have the service ticket it will be retrieved
                // from the TGS on the first call to initSecContext(). The
                // subject's private credentials are checked for the service ticket.            
                // If we run this action as the initiator subject, the service ticket
                // will be stored in the subject's credentials and will not need
                // to be retrieved next time the client wishes to talk to the
                // server (acceptor).

                Subject subject = Subject.getSubject(AccessController.getContext());
                int beforeNumSubjectCreds = traceBeforeNegotiate();

                negotiationToken = context.initSecContext(negotiationToken, 0, negotiationToken.length);

                traceAfterNegotiate(beforeNumSubjectCreds);

            } catch (GSSException e) {
                // Trace out some info
                traceServiceTickets();
                exception = e;
            }

            return negotiationToken;
        }

        public GSSException getGSSException() {

            return exception;
        }

        private int traceBeforeNegotiate() {

            int beforeNumSubjectCreds = 0;
            // Traces all credentials too.
            if (subject != null) {
                log.debug("[" + getName() + "] AUTH_NEGOTIATE as subject " + subject.toString());
                beforeNumSubjectCreds = subject.getPrivateCredentials().size();
            }

            if (negotiationToken != null && negotiationToken.length > 0) {
                try {
                    OutputStream os = new ByteArrayOutputStream();
                    HexDump.dump(negotiationToken, 0, os, 0);
                    log.debug("[" + getName() + "] AUTH_NEGOTIATE Process token from acceptor==>\n"
                              + os.toString());
                } catch (IOException e) {}
            }

            return beforeNumSubjectCreds;
        }

        private void traceAfterNegotiate( int beforeNumSubjectCreds ) {

            if (subject != null) {
                int afterNumSubjectCreds = subject.getPrivateCredentials().size();
                if (afterNumSubjectCreds > beforeNumSubjectCreds) {
                    log.debug("[" + getName() + "] AUTH_NEGOTIATE have extra credentials.");
                    // Traces all credentials too.
                    log.debug("[" + getName() + "] AUTH_NEGOTIATE updated subject=" + subject.toString());
                }
            }

            if (negotiationToken != null && negotiationToken.length > 0) {
                try {
                    OutputStream os = new ByteArrayOutputStream();
                    HexDump.dump(negotiationToken, 0, os, 0);
                    log.debug("[" + getName() + "] AUTH_NEGOTIATE Send token to acceptor==>\n"
                              + os.toString());
                } catch (IOException e) {}
            }
        }

        public void traceServiceTickets() {

            if (subject == null)
                return;
            Set<Object> creds = subject.getPrivateCredentials();
            if (creds.size() == 0) {
                log.debug("[" + getName() + "] No service tickets");
            }

            synchronized (creds) {
                // The Subject's private credentials is a synchronizedSet
                // We must manually synchronize when iterating through the set.
                for (Object cred : creds) {
                    if (cred instanceof KerberosTicket) {
                        KerberosTicket ticket = (KerberosTicket) cred;
                        log.debug("[" + getName() + "] Service ticket " + "belonging to client principal ["
                                  + ticket.getClient().getName() + "] for server principal ["
                                  + ticket.getServer().getName() + "] End time=[" + ticket.getEndTime()
                                  + "] isCurrent=" + ticket.isCurrent());
                    }
                }
            }
        }
    }
}
