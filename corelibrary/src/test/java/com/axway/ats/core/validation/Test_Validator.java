/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.core.validation;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.core.BaseTest;
import com.axway.ats.core.validation.exceptions.InvalidInputArgumentsException;

/**
 * This Unit test is used to verify the proper work of the
 * {@link Validator} class
 */
@SuppressWarnings( "boxing")
public class Test_Validator extends BaseTest {

    private static final String[]  SPAMISH_TYPES          = { "uno", "dos", "tres" };

    /** Valid test data */
    private static final String    EXISTING_FILE_NAME     = "src/test/resources/validation/file.txt";         // TODO change as for ANT the CWD is project root
    private static final Object    NUMBER_IN_RANGE        = new Double(1);
    private static final String    VALID_SPAMISH_TYPE     = SPAMISH_TYPES[1];
    private static final Integer[] VALID_POSITIVE_ARRAY   = { 1, 2, 3, 4, 5 };

    /** Invalid test data */
    private static final String    NONEXISTING_FILE_NAME  = "src/test/resources/validation/file.tx_NOT_FOUND";
    private static final Object    NUMBER_OUT_OF_RANGE    = new Double(100);
    private static final String    INVALID_SPAMISH_TYPE   = "quiattro";

    private static final Integer[] INVALID_POSITIVE_ARRAY = { 1, -2, 3, 4, 5 };

    private final Validator        validator              = new Validator();

    /**
     * Set up the test cases
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        // empty
    }

    /**
     * Checks method arguments validation
     * @throws Exception
     */
    @Test
    public void testValidatorPositive() throws Exception {

        testMethod(EXISTING_FILE_NAME, NUMBER_IN_RANGE, VALID_SPAMISH_TYPE, VALID_POSITIVE_ARRAY);
    }

    /**
     * Checks method arguments validation
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testValidatorNegativeNonExisting() throws Exception {

        testMethod(NONEXISTING_FILE_NAME, NUMBER_IN_RANGE, VALID_SPAMISH_TYPE, VALID_POSITIVE_ARRAY);
    }

    /**
     * Checks method arguments validation
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testValidatorNegativeOutOfRange() throws Exception {

        testMethod(EXISTING_FILE_NAME, NUMBER_OUT_OF_RANGE, VALID_SPAMISH_TYPE, VALID_POSITIVE_ARRAY);
    }

    /**
     * Checks method arguments validation
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testValidatorNegativeInvalidType() throws Exception {

        testMethod(EXISTING_FILE_NAME, NUMBER_IN_RANGE, INVALID_SPAMISH_TYPE, VALID_POSITIVE_ARRAY);
    }

    /**
     * Checks method arguments validation
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testValidatorNegativeInvalidArray() throws Exception {

        testMethod(EXISTING_FILE_NAME, NUMBER_IN_RANGE, VALID_SPAMISH_TYPE, INVALID_POSITIVE_ARRAY);
    }

    /**
     * Checks method arguments validation
     * @throws Exception
     */
    @Test( expected = InvalidInputArgumentsException.class)
    public void testValidatorNegativeNullValue() throws Exception {

        testMethod(null, NUMBER_IN_RANGE, VALID_SPAMISH_TYPE, INVALID_POSITIVE_ARRAY);
    }

    /**
     * Negative check - wrong array name specified in the {@link Validate} annotation
     * @throws Exception
     */
    @Test( expected = RuntimeException.class)
    public void testValidatorWrongArrayName() throws Exception {

        invalidTestMethodWrongArrayName(VALID_SPAMISH_TYPE);
    }

    /**
     * Negative check - no array name specified in the {@link Validate} annotation
     * @throws Exception
     */
    @Test( expected = RuntimeException.class)
    public void testValidatorNoArrayName() throws Exception {

        invalidTestMethodNoArrayName(VALID_SPAMISH_TYPE);
    }

    private void testMethod(
                             @Validate( name = "fileName", type = ValidationType.STRING_EXISTING_FILE) String fileName,
                             @Validate( name = "number", type = ValidationType.NUMBER_WITHIN_RANGE, args = { "0",
                                                                                                             "10" }) Object number,
                             @Validate( name = "spamish", type = ValidationType.STRING_CONSTANT, args = { "SPAMISH_TYPES" }) String spamish,
                             @Validate( name = "spamish", type = ValidationType.NUMBER_POSITIVE) Integer[] array )
                                                                                                                   throws Exception {

        this.validator.validateMethodParameters(new Object[]{ fileName, number, spamish, array });
    }

    private void invalidTestMethodWrongArrayName(
                                                  @Validate( name = "spamish", type = ValidationType.STRING_CONSTANT, args = { "SPANISH_TYPES" }) String spamish )
                                                                                                                                                                   throws Exception {

        this.validator.validateMethodParameters(new Object[]{ spamish });
    }

    private void invalidTestMethodNoArrayName(
                                               @Validate( name = "spamish", type = ValidationType.STRING_CONSTANT) String spamish )
                                                                                                                                    throws Exception {

        this.validator.validateMethodParameters(new Object[]{ spamish });
    }
}
