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
package com.axway.ats.core.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;

/**
 * Utility class giving access to some host names and ports
 */
public class HostUtils {

    private static final Logger      log                 = LogManager.getLogger(HostUtils.class);

    public static final String       LOCAL_HOST_NAME     = "localhost";
    public static final String       LOCAL_HOST_IPv4     = "127.0.0.1";
    public static final String       LOCAL_HOST_IPv6     = "::1";
    public static final int          LOWEST_PORT_NUMBER  = 1;
    public static final int          HIGHEST_PORT_NUMBER = 64 * 1024;

    // List of hosts found to be local ones
    private static final Set<String> localHosts;

    static {
        localHosts = Collections.synchronizedSet(new HashSet<String>());

        localHosts.add(LOCAL_HOST_NAME);
        localHosts.add(LOCAL_HOST_IPv4);
        localHosts.add(LOCAL_HOST_IPv6);
    }

    // List of hosts found to be non local ones
    private static final Set<String>        nonlocalHosts            = Collections.synchronizedSet(new HashSet<String>());

    /**
     * List of public addresses of the local host. The remote Agent uses that address to connect to the local host.
     * In case of many local network adapters, it may turn that different Agents will use different addresses
     * to connect to the local host.
     *
     * < [remote ATS Agent] , [public address of the local host] >
     */
    private static Map<String, InetAddress> localHostPublicAddresses = new HashMap<String, InetAddress>();

    /**
     * <em>Internal method</em> and could be changed at any time. You may use CommonConfigurator.setHostLocality().
     * Allows specifying externally(from the test code) whether some host is a local or remote one
     *
     * @param host host name or IP address. Currently does not expect and does not parse agent address (IP:port or
     *             host:port)
     * @param isLocal whether this host should be treated as a local or remote one
     */
    public static void setHostLocality( String host, boolean isLocal ) {

        if (isLocal) {
            localHosts.add(host);
            nonlocalHosts.remove(host);
        } else {
            nonlocalHosts.add(host);
            localHosts.remove(host);
        }
    }

    /**
     * Get the local host name (e.g. someone.corp.axway.com)
     *
     * @return
     */
    public static String getLocalHostName() {

        String localHostName = "";
        InetAddress inetAddress = null;
        List<InetAddress> ipList = getIpAddressesList(true, false);
        if (ipList.size() > 0) {
            inetAddress = ipList.get(0);
        }
        if (inetAddress != null) {
            localHostName = inetAddress.getHostName();
        }
        return localHostName;
    }

    /**
     * Get the IP (not the loopback 127.0.0.1) of the local host. Note: it will
     * return the first valid one, so the result is not clear when have more
     * than one network card installed.
     *
     * @return first IP address found
     */
    public static String getLocalHostIP() {

        String localHostIP = "";
        InetAddress inetAddress = null;
        List<InetAddress> ipList = getIpAddressesList(true, false);
        if (ipList.size() > 0) {
            inetAddress = ipList.get(0);
        }
        if (inetAddress != null) {
            localHostIP = inetAddress.getHostAddress();
        }
        return localHostIP;
    }

    /**
     * Get the IP (not the loopback 127.0.0.1) of the local host. Note: it will
     * return the first valid one, so the result is not clear when have more
     * than one network card installed.
     * @param excludeDownOnes - whether to exclude interfaces that are down
     * @return
     */
    public static String getLocalHostIP( boolean excludeDownOnes ) {

        String localHostIP = "";
        InetAddress inetAddress = null;
        List<InetAddress> ipList = getIpAddressesList(true, excludeDownOnes);
        if (ipList.size() > 0) {
            inetAddress = ipList.get(0);
        }
        if (inetAddress != null) {
            localHostIP = inetAddress.getHostAddress();
        }
        return localHostIP;
    }

