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
package com.axway.ats.action.model;

import gnu.inet.smtp.ParameterList;
import gnu.inet.util.BASE64;
import gnu.inet.util.CRLFInputStream;
import gnu.inet.util.CRLFOutputStream;
import gnu.inet.util.EmptyX509TrustManager;
import gnu.inet.util.LineInputStream;
import gnu.inet.util.MessageOutputStream;
import gnu.inet.util.SaslCallbackHandler;
import gnu.inet.util.SaslCramMD5;
import gnu.inet.util.SaslInputStream;
import gnu.inet.util.SaslLogin;
import gnu.inet.util.SaslOutputStream;
import gnu.inet.util.SaslPlain;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.exceptions.SmtpException;

/**
 * An SMTP client.
 * This implements RFC 2821.
 *
 */
public class InetSmtpConnection {

    /**
     * The default SMTP port.
     */
    public static final int       DEFAULT_PORT  = 25;

    protected static final String MAIL_FROM     = "MAIL FROM:";
    protected static final String RCPT_TO       = "RCPT TO:";
    protected static final String SP            = " ";
    protected static final String DATA          = "DATA";
    protected static final String FINISH_DATA   = "\n.";
    protected static final String RSET          = "RSET";
    protected static final String VRFY          = "VRFY";
    protected static final String EXPN          = "EXPN";
    protected static final String HELP          = "HELP";
    protected static final String NOOP          = "NOOP";
    protected static final String QUIT          = "QUIT";
    protected static final String HELO          = "HELO";
    protected static final String EHLO          = "EHLO";
    protected static final String AUTH          = "AUTH";
    protected static final String STARTTLS      = "STARTTLS";

    protected static final int    INFO          = 214;
    protected static final int    READY         = 220;
    protected static final int    OK            = 250;
    protected static final int    OK_NOT_LOCAL  = 251;
    protected static final int    OK_UNVERIFIED = 252;
    protected static final int    SEND_DATA     = 354;
    protected static final int    AMBIGUOUS     = 553;

    /**
     * The logger used for SMTP protocol traces.
     */
    public final Logger           log           = LogManager.getLogger(this.getClass());

    /**
     * The underlying socket used for communicating with the server.
     */
    protected Socket              socket;

    /**
     * The input stream used to read responses from the server.
     */
    protected LineInputStream     in;

    /**
     * The output stream used to send commands to the server.
     */
    protected CRLFOutputStream    out;

    /**
     * The last response message received from the server.
     */
    protected String              response;

    /**
     * The last response code received from the server.
     */
    protected int                 responseCode;

    /**
     * If true, there are more responses to read.
     */
    protected boolean             continuation;

    /**
     * The greeting message given by the server.
     */
    protected String              greeting;

    /**
     * Creates a new connection to the specified host, using the default SMTP
     * port.
     * @param host the server hostname
     */
    public InetSmtpConnection( String host ) throws IOException {

        this(host, DEFAULT_PORT, null, 0, 0, false, null);
    }

    /**
     * Creates a new connection to the specified host, using the specified
     * port.
     * @param host the server hostname
     * @param port the port to connect to
     */
    public InetSmtpConnection( String host,
                               int port ) throws IOException {

        this(host, port, null, 0, 0, false, null);
    }

    /**
     * Creates a new connection to the specified host, using the specified
     * port.
     * @param host the server hostname
     * @param port the port to connect to
     * @param bindAddress the local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     */
    public InetSmtpConnection( String host,
                               int port,
                               String bindAddress ) throws IOException {

        this(host, port, bindAddress, 0, 0, false, null);
    }

    /**
     * Creates a new connection to the specified host, using the specified
     * port.
     * @param host the server hostname
     * @param port the port to connect to
     * @param bindAddress the local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     * @param connectionTimeout the connection timeout in milliseconds
     * @param timeout the I/O timeout in milliseconds
     */
    public InetSmtpConnection( String host,
                               int port,
                               String bindAddress,
                               int connectionTimeout,
                               int timeout ) throws IOException {

        this(host, port, bindAddress, connectionTimeout, timeout, false, null);
    }

