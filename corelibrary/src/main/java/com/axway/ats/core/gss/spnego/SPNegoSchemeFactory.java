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

import org.apache.http.annotation.Immutable;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.axway.ats.core.gss.GssClient;

/**
 * {@link AuthSchemeProvider} implementation that creates and initializes
 * {@link SPNegoScheme} instances.
 *
 * Modified original org.apache.http.impl.auth.SPNegoSchemeFactory.
 * With these modifications we can specify any principals for the Kerberos client and 
 * service without the need to run kinit or have a login.conf file.
 * A krb5.conf is still needed.
 * Original code (on Windows) behaves like a browser for SPNEGO 
 * authentication where the client is the logged-in user
 * and the service principal is HTTP/host.com when the client
 * is talking to URL http://host.com/path.
 */
@Immutable
@SuppressWarnings( "deprecation")
public class SPNegoSchemeFactory implements AuthSchemeFactory, AuthSchemeProvider {
    private GssClient gssClient;
    private String    servicePrincipalName;
    private Oid       servicePrincipalOid;

    public SPNegoSchemeFactory( GssClient gssClient,
                                String servicePrincipalName,
                                String servicePrincipalType ) {

        this.gssClient = gssClient;
        this.servicePrincipalName = servicePrincipalName;
        this.servicePrincipalOid = getOidForType(servicePrincipalType);
    }

    public AuthScheme newInstance(
                                   final HttpParams params ) {

        return new SPNegoScheme(gssClient, servicePrincipalName, servicePrincipalOid);
    }

    public AuthScheme create(
                              final HttpContext context ) {

        return new SPNegoScheme(gssClient, servicePrincipalName, servicePrincipalOid);
    }

    private Oid getOidForType(
                               String type ) {

        if ("NT_USER_NAME".equals(type)) {
            return GSSName.NT_USER_NAME;
        } else if ("NT_HOSTBASED_SERVICE".equals(type)) {
            return GSSName.NT_HOSTBASED_SERVICE;
        } else if ("NT_MACHINE_UID_NAME".equals(type)) {
            return GSSName.NT_MACHINE_UID_NAME;
        } else if ("NT_STRING_UID_NAME".equals(type)) {
            return GSSName.NT_STRING_UID_NAME;
        } else if ("NT_ANONYMOUS".equals(type)) {
            return GSSName.NT_ANONYMOUS;
        } else if ("NT_EXPORT_NAME".equals(type)) {
            return GSSName.NT_EXPORT_NAME;
        }
        return GSSName.NT_USER_NAME;
    }
}