    /**
     * Tells if host is the local host. Note it will return true if:
     * <ul>
     * <li>host is null,</li>
     * <li>empty string,</li>
     * <li>IP of the local host including 127.0.0.1,</li>
     * <li>the name of the local host including &quot;localhost&quot;.</li>
     * </ul>
     * Otherwise it will return false.
     *
     * @param host
     *            the host to check
     * @return if host is the local host
     */
    public static boolean isLocalHost( String host ) {

        Set<InetAddress> foundAddresses = new HashSet<>();

        if (StringUtils.isNullOrEmpty(host)) {
            // we assume a local host
            return true;
        } else if (localHosts.contains(host)) {
            // we already know this is a local host
            return true;
        } else if (nonlocalHosts.contains(host)) {
            // we already know this is not a local host
            return false;
        } else {
            // unknown host, check if it is local or not
            Enumeration<NetworkInterface> netInterfaces = null;
            long startTimeMs = -1;
            if (log.isTraceEnabled()) {
                startTimeMs = System.currentTimeMillis();
            }
            try {
                try {
                    netInterfaces = NetworkInterface.getNetworkInterfaces();
                } catch (SocketException e) {
                    // we hope this will never happen
                    log.error("Error obtaining info about this system's network interfaces. We will assume '"
                              + host + "' is a local host", e);
                    return true;
                }

                while (netInterfaces.hasMoreElements()) {
                    NetworkInterface netInterface = netInterfaces.nextElement();

                    Enumeration<InetAddress> inetAddresses = netInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {

                        InetAddress inetAddress = inetAddresses.nextElement();
                        foundAddresses.add(inetAddress); // cache the found inetAddress
                        String hostAddress = inetAddress.getHostAddress();
                        if (inetAddress instanceof Inet6Address) {

                            if (hostAddress != null
                                && stripIPv6InterfaceId(compressIPv6Address(host)).equalsIgnoreCase(
                                                                                                    stripIPv6InterfaceId(compressIPv6Address(hostAddress)))) {
                                localHosts.add(host);
                                return true;
                            }
                        } else if (host.equalsIgnoreCase(hostAddress)) {
                            localHosts.add(host);
                            return true;
                        }

                        String hostName = inetAddress.getHostName();
                        localHosts.add(hostName); // local host name. Cache it regardless if this is currently checked
                        // "host" or not
                        if (host.equalsIgnoreCase(hostName)) {
                            return true;
                        }

                    }
                }

                try {
                    InetAddress address = InetAddress.getByName(host);
                    if (address != null && foundAddresses.contains(address)) {
                        localHosts.add(host);
                        return true;
                    }
                } catch (UnknownHostException e) {
                    log.warn("Host '" + host + "' could not be resolved! Check name and network configuration", e);
                    nonlocalHosts.add(host);
                    return false;
                }

                nonlocalHosts.add(host);
                return false;
            } finally {
                if (log.isTraceEnabled()) {
                    log.trace(
                              "Total duration to enumerate network interfaces and check locality for host: " + host
                              + ": "
                              + (System.currentTimeMillis() - startTimeMs) + " ms");
                }
            }
        }
    }

    /**
     * @return a list with all IP addresses
     */
    public static List<InetAddress> getAllIpAddresses() {

        return getIpAddressesList(false, false);
    }

    /**
     * Return a list with addresses.
     * if called with true, it will try to return the first IPv4 address or if there isn't such - the last IPv6 address
     * if called with false, it will return a list with all IP addresses
     *
     * @return
     */
    private static List<InetAddress> getIpAddressesList( boolean exitOnFirstIPv4, boolean excludeDownOnes ) {

        //list containing all ip addresses
        List<InetAddress> ipList = new ArrayList<InetAddress>();
        try {

            // cycle all net interfaces
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            if (log.isTraceEnabled()) {
                log.trace("Start iterating all network interfaces!");
            }
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = netInterfaces.nextElement();
                if (!netInterface.isUp()) {
                    log.trace("Network interface '" + netInterface.toString() + "' is down. Skipping it");
                    if (excludeDownOnes) {
                        continue;
                    }

                }
                if (!netInterface.isLoopback()) {
                    if (log.isTraceEnabled()) {
                        log.trace("    Start iterating interface '" + netInterface.getName() + "'");
                    }
                    // for each net interface cycle all IP addresses
                    Enumeration<InetAddress> ipAddresses = netInterface.getInetAddresses();
                    InetAddress ipAddress = null;
                    while (ipAddresses.hasMoreElements()) {
                        ipAddress = ipAddresses.nextElement();

                        if (ipAddress instanceof java.net.Inet4Address) {
                            Inet4Address ipv4 = (Inet4Address) ipAddress;
                            if (!ipv4.isLoopbackAddress()) {
                                // we found an appropriate IPv4 address
                                ipList.add(ipv4);
                                if (exitOnFirstIPv4) {
                                    /*
                                     * return list, containing the last added IP address,
                                     * which will be the first IPv4 address, that was found
                                     */
                                    int listSize = ipList.size();
                                    return ipList.subList(listSize - 1, listSize);
                                }
                            }
                        } else { // ( ip instanceof java.net.Inet6Address )
                            Inet6Address ipv6 = (Inet6Address) ipAddress;
                            // FIXME: currently we do not filter out the temporary IPv6 addresses
                            if (!ipv6.isLinkLocalAddress()) {
                                // We found an appropriate IPv6 address. Remember it, but keep searching for an appropriate IPv4 address.
                                ipList.add(ipv6);
                            }
                        }
                    }
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("Finish iterating all network interfaces!");
            }
        } catch (SocketException se) {
            log.error("Error obtaining the local host address", se);
        }

        /*
         * We will return either list with the IP addresses or empty list
         */
        if (exitOnFirstIPv4 && !ipList.isEmpty()) {
            int listSize = ipList.size();
            return ipList.subList(listSize - 1, listSize);
        }
        return ipList;
    }

