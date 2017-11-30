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

import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.exceptions.TypeException;

/**
 * The {@link TypeStringCIDR} class introduces the CIDR (Classless
 * Inter-Domain Routing) address checking.
 *
 * Created on : Oct 11, 2007
 */
public class TypeStringCIDR extends TypeString {

    private static final String ERROR_MESSAGE_INVALID       = "Argiment is not a valid CIDR address. ";
    private static final String ERROR_MESSAGE_NON_INTEGER   = "Argiment has a part that is a non-integer CIDR range. ";
    private static final String ERROR_MESSAGE_INVALID_RANGE = "Argiment has a CIDR range that is not in the correct range (1 - 32). ";
    private static final String ERROR_MESSAGE_INVALID_PART  = "Argiment is not a valid CIDR address - first part should be first IP address in the range. ";
    private static final String ERROR_VALIDATING_CIDR       = "Unable to verify CIDR address. ";

    /** Constructor */
    protected TypeStringCIDR( String paramName,
                              Object val,
                              Object[] args ) {

        super(paramName, val, args);
    }

    /**
     * Performs a type-specific validation for the given
     * {@link ValidationType}
     */
    @Override
    public void validate() throws TypeException {

        try {
            super.validate();

        } catch (TypeException e) {
            throw new TypeException(ERROR_VALIDATING_CIDR + e.getMessage(), e.getParameterName(), e);
        }

        //get the individual parts
        String[] parts = this.validatedValue.split("/");
        if (parts.length != 2) {
            throw new TypeException(ERROR_MESSAGE_INVALID, this.parameterName);
        }

        TypeRegex typeRegex = new TypeRegex(null, parts[0], null, TypeRegex.RegexType.IPv4_ADDRESS);

        try {
            typeRegex.validate();
        } catch (TypeException e) {
            throw new TypeException(ERROR_VALIDATING_CIDR + e.getMessage(), e.getParameterName());
        }

        int cidrRange;
        try {
            cidrRange = Integer.parseInt(parts[1]);
        } catch (NumberFormatException nfe) {
            throw new TypeException(ERROR_MESSAGE_NON_INTEGER, this.parameterName, nfe);
        }

        if (cidrRange < 1 || cidrRange > 32) {
            throw new TypeException(ERROR_MESSAGE_INVALID_RANGE, this.parameterName);
        }

        String[] ipParts = parts[0].split("\\.");
        int currentIpPart;

        for (String ipPart : ipParts) {

            //we should be safe, as this has been confirmed as valid IP address
            currentIpPart = Integer.parseInt(ipPart);

            if (cidrRange >= 8) {
                cidrRange -= 8;
            } else if ( (cidrRange > 0) && (cidrRange < 8)) {
                if ( ( ( (1 << (8 - cidrRange)) - 1) & currentIpPart) != 0) {
                    throw new TypeException(ERROR_MESSAGE_INVALID_PART, this.parameterName);
                }
                cidrRange = 0;
            } else {
                if (currentIpPart != 0) {
                    throw new TypeException(ERROR_MESSAGE_INVALID_PART, this.parameterName);
                }
            }
        }
    }
}
