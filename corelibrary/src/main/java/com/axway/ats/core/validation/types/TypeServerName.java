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
package com.axway.ats.core.validation.types;

import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.exceptions.TypeException;

/**
 * The {@link TypeServerName} class introduces the verification of
 * server addresses. It combines the verification for IP address,
 * port number and domain name in one place.
 *
 * Created on : Oct 11, 2007
 */
public class TypeServerName extends TypeString {

    private static final String ERROR_MESSAGE_MALFORMED         = "Argument is not a valid server address, because it contains more than one \":\" character sign. ";
    private static final String ERROR_VALIDATING_SERVER         = "Argument is not a valid server address. ";
    private static final String ERROR_VALIDATING_SERVER_NEITHER = "Argument is not a valid server address. It is neither a valid hostname, nor a valid IP address. ";
    private static final String ERROR_VALIDATING_SERVER_PORT    = "Argument is not a valid server address/hostname with port number.";

    private boolean             requirePort                     = false;
    private boolean             extended                        = false;

    /** Constructor */
    protected TypeServerName( String paramName, Object val, Object[] args, boolean requirePort,
                              boolean ext ) {

        super(paramName, val, args);
        this.requirePort = requirePort;
        this.extended = ext;
    }

    /**
     * Performs a type-specific validation for the given
     * {@link ValidationType}
     */
    @Override
    public void validate() throws TypeException {

        // --- proper string validation ---
        try {
            super.validate();

        } catch (TypeException e) {
            throw new TypeException(ERROR_VALIDATING_SERVER + e.getMessage(), e.getParameterName(), e);
        }

        BaseType typeIP = null;
        BaseType typeHostname = null;
        BaseType typeCIDR = null;
        BaseType typePort = null;

        String[] tokens = HostUtils.splitAddressHostAndPort(this.validatedValue);
        if (tokens.length == 1 && requirePort) {

            throw new TypeException(ERROR_VALIDATING_SERVER_PORT, this.parameterName);
        }

        if (tokens.length == 2) { // port number detected

            typePort = TypeFactory.getInstance().createValidationType(ValidationType.NUMBER_PORT_NUMBER,
                                                                      tokens[1]);
            try {
                typePort.validate();
            } catch (TypeException e) {
                throw new TypeException(ERROR_VALIDATING_SERVER + e.getMessage(), this.parameterName);
            }
        }

        // validate hostname or IP address
        if (tokens[0].split(":").length > 1) { //assume it is IPv6

            if (isValidIPv6Address(tokens[0])) {
                return;
            }
            throw new TypeException(ERROR_MESSAGE_MALFORMED, this.parameterName);
        }

        typeIP = TypeFactory.getInstance().createValidationType(ValidationType.STRING_IP, tokens[0]);
        typeHostname = TypeFactory.getInstance().createValidationType(ValidationType.STRING_HOST_NAME,
                                                                      tokens[0]);
        typeCIDR = TypeFactory.getInstance().createValidationType(ValidationType.STRING_CIDR, tokens[0]);

        // every validation is checked and if it fails it's error message is inserted
        // into the next ones so that in the end if all three of them were to fail
        // the error message will contain all of them

        // check if this is a valid hostname or IP address
        try {
            typeIP.validate();
            return;
        } catch (TypeException outerException) {
            // if not check if this is a valid hostname
            try {
                typeHostname.validate();
                return;
            } catch (TypeException innerException) {
                // check if this is a valid CIDR IPv4 address   //TODO: add support for IPv6
                if (this.extended) {
                    try {
                        typeCIDR.validate();
                        return;
                    } catch (TypeException e) {
                        throw new TypeException(ERROR_VALIDATING_SERVER + e.getMessage(),
                                                this.parameterName);
                    }
                }
                throw new TypeException(ERROR_VALIDATING_SERVER_NEITHER, this.parameterName);
            }
        }
    }

    /**
     *
     * @param address IPv6 address
     * @return <code>true</code> if the specified address is valid
     */
    private boolean isValidIPv6Address( String address ) {

        try {
            java.net.Inet6Address.getByName(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