    /**
     * Returns the public address of the local host.
     * It is used when a remote agent needs to initiates connection to the local host
     *
     * @param remoteAtsAgent the remote ATS agent
     * @return the public address of the local host
     */
    public static String getPublicLocalHostIp( String remoteAtsAgent ) {

        InetAddress localHostPublicAddress;

        // make sure the port is available. Adds default port no custom is specified
        remoteAtsAgent = getAtsAgentIpAndPort(remoteAtsAgent);

        // check if we already know the good public address
        synchronized (localHostPublicAddresses) {
            localHostPublicAddress = localHostPublicAddresses.get(remoteAtsAgent);

            // 1. try to make a WS connection with the remote ATS Agent,
            // if succeed - we will find a good public IP
            if (localHostPublicAddress == null) {
                String[] atsAgentTokens = splitAddressHostAndPort(remoteAtsAgent);
                Socket socket = null;
                try {
                    socket = new Socket();
                    try {
                        InetSocketAddress sa = new InetSocketAddress(atsAgentTokens[0],
                                                                     Integer.parseInt(atsAgentTokens[1]));
                        socket.connect(sa, 30000);
                        InetAddress tmpPublicAddress = socket.getLocalAddress();
                        if (tmpPublicAddress == null || tmpPublicAddress.isAnyLocalAddress()
                        /* like: 0.0.0.0 - there is Win & Java 6 issue for IPv6*/) {
                            localHostPublicAddress = null;
                            throw new IllegalStateException(
                                                            "Unable to retrieve public IP of the local host which is used to connect to agent at "
                                                            + remoteAtsAgent + ". Address returned was "
                                                            + tmpPublicAddress);
                        } else {
                            if (tmpPublicAddress.isLoopbackAddress()) {
                                List<InetAddress> ipList = getIpAddressesList(true, false);
                                if (ipList.size() > 0) {
                                    tmpPublicAddress = ipList.get(0);
                                    log.warn(
                                             "We are unable to reliably retrieve the public IP of the local host which is used to connect to agent at "
                                             + remoteAtsAgent + ". We will use the first retrieved IP: "
                                             + tmpPublicAddress);
                                }
                            }
                            localHostPublicAddress = tmpPublicAddress;
                        }
                    } finally {
                        if (socket != null) {
                            IoUtils.closeStream(socket);
                        }
                    }

                    // cache it
                    localHostPublicAddresses.put(remoteAtsAgent, localHostPublicAddress);
                } catch (Exception e) {
                    log.warn("Unable to determine the appropriate local address for communicating with ATS agent at "
                             + remoteAtsAgent
                             + ". Now we will ask the Java virtual machine to propose a local address"
                             + ", but this is not reliable when have more than one network adapters.", e);
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {}
                    }
                }
            }

            // 2. our last chance is to ask the JVM
            if (localHostPublicAddress == null) {
                List<InetAddress> ipList = getIpAddressesList(true, false);
                if (ipList.size() > 0) {
                    localHostPublicAddress = ipList.get(0);
                }
                if (localHostPublicAddress != null) {
                    log.warn("ATS will use '" + localHostPublicAddress.getHostAddress()
                             + "' as IP address of current host visible to '" + remoteAtsAgent + "'");
                    // cache it
                    localHostPublicAddresses.put(remoteAtsAgent, localHostPublicAddress);
                } else {
                    throw new IllegalStateException("The returned public address for local host is null");
                }
            }
        }

