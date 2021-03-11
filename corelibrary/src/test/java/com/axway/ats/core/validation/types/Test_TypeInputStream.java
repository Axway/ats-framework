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
package com.axway.ats.core.validation.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.core.BaseTest;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

/**
 * This Unit test is used to verify the proper work of the
 * {@link TypeInputStream} class
 */
public class Test_TypeInputStream extends BaseTest {

    private final Validator             validator          = new Validator();

    private static final String         EXISTING_FILE_NAME = "src/test/resources/validation/file.txt";   // TODO use classpath
    private static final File           EXISTING_FILE      = new File(EXISTING_FILE_NAME);

    /** Invalid test data */
    private static final String         EMPTY_FILE_NAME    = "src/test/resources/validation/empty.file"; // TODO use classpath
    private static final File           EMPTY_FILE         = new File(EMPTY_FILE_NAME);

    private static final ValidationType VALIDATION_TYPE    = ValidationType.INPUT_STREAM_NOT_EMPTY;

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
    public void testValidation() throws Exception {

        FileInputStream stream = new FileInputStream(EXISTING_FILE);
        assertTrue(this.validator.validate(VALIDATION_TYPE, stream));
        stream.close();

        stream = new FileInputStream(EMPTY_FILE);
        assertFalse(this.validator.validate(VALIDATION_TYPE, stream));
        stream.close();

        assertFalse(this.validator.validate(VALIDATION_TYPE, new Object()));
        assertFalse(this.validator.validate(VALIDATION_TYPE, null));
    }
}
