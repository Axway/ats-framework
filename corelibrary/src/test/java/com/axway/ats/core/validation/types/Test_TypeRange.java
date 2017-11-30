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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.core.BaseTest;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

/**
 * This Unit test is used to verify the proper work of the
 * {@link Test_TypeRange} class
 */
public class Test_TypeRange extends BaseTest {

    private final Validator             validator               = new Validator();

    /** Valid test data */
    private static final Integer        INTEGER_VALUE           = new Integer(1);
    private static final Long           LONG_VALUE              = new Long(1);
    private static final Float          FLOAT_VALUE             = new Float(1.0);
    private static final Double         DOUBLE_VALUE            = new Double(1.0);
    private static final Double         LOWER_VALUE             = new Double(0.4);
    private static final Integer        VALID_RANGE_MAX         = new Integer(10);
    private static final Object[]       VALID_LOWER_VALUE       = new Object[]{ LOWER_VALUE };
    private static final Object[]       VALID_RANGE             = new Object[]{ LOWER_VALUE, VALID_RANGE_MAX };

    /** Invalid test data */
    private static final Object         NON_NUMBER              = "One";
    private static final Integer        NEGATIVE_INTEGER_VALUE  = new Integer(-1);

    private static final Integer        INVALID_RANGE_MIN       = new Integer(5);
    private static final Integer        INVALID_PORT_NUMBER     = new Integer(65555);

    private static final Object[]       INVALID_LOWER_VALUE     = new Object[]{ INVALID_RANGE_MIN };
    private static final Object[]       RANGE_OVER_VALID_NUMBER = new Object[]{ INVALID_LOWER_VALUE,
                                                                                VALID_RANGE_MAX };
    private static final Object[]       INVALID_RANGE           = new Object[]{ VALID_RANGE_MAX,
                                                                                INVALID_RANGE_MIN };

    private static final ValidationType TYPE_POSITIVE           = ValidationType.NUMBER_POSITIVE;
    private static final ValidationType TYPE_GREATER_THAN_ZERO  = ValidationType.NUMBER_GREATER_THAN_ZERO;
    private static final ValidationType TYPE_GREATER_THAN       = ValidationType.NUMBER_GREATER_THAN;
    private static final ValidationType TYPE_RANGE              = ValidationType.NUMBER_WITHIN_RANGE;
    private static final ValidationType TYPE_PORT               = ValidationType.NUMBER_PORT_NUMBER;

    /**
     * Set up the test cases
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        // empty
    }

    /**
     * Test this certain type's validation
     * 
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testValidationPositive() throws Exception {

        assertTrue(this.validator.validate(TYPE_POSITIVE, INTEGER_VALUE));
        assertTrue(this.validator.validate(TYPE_POSITIVE, LONG_VALUE));
        assertTrue(this.validator.validate(TYPE_POSITIVE, FLOAT_VALUE));
        assertTrue(this.validator.validate(TYPE_POSITIVE, DOUBLE_VALUE));

        assertFalse(this.validator.validate(TYPE_POSITIVE, NON_NUMBER));
        assertFalse(this.validator.validate(TYPE_POSITIVE, null));
        assertFalse(this.validator.validate(TYPE_POSITIVE, NEGATIVE_INTEGER_VALUE));
    }

    /**
     * Test this certain type's validation
     * 
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testValidationGreaterThanZero() throws Exception {

        assertTrue(this.validator.validate(TYPE_GREATER_THAN_ZERO, INTEGER_VALUE));
        assertTrue(this.validator.validate(TYPE_GREATER_THAN_ZERO, LONG_VALUE));
        assertTrue(this.validator.validate(TYPE_GREATER_THAN_ZERO, FLOAT_VALUE));
        assertTrue(this.validator.validate(TYPE_GREATER_THAN_ZERO, DOUBLE_VALUE));

        assertFalse(this.validator.validate(TYPE_GREATER_THAN_ZERO, LOWER_VALUE));
        assertFalse(this.validator.validate(TYPE_GREATER_THAN_ZERO, NON_NUMBER));
        assertFalse(this.validator.validate(TYPE_GREATER_THAN_ZERO, null));
        assertFalse(this.validator.validate(TYPE_GREATER_THAN_ZERO, NEGATIVE_INTEGER_VALUE));
    }

    /**
     * Test this certain type's validation
     * 
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testValidationGreaterThen() throws Exception {

        assertTrue(this.validator.validate(TYPE_GREATER_THAN, INTEGER_VALUE, VALID_LOWER_VALUE));

        assertFalse(this.validator.validate(TYPE_GREATER_THAN, NON_NUMBER));
        assertFalse(this.validator.validate(TYPE_GREATER_THAN, NON_NUMBER, VALID_LOWER_VALUE));
        assertFalse(this.validator.validate(TYPE_GREATER_THAN, null, VALID_LOWER_VALUE));
        assertFalse(this.validator.validate(TYPE_GREATER_THAN, INTEGER_VALUE, INVALID_LOWER_VALUE));
    }

    /**
     * Test this certain type's validation
     * 
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testValidationRange() throws Exception {

        assertTrue(this.validator.validate(TYPE_RANGE, INTEGER_VALUE, VALID_RANGE));

        assertFalse(this.validator.validate(TYPE_RANGE, NON_NUMBER));
        assertFalse(this.validator.validate(TYPE_RANGE, null));
        assertFalse(this.validator.validate(TYPE_RANGE, INTEGER_VALUE, RANGE_OVER_VALID_NUMBER));
        assertFalse(this.validator.validate(TYPE_RANGE,
                                            INTEGER_VALUE,
                                            new Object[]{ NON_NUMBER, NON_NUMBER }));
        assertFalse(this.validator.validate(TYPE_RANGE, INTEGER_VALUE, INVALID_RANGE));
    }

    /**
     * Test this certain type's validation
     * 
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testValidationPort() throws Exception {

        assertTrue(this.validator.validate(TYPE_PORT, INTEGER_VALUE));

        assertFalse(this.validator.validate(TYPE_PORT, INVALID_PORT_NUMBER));
        assertFalse(this.validator.validate(TYPE_PORT, null));
        assertFalse(this.validator.validate(TYPE_PORT, NEGATIVE_INTEGER_VALUE));
    }
}