        return localHostPublicAddress.getHostAddress();
    }

    /**
     * Checks if agent is local, on same host as the caller/test executor.
     * @param atsAgent ATS agent address (hostname(IP) or hostname:port)
     * @return whether the agent is located on the same  host as the caller
     */
    public static boolean isLocalAtsAgent( String atsAgent ) {

        if (!StringUtils.isNullOrEmpty(atsAgent)) {

            String[] tokens = HostUtils.splitAddressHostAndPort(atsAgent);
            if (tokens.length > 0) {
                return HostUtils.isLocalHost(tokens[0]);
            }
        }

        return true;
    }

    /**
     * Returns the ATS agent address which contains its IP and port.
     * If a port is not passed, we will append the default one.
     *
     * @param atsAgent the agent
     * @return
     */
    public static String getAtsAgentIpAndPort( String atsAgent ) {

        if (atsAgent == null) {
            return null;
        }

        String[] tokens = splitAddressHostAndPort(atsAgent);

        if (tokens.length != 1 // port number detected
            || StringUtils.isNullOrEmpty(tokens[0])) { // null or empty IP, do not touch

            return atsAgent;
        } else {

            // IP/Hostname provided, but no PORT, so use the default one
            if (!atsAgent.startsWith("[") && tokens[0].split(":").length > 1) { // assume it is IPv6
                return "[" + atsAgent + "]:" + AtsSystemProperties.getAgentDefaultPort();
            }
            return atsAgent + ":" + AtsSystemProperties.getAgentDefaultPort();
        }
    }

    /**
     * Returns the ATS agent addresses which contains its IP and port.
     * If a port is not passed, we will append the default one.
     *
     * @param atsAgents a list of agents
     * @return
     */
    public static String[] getAtsAgentsIpAndPort( String[] atsAgents ) {

        if (atsAgents == null) {
            return null;
        }

        String[] fixedAtsAgents = new String[atsAgents.length];
        for (int i = 0; i < atsAgents.length; i++) {
            fixedAtsAgents[i] = getAtsAgentIpAndPort(atsAgents[i]);
        }

        return fixedAtsAgents;
    }

    /**
     * Splits an Address into Host and Port.
     *
     * @param address the address to parse
     * @return either { "host", "port" } or just { "host" }
     */
    public static String[] splitAddressHostAndPort( String address ) {

        String[] tokens = address.split(":");
        if (tokens.length == 2) {
            // assume it is IPv4 with port (IP:PORT) or HOSTNAME:PORT
            return new String[]{ tokens[0], tokens[1] };
        } else if (tokens.length > 2) {
            // assume it is IPv6 (IP or [IP]:PORT)
            if (address.startsWith("[") && address.contains("]:")) {

                return new String[]{ address.substring(0, address.indexOf(']') + 1),
                                     address.substring(address.lastIndexOf(':') + 1) };
            }
        }

        // return back the initial address
        return new String[]{ address };
    }

    /**
     * @param ipv6Address IPv6 address
     * @return the compressed variant of the specified address
     */
    public static String compressIPv6Address( String ipv6Address ) {

        return ipv6Address.replaceFirst("(^|:)(0+(:|$)){2,8}", "::") // remove empty octets (000:00:0:0000 -> ::)
                          .replaceAll("(:|^)0+([0-9a-fA-F]+)", "$1$2"); // remove leading zeroes (00ef:000f -> ef:f)
    }

    /**
     *
     * @param ipv6Address IPv6 address
     * @return the specified address with the leading interface id (the zone
     *         index: interface index or interface name, depending on the OS)
     */
    public static String stripIPv6InterfaceId( String ipv6Address ) {

        int interfaceIdIndex = ipv6Address.indexOf(
                                                   '%'); // this is the zone index: interface index or interface name, depending on the OS
        if (interfaceIdIndex > 0) {
            return ipv6Address.substring(0, interfaceIdIndex);
        }
        return ipv6Address;
    }
}
