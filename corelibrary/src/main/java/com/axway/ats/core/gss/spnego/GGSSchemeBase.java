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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.ContextAwareAuthScheme;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.impl.auth.AuthSchemeBase;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.axway.ats.core.gss.GssClient;

/**
 * Modified original org.apache.http.impl.auth.GGSSchemeBase.
 * With these modifications we can specify any principals for the Kerberos client and 
 * service without the need to run kinit or have a login.conf file.
 * A krb5.conf is still needed.
 * Original code (on Windows) behaves like a browser for SPNEGO 
 * authentication where the client is the logged-in user
 * and the service principal is HTTP/host.com when the client
 * is talking to URL http://host.com/path.
 */
public abstract class GGSSchemeBase extends AuthSchemeBase {

    enum State {
        UNINITIATED, CHALLENGE_RECEIVED, TOKEN_GENERATED, FAILED,
    }

    private final Log    log = LogFactory.getLog(getClass());

    private final Base64 base64codec;

    /** Authentication process state */
    private State        state;

    /** base64 decoded challenge **/
    private byte[]       token;

    protected GssClient  gssClient;
    protected String     servicePrincipalName;
    protected Oid        servicePrincipalOid;

    GGSSchemeBase( GssClient gssClient,
                   String servicePrincipalName,
                   Oid servicePrincipalOid ) {

        super();
        this.base64codec = new Base64();
        this.state = State.UNINITIATED;
        this.gssClient = gssClient;
        this.servicePrincipalName = servicePrincipalName;
        this.servicePrincipalOid = servicePrincipalOid;
    }

    protected GSSManager getManager() {

        return GSSManager.getInstance();
    }

    protected byte[] generateGSSToken(
                                       final byte[] input,
                                       final Oid oid ) throws GSSException {

        byte[] token = input;
        if (token == null) {
            token = new byte[0];
        }
        GSSManager manager = getManager();

        GSSName serverName = manager.createName(servicePrincipalName, servicePrincipalOid);

        GSSContext gssContext = manager.createContext(serverName.canonicalize(oid),
                                                      oid,
                                                      null,
                                                      GSSContext.DEFAULT_LIFETIME);
        gssContext.requestMutualAuth(true);
        gssContext.requestCredDeleg(true);
        // Get client to login if not already done
        return gssClient.negotiate(gssContext, token);
    }

    protected abstract byte[] generateToken(
                                             byte[] input ) throws GSSException;

    public boolean isComplete() {

        return this.state == State.TOKEN_GENERATED || this.state == State.FAILED;
    }

    /**
     * @deprecated (4.2) Use {@link ContextAwareAuthScheme#authenticate(Credentials, HttpRequest, org.apache.http.protocol.HttpContext)}
     */
    @Deprecated
    public Header authenticate(
                                final Credentials credentials,
                                final HttpRequest request ) throws AuthenticationException {

        return authenticate(credentials, request, null);
    }

    @Override
    public Header authenticate(
                                final Credentials credentials,
                                final HttpRequest request,
                                final HttpContext context ) throws AuthenticationException {

        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        switch (state) {
            case UNINITIATED:
                throw new AuthenticationException(getSchemeName() + " authentication has not been initiated");
            case FAILED:
                throw new AuthenticationException(getSchemeName() + " authentication has failed");
            case CHALLENGE_RECEIVED:
                try {
                    token = generateToken(token);
                    state = State.TOKEN_GENERATED;
                } catch (GSSException gsse) {
                    state = State.FAILED;
                    if (gsse.getMajor() == GSSException.DEFECTIVE_CREDENTIAL
                        || gsse.getMajor() == GSSException.CREDENTIALS_EXPIRED)
                        throw new InvalidCredentialsException(gsse.getMessage(), gsse);
                    if (gsse.getMajor() == GSSException.NO_CRED)
                        throw new InvalidCredentialsException(gsse.getMessage(), gsse);
                    if (gsse.getMajor() == GSSException.DEFECTIVE_TOKEN
                        || gsse.getMajor() == GSSException.DUPLICATE_TOKEN
                        || gsse.getMajor() == GSSException.OLD_TOKEN)
                        throw new AuthenticationException(gsse.getMessage(), gsse);
                    // other error
                    throw new AuthenticationException(gsse.getMessage());
                }
                // continue to next case block
            case TOKEN_GENERATED:
                String tokenstr = new String(base64codec.encode(token));
                if (log.isDebugEnabled()) {
                    log.debug("Sending response '" + tokenstr + "' back to the auth server");
                }
                return new BasicHeader("Authorization", "Negotiate " + tokenstr);
            default:
                throw new IllegalStateException("Illegal state: " + state);
        }
    }

    @Override
    protected void parseChallenge(
                                   final CharArrayBuffer buffer,
                                   int beginIndex,
                                   int endIndex ) throws MalformedChallengeException {

        String challenge = buffer.substringTrimmed(beginIndex, endIndex);
        if (log.isDebugEnabled()) {
            log.debug("Received challenge '" + challenge + "' from the auth server");
        }
        if (state == State.UNINITIATED) {
            token = base64codec.decode(challenge.getBytes());
            state = State.CHALLENGE_RECEIVED;
        } else {
            log.debug("Authentication already attempted");
            state = State.FAILED;
        }
    }
}
