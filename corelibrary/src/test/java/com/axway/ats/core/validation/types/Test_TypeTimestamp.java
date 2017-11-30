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
 * {@link Test_TypeTimestamp} class
 */
public class Test_TypeTimestamp extends BaseTest {

    private final Validator             validator                      = new Validator();

    /** Valid test data */
    private static final ValidationType VALIDATION_TYPE_TIMESTAMP_DAY  = ValidationType.NUMBER_TIMESTAMP_FULL_DAY;
    private static final ValidationType VALIDATION_TYPE_TIMESTAMP_HOUR = ValidationType.NUMBER_TIMESTAMP_FULL_HOUR;

    private static final String         VALID_DAY_TIMESTAMP            = "22118400";
    private static final String         VALID_HOUR_TIMESTAMP           = "1281600";

    /** Invalid test data */
    private static final String         INVALID_DAY_TIMESTAMP          = "30749856";
    private static final String         INVALID_HOUR_TIMESTAMP         = "1281244";
    private static final String         NOT_A_NUMBER                   = "not-a-number";

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
    public void testTimestampFullDayValidation() throws Exception {

        assertTrue(this.validator.validate(VALIDATION_TYPE_TIMESTAMP_DAY, VALID_DAY_TIMESTAMP));
        assertFalse(this.validator.validate(VALIDATION_TYPE_TIMESTAMP_DAY, INVALID_DAY_TIMESTAMP));
        assertFalse(this.validator.validate(VALIDATION_TYPE_TIMESTAMP_DAY, VALID_HOUR_TIMESTAMP));
        assertFalse(this.validator.validate(VALIDATION_TYPE_TIMESTAMP_DAY, NOT_A_NUMBER));
    }

    /**
     * Test this certain type's validation
     * 
     * @throws Exception if any unexpected error occurs
     */
    @Test
    public void testTimestampFullHourValidation() throws Exception {

        assertTrue(this.validator.validate(VALIDATION_TYPE_TIMESTAMP_HOUR, VALID_HOUR_TIMESTAMP));
        assertFalse(this.validator.validate(VALIDATION_TYPE_TIMESTAMP_HOUR, INVALID_HOUR_TIMESTAMP));
        assertFalse(this.validator.validate(VALIDATION_TYPE_TIMESTAMP_HOUR, NOT_A_NUMBER));
    }
}
