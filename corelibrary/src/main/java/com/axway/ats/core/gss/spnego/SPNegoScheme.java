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
package com.axway.ats.core.gss.spnego;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.protocol.HttpContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

import com.axway.ats.core.gss.GssClient;

/**
 * SPNEGO (Simple and Protected GSSAPI Negotiation Mechanism) authentication
 * scheme.
 * Modified original org.apache.http.impl.auth.SPNegoScheme.
 * With these modifications we can specify any principals for the Kerberos client and 
 * service without the need to run kinit or have a login.conf file.
 * A krb5.conf is still needed.
 * Original code (on Windows) behaves like a browser for SPNEGO 
 * authentication where the client is the logged-in user
 * and the service principal is HTTP/host.com when the client
 * is talking to URL http://host.com/path.
 */
public class SPNegoScheme extends GGSSchemeBase {

    private static final String SPNEGO_OID = "1.3.6.1.5.5.2";

    public SPNegoScheme( GssClient client,
                         String servicePrincipalName,
                         Oid servicePrincipalOid ) {

        super(client, servicePrincipalName, servicePrincipalOid);
    }

    public String getSchemeName() {

        return "Negotiate";
    }

    /**
     * Produces SPNEGO authorization Header based on token created by
     * processChallenge.
     *
     * @param credentials not used by the SPNEGO scheme.
     * @param request The request being authenticated
     *
     * @throws AuthenticationException if authentication string cannot
     *   be generated due to an authentication failure
     *
     * @return SPNEGO authentication Header
     */
    @Override
    public Header authenticate(
                                final Credentials credentials,
                                final HttpRequest request,
                                final HttpContext context ) throws AuthenticationException {

        return super.authenticate(credentials, request, context);
    }

    @Override
    protected byte[] generateToken(
                                    final byte[] input ) throws GSSException {

        return generateGSSToken(input, new Oid(SPNEGO_OID));
    }

    /**
     * There are no valid parameters for SPNEGO authentication so this
     * method always returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public String getParameter(
                                String name ) {

        if (name == null) {
            throw new IllegalArgumentException("Parameter name may not be null");
        }
        return null;
    }

    /**
     * The concept of an authentication realm is not supported by the Negotiate
     * authentication scheme. Always returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public String getRealm() {

        return null;
    }

    /**
     * Returns <tt>true</tt>. SPNEGO authentication scheme is connection based.
     *
     * @return <tt>true</tt>.
     */
    public boolean isConnectionBased() {

        return true;
    }

}