    /**
     * Creates a new connection to the specified host, using the specified
     * port.
     * @param host the server hostname
     * @param port the port to connect to
     * @param bindAddress the local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     * @param connectionTimeout the connection timeout in milliseconds
     * @param timeout the I/O timeout in milliseconds
     * @param secure true to create an SMTPS connection
     * @param tm a trust manager used to check SSL certificates, or null to
     * use the default
     */
    public InetSmtpConnection( String host,
                               int port,
                               String bindAddress,
                               int connectionTimeout,
                               int timeout,
                               boolean secure,
                               TrustManager tm ) throws IOException {

        this(host, port, bindAddress, connectionTimeout, timeout, secure, tm, true);
    }

    /**
     * Creates a new connection to the specified host, using the specified
     * port.
     * @param host the server hostname
     * @param port the port to connect to
     * @param bindAddress the local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     * @param connectionTimeout the connection timeout in milliseconds
     * @param timeout the I/O timeout in milliseconds
     * @param secure true to create an SMTPS connection
     * @param tm a trust manager used to check SSL certificates, or null to
     * use the default
     * @param init initialize the connection
     */
    public InetSmtpConnection( String host,
                               int port,
                               String bindAddress,
                               int connectionTimeout,
                               int timeout,
                               boolean secure,
                               TrustManager tm,
                               boolean init ) throws IOException {

        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        response = null;
        continuation = false;

        // Initialize socket
        try {
            socket = new Socket();
            if (bindAddress != null && !bindAddress.isEmpty()) {
                socket.setReuseAddress(true);
                // In case of exceptions or lost data we may try with setting small
                // but positive socket linger timeout like 2-3 seconds.
                // Noted in http://www.rgagnon.com/javadetails/java-0293.html
                socket.setSoLinger(true, 0); // prevent socket TIME_WAIT state.
                socket.bind(new InetSocketAddress(bindAddress, 0));
            }
            InetSocketAddress address = new InetSocketAddress(host, port);
            if (connectionTimeout > 0) {
                socket.connect(address, connectionTimeout);
            } else {
                socket.connect(address);
            }
            if (timeout > 0) {
                socket.setSoTimeout(timeout);
            }
            if (secure) {
                SSLSocketFactory factory = getSSLSocketFactory(tm);
                SSLSocket ss = (SSLSocket) factory.createSocket(socket, host, port, true);
                String[] protocols = { "TLSv1", "SSLv3" };
                ss.setEnabledProtocols(protocols);
                ss.setUseClientMode(true);
                ss.startHandshake();
                socket = ss;
            }
        } catch (GeneralSecurityException e) {
            IOException e2 = new IOException();
            e2.initCause(e);
            throw e2;
        }

        // Initialize streams
        InputStream is = socket.getInputStream();
        is = new BufferedInputStream(is);
        is = new CRLFInputStream(is);
        in = new LineInputStream(is);
        OutputStream os = socket.getOutputStream();
        os = new BufferedOutputStream(os);
        out = new CRLFOutputStream(os);

        if (init)
            init();
    }

    /**
     * Initializes the connection.
     * Unless the init parameter was provided with the value false,
     * do not call this method. Otherwise call it only once after e.g.
     * configuring logging.
     */
    public void init() throws IOException {

        // Greeting
        StringBuffer greetingBuffer = new StringBuffer();
        boolean notFirst = false;
        do {
            if (getResponse() != READY) {
                throw new SmtpException(response, responseCode);
            }
            if (notFirst) {
                greetingBuffer.append('\n');
            } else {
                notFirst = true;
            }
            greetingBuffer.append(response);

        } while (continuation);
        greeting = greetingBuffer.toString();
    }

    /**
     * Returns the server greeting message.
     */
    public String getGreeting() {

        return greeting;
    }

    /**
     * Returns the text of the last response received from the server.
     */
    public String getLastResponse() {

        return response;
    }

    /**
     * Returns the code of the last response received from the server.
     */
    public int getLastResponseCode() {

        return responseCode;
    }

    // -- 3.3 Mail transactions --

