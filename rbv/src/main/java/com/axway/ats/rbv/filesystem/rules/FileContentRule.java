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
package com.axway.ats.rbv.filesystem.rules;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.rules.AbstractRule;
import com.axway.ats.rbv.rules.Rule;

/**
 *  This {@link Rule} is used for searching for a given string expression inside the
 *  file itself. The expression could be either a regular expression or a simple line
 *  of text. 
 */
public class FileContentRule extends AbstractRule {

    private String  expression;
    private boolean isRegularExpression;

    /**
     * Constructor
     * 
     * @param expression the expression to search for
     * @param ruleName the name of the {@link Rule}
     * @param isRegularExpression whether or not the expression should be treated as a regular expression 
     * @param expectedResult the expected result
     */
    public FileContentRule( String expression,
                            String ruleName,
                            boolean isRegularExpression,
                            boolean expectedResult ) {

        super(ruleName, expectedResult, FileSystemMetaData.class);

        //initialize members
        this.expression = expression;
        this.isRegularExpression = isRegularExpression;
    }

    @Override
    public boolean performMatch(
                                 MetaData metaData ) throws RbvException {

        // get the file from the meta data
        if (metaData instanceof FileSystemMetaData) {
            FilePackage file = ((FileSystemMetaData) metaData).getFilePackage();

            try {
                // grep the contents of the file for the expression           
                String[] actualContent = file.grep(expression, isRegularExpression);

                if (actualContent != null && actualContent.length > 0) {
                    return true;
                }
            } catch (PackageException pe) {
                throw new RbvStorageException(pe);
            }
        }

        return false;
    }

    @Override
    protected String getRuleDescription() {

        StringBuilder description = new StringBuilder("which expects the file to ");
        if (!getExpectedResult()) {
            description.append("not ");
        }
        description.append("contain the following ");

        if (isRegularExpression) {
            description.append("regular expression '");
        } else {
            description.append("line of text '");
        }

        return description.append(this.expression).append("'").toString();
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(FileSystemMetaData.FILE_PACKAGE);
        return metaKeys;
    }
}
