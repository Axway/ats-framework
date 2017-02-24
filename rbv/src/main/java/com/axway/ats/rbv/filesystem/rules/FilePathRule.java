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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.rules.AbstractRule;
import com.axway.ats.rbv.rules.Rule;

public class FilePathRule extends AbstractRule {

    private String path;
    private String filenameRegex = null;

    /**
     * Constructor
     * 
     * @param path the path with which to compare 
     * @param ruleName the name of the {@link Rule}
     * @param expectedResult matcher's expected result
     */
    public FilePathRule( String path, String ruleName, boolean expectedResult ) {

        super( ruleName, expectedResult, FileSystemMetaData.class );
        this.path = path;
    }

    /**
     * Constructor, allowing a priority to be set for this {@link Rule}
     * 
     * @param path the path with which to compare 
     * @param ruleName the name of the {@link Rule}
     * @param expectedResult matcher's expected result
     * @param priority the {@link Rule}s priority
     */
    public FilePathRule( String path, String ruleName, boolean expectedResult, int priority ) {

        super( ruleName, expectedResult, FileSystemMetaData.class, priority );
        this.path = path;
    }

    /**
     * Constructor, allowing a priority to be set for this {@link Rule}
     * 
     * @param directory the path to the file to compare
     * @param filenameRegex the pattern for matching the file name 
     * @param ruleName the name of the {@link Rule}
     * @param expectedResult matcher's expected result
     * @param priority the {@link Rule}s priority
     */
    public FilePathRule( String directory, String filenameRegex, String ruleName, boolean expectedResult,
                         int priority ) {

        super( ruleName, expectedResult, FileSystemMetaData.class, priority );
        this.path = directory;
        this.filenameRegex = filenameRegex;
    }

    @Override
    public boolean performMatch(
                                 MetaData metaData ) throws RbvException {

        boolean actualResult = false;

        if( metaData instanceof FileSystemMetaData ) {
            //get the file from the meta data
            FilePackage file = ( ( FileSystemMetaData ) metaData ).getFilePackage();

            String destAbsolutePath = file.getAbsolutePath();
            log.info( "Actual value is '" + destAbsolutePath + "'" );

            if( filenameRegex == null ) {
                actualResult = !StringUtils.isNullOrEmpty( destAbsolutePath )
                               && destAbsolutePath.equals( this.path );
            } else {
                String fileName = file.getName();
                String filePath = IoUtils.getFilePath( destAbsolutePath );

                Pattern pattern = Pattern.compile( filenameRegex );
                Matcher matcher = pattern.matcher( fileName );

                actualResult = !StringUtils.isNullOrEmpty( filePath ) && filePath.equals( this.path )
                               && matcher.matches();
            }
        }

        return actualResult;
    }

    @Override
    protected String getRuleDescription() {

        StringBuilder description = new StringBuilder( "which expects file with name " );
        if( !getExpectedResult() ) {
            description.append( "different than " );
        }
        description.append( "'" );
        description.append( this.path );
        if( filenameRegex != null ) {
            description.append( filenameRegex );
        }
        description.append( "'" );

        return description.toString();
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add( FileSystemMetaData.FILE_PACKAGE );
        return metaKeys;
    }
}