    /**
     * Execute a MAIL command.
     * @param reversePath the source mailbox(from address)
     * @param parameters optional ESMTP parameters
     * @return true if accepted, false otherwise
     */
    public boolean mailFrom(
                             String reversePath,
                             ParameterList parameters ) throws IOException {

        StringBuffer command = new StringBuffer(MAIL_FROM);
        command.append('<');
        command.append(reversePath);
        command.append('>');
        if (parameters != null) {
            command.append(SP);
            command.append(parameters);
        }
        send(command.toString());
        switch (getAllResponses()) {
            case OK:
            case OK_NOT_LOCAL:
            case OK_UNVERIFIED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Execute a RCPT command.
     * @param forwardPath the forward-path(recipient address)
     * @param parameters optional ESMTP parameters
     * @return true if successful, false otherwise
     */
    public boolean rcptTo(
                           String forwardPath,
                           ParameterList parameters ) throws IOException {

        StringBuffer command = new StringBuffer(RCPT_TO);
        command.append('<');
        command.append(forwardPath);
        command.append('>');
        if (parameters != null) {
            command.append(SP);
            command.append(parameters);
        }
        send(command.toString());
        switch (getAllResponses()) {
            case OK:
            case OK_NOT_LOCAL:
            case OK_UNVERIFIED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Requests an output stream to write message data to.
     * When the entire message has been written to the stream, the
     * <code>flush</code> method must be called on the stream. Until then no
     * further methods should be called on the connection.
     * Immediately after this procedure is complete, <code>finishData</code>
     * must be called to complete the transfer and determine its success.
     * @return a stream for writing messages to
     */
    public OutputStream data() throws IOException {

        send(DATA);
        switch (getAllResponses()) {
            case SEND_DATA:
                return new MessageOutputStream(out);
            default:
                throw new SmtpException(response, responseCode);
        }
    }

    /**
     * Completes the DATA procedure.
     * @see #data
     * @return true id transfer was successful, false otherwise
     */
    public boolean finishData() throws IOException {

        send(FINISH_DATA);
        switch (getAllResponses()) {
            case OK:
                return true;
            default:
                return false;
        }
    }

    /**
     * Aborts the current mail transaction.
     */
    public void rset() throws IOException {

        send(RSET);
        if (getAllResponses() != OK) {
            throw new SmtpException(response, responseCode);
        }
    }

    // -- 3.5 Commands for Debugging Addresses --

    /**
     * Returns a list of valid possibilities for the specified address, or
     * null on failure.
     * @param address a mailbox, or real name and mailbox
     */
    public List<String> vrfy(
                              String address ) throws IOException {

        String command = VRFY + ' ' + address;
        send(command);
        List<String> list = new ArrayList<String>();
        do {
            switch (getResponse()) {
                case OK:
                case AMBIGUOUS:
                    response = response.trim();
                    if (response.indexOf('@') != -1) {
                        list.add(response);
                    } else if (response.indexOf('<') != -1) {
                        list.add(response);
                    } else if (response.indexOf(' ') == -1) {
                        list.add(response);
                    }
                    break;
                default:
                    return null;
            }
        } while (continuation);
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns a list of valid possibilities for the specified mailing list,
     * or null on failure.
     * @param address a mailing list name
     */
    public List<String> expn(
                              String address ) throws IOException {

        String command = EXPN + ' ' + address;
        send(command);
        List<String> list = new ArrayList<String>();
        do {
            switch (getResponse()) {
                case OK:
                    response = response.trim();
                    list.add(response);
                    break;
                default:
                    return null;
            }
        } while (continuation);
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns some useful information about the specified parameter.
     * Typically this is a command.
     * @param arg the context of the query, or null for general information
     * @return a list of possibly useful information, or null if the command
     * failed.
     */
    public List<String> help(
                              String arg ) throws IOException {

        String command = (arg == null)
                                       ? HELP
                                       : HELP + ' ' + arg;
        send(command);
        List<String> list = new ArrayList<String>();
        do {
            switch (getResponse()) {
                case INFO:
                    list.add(response);
                    break;
                default:
                    return null;
            }
        } while (continuation);
        return Collections.unmodifiableList(list);
    }

    /**
     * Issues a NOOP command.
     * This does nothing, but can be used to keep the connection alive.
     */
    public void noop() throws IOException {

        send(NOOP);
        getAllResponses();
    }

    /**
     * Close the connection to the server.
     */
    public void quit() throws IOException {

        try {
            send(QUIT);
            getAllResponses();
            /* RFC 2821 states that the server MUST send an OK reply here, but
             * many don't: postfix, for instance, sends 221.
             * In any case we have done our best. */
        } catch (IOException e) {} finally {
            // Close the socket anyway.
            socket.close();
        }
    }

    /**
     * Issues a HELO command.
     * @param hostname the local host name
     */
    public boolean helo(
                         String hostname ) throws IOException {

        String command = HELO + ' ' + hostname;
        send(command);
        return (getAllResponses() == OK);
    }

    /**
     * Issues an EHLO command.
     * If successful, returns a list of the SMTP extensions supported by the
     * server.
     * Otherwise returns null, and HELO should be called.
     * @param hostname the local host name
     */
    public List<String> ehlo(
                              String hostname ) throws IOException {

        String command = EHLO + ' ' + hostname;
        send(command);
        List<String> extensions = new ArrayList<String>();
        do {
            switch (getResponse()) {
                case OK:
                    extensions.add(response);
                    break;
                default:
                    return null;
            }
        } while (continuation);
        return Collections.unmodifiableList(extensions);
    }

    /**
     * Returns a configured SSLSocketFactory to use in creating new SSL
     * sockets.
     * @param tm an optional trust manager to use
     */
    protected SSLSocketFactory getSSLSocketFactory(
                                                    TrustManager tm ) throws GeneralSecurityException {

        if (tm == null) {
            tm = new EmptyX509TrustManager();
        }
        SSLContext context = SSLContext.getInstance("TLS");
        TrustManager[] trust = new TrustManager[]{ tm };
        context.init(null, trust, null);
        return context.getSocketFactory();
    }

    /**
     * Negotiate TLS over the current connection.
     * This depends on many features, such as the JSSE classes being in the
     * classpath. Returns true if successful, false otherwise.
     */
    public boolean starttls() throws IOException {

        return starttls(new EmptyX509TrustManager());
    }

    /**
     * Negotiate TLS over the current connection.
     * This depends on many features, such as the JSSE classes being in the
     * classpath. Returns true if successful, false otherwise.
     * @param tm the custom trust manager to use
     */
    public boolean starttls(
                             TrustManager tm ) throws IOException {

        try {
            SSLSocketFactory factory = getSSLSocketFactory(tm);

            send(STARTTLS);
            if (getAllResponses() != READY) {
                return false;
            }

            String hostname = socket.getInetAddress().getHostName();
            int port = socket.getPort();
            SSLSocket ss = (SSLSocket) factory.createSocket(socket, hostname, port, true);
            String[] protocols = { "TLSv1", "SSLv3" };
            ss.setEnabledProtocols(protocols);
            ss.setUseClientMode(true);
            ss.startHandshake();

            // Set up streams
            InputStream is = ss.getInputStream();
            is = new BufferedInputStream(is);
            is = new CRLFInputStream(is);
            in = new LineInputStream(is);
            OutputStream os = ss.getOutputStream();
            os = new BufferedOutputStream(os);
            out = new CRLFOutputStream(os);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    // -- Authentication --

    /**
     * Authenticates the connection using the specified SASL mechanism,
     * username, and password.
     * @param mechanism a SASL authentication mechanism, e.g. LOGIN, PLAIN,
     * CRAM-MD5, GSSAPI
     * @param username the authentication principal
     * @param password the authentication credentials
     * @return true if authentication was successful, false otherwise
     */
    public boolean authenticate(
                                 String mechanism,
                                 String username,
                                 String password ) throws IOException {

        try {
            String[] m = new String[]{ mechanism };
            CallbackHandler ch = new SaslCallbackHandler(username, password);
            // Avoid lengthy callback procedure for GNU Crypto
            HashMap<String, String> p = new HashMap<String, String>();
            p.put("gnu.crypto.sasl.username", username);
            p.put("gnu.crypto.sasl.password", password);
            SaslClient sasl = Sasl.createSaslClient(m,
                                                    null,
                                                    "smtp",
                                                    socket.getInetAddress().getHostName(),
                                                    p,
                                                    ch);
            if (sasl == null) {
                // Fall back to home-grown SASL clients
                if ("LOGIN".equalsIgnoreCase(mechanism)) {
                    sasl = new SaslLogin(username, password);
                } else if ("PLAIN".equalsIgnoreCase(mechanism)) {
                    sasl = new SaslPlain(username, password);
                } else if ("CRAM-MD5".equalsIgnoreCase(mechanism)) {
                    sasl = new SaslCramMD5(username, password);
                } else {
                    return false;
                }
            }

            StringBuffer cmd = new StringBuffer(AUTH);
            cmd.append(' ');
            cmd.append(mechanism);
            if (sasl.hasInitialResponse()) {
                cmd.append(' ');
                byte[] init = sasl.evaluateChallenge(new byte[0]);
                if (init.length == 0) {
                    cmd.append('=');
                } else {
                    cmd.append(new String(BASE64.encode(init), "US-ASCII"));
                }
            }
            send(cmd.toString());
            while (true) {
                switch (getAllResponses()) {
                    case 334:
                        try {
                            byte[] c0 = response.getBytes("US-ASCII");
                            byte[] c1 = BASE64.decode(c0); // challenge
                            byte[] r0 = sasl.evaluateChallenge(c1);
                            byte[] r1 = BASE64.encode(r0); // response
                            out.write(r1);
                            out.write(0x0d);
                            out.flush();
                            log.trace("> " + new String(r1, "US-ASCII"));
                        } catch (SaslException e) {
                            // Error in SASL challenge evaluation - cancel exchange
                            out.write(0x2a);
                            out.write(0x0d);
                            out.flush();
                            log.trace("> *");
                        }
                        break;
                    case 235:
                        String qop = (String) sasl.getNegotiatedProperty(Sasl.QOP);
                        if ("auth-int".equalsIgnoreCase(qop) || "auth-conf".equalsIgnoreCase(qop)) {
                            InputStream is = socket.getInputStream();
                            is = new BufferedInputStream(is);
                            is = new SaslInputStream(sasl, is);
                            is = new CRLFInputStream(is);
                            in = new LineInputStream(is);
                            OutputStream os = socket.getOutputStream();
                            os = new BufferedOutputStream(os);
                            os = new SaslOutputStream(sasl, os);
                            out = new CRLFOutputStream(os);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        } catch (SaslException e) {
            log.error(e.getMessage(), e);
            return false; // No provider for mechanism
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            return false; // No javax.security.sasl classes
        }
    }

    // -- Utility methods --

    /**
     * Send the specified command string to the server and return the responses.
     * If the response is a continuation, continues to read responses until continuation ceases.
     * If a different response code from the first is encountered, this causes a protocol exception.
     *
     * @param command the command to send
     * @return the response from the server
     */
    public int sendCommand(
                            String command ) throws IOException {

        send(command);
        return getAllResponses();
    }

    /**
     * Send the specified command string to the server.
     * @param command the command to send
     */
    protected void send(
                         String command ) throws IOException {

        log.trace("> " + command);
        out.write(command.getBytes("US-ASCII"));
        out.write(0x0d);
        out.flush();
    }

    /**
     * Returns the next response from the server.
     */
    protected int getResponse() throws IOException {

        String line = null;
        try {
            line = in.readLine();
            // Handle special case eg. 334 where CRLF occurs after code.
            if (line.length() < 4) {
                line = line + '\n' + in.readLine();
            }
            log.trace("< " + line);
            responseCode = Integer.parseInt(line.substring(0, 3));
            continuation = (line.charAt(3) == '-');
            response = line.substring(4);
            return responseCode;
        } catch (NumberFormatException e) {
            throw new SmtpException("Unexpected response: " + line);
        }
    }

    /**
     * Returns the next response from the server.
     * If the response is a continuation, continues to read responses until
     * continuation ceases. If a different response code from the first is
     * encountered, this causes a protocol exception.
     */
    protected int getAllResponses() throws IOException {

        int code1, code;
        boolean err = false;
        code1 = code = getResponse();
        while (continuation) {
            code = getResponse();
            if (code != code1) {
                err = true;
            }
        }
        if (err) {
            throw new SmtpException("Conflicting response codes");
        }
        return code;
    }
}
