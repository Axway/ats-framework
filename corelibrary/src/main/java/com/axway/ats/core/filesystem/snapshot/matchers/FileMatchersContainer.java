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
package com.axway.ats.core.filesystem.snapshot.matchers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axway.ats.core.filesystem.snapshot.matchers.SkipContentMatcher.MATCH_TYPE;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipIniMatcher.MATCH_ENTITY;

/**
 * Contains all the matchers for all files in some directory
 */
public class FileMatchersContainer implements Serializable {

    private static final long                     serialVersionUID    = 1L;

    public Map<String, FindRules>                 fileAttributesMap;
    public Map<String, List<SkipPropertyMatcher>> propertyMatchersMap;
    public Map<String, List<SkipXmlNodeMatcher>>  xmlMatchersMap;
    public Map<String, List<SkipIniMatcher>>      iniMatchersMap;
    public Map<String, List<SkipTextLineMatcher>> textMatchersMap;
    
    public FileMatchersContainer() {

        this.fileAttributesMap = new HashMap<>();
        this.propertyMatchersMap = new HashMap<>();
        this.xmlMatchersMap = new HashMap<>();
        this.iniMatchersMap = new HashMap<>();
        this.textMatchersMap = new HashMap<>();
    }

    /**
     * Extract the matchers for some sub-directory(if any)
     * @param subdir the sub directory
     * @return
     */
    public FileMatchersContainer getMatchersContainerForSubdir( String subdir ) {

        FileMatchersContainer newFileMatchersContainer = new FileMatchersContainer();

        // add file attributes
        for( String path : fileAttributesMap.keySet() ) {
            if( path.contains( subdir ) ) {
                String newPath = path.substring( subdir.length() );
                newFileMatchersContainer.fileAttributesMap.put( newPath, fileAttributesMap.get( path ) );
            }
        }

        // add property file matchers
        for( String path : propertyMatchersMap.keySet() ) {
            if( path.contains( subdir ) ) {
                String newPath = path.substring( subdir.length() );
                newFileMatchersContainer.propertyMatchersMap.put( newPath, propertyMatchersMap.get( path ) );
            }
        }

        // add XML file matchers
        for( String path : xmlMatchersMap.keySet() ) {
            if( path.contains( subdir ) ) {
                String newPath = path.substring( subdir.length() );
                newFileMatchersContainer.xmlMatchersMap.put( newPath, xmlMatchersMap.get( path ) );
            }
        }
        
        // add INI file matchers
        for( String path : iniMatchersMap.keySet() ) {
            if( path.contains( subdir ) ) {
                String newPath = path.substring( subdir.length() );
                newFileMatchersContainer.iniMatchersMap.put( newPath, iniMatchersMap.get( path ) );
            }
        }
        
        // add TEXT file matchers
        for( String path : textMatchersMap.keySet() ) {
            if( path.contains( subdir ) ) {
                String newPath = path.substring( subdir.length() );
                newFileMatchersContainer.textMatchersMap.put( newPath, textMatchersMap.get( path ) );
            }
        }

        return newFileMatchersContainer;
    }

    public FindRules getFileAtrtibutes( String filePath ) {

        return fileAttributesMap.get( filePath );
    }

    public void addFileAttributesMap( Map<String, FindRules> fileRules ) {

        if( fileRules != null && !fileRules.isEmpty() ) {
            fileAttributesMap.putAll( fileRules );
        }
    }

    public void addFileAttributes( String filePath, FindRules fileAttributes ) {

        fileAttributesMap.put( filePath, fileAttributes );
    }

    public Map<String, List<SkipPropertyMatcher>> getPropertyMatchersMap() {

        return propertyMatchersMap;
    }

    public List<SkipPropertyMatcher> getPropertyMatchers( String filePath ) {

        return propertyMatchersMap.get( filePath );
    }

    public void addSkipPropertyMatcher( String directoryAlias, String filePathInThisDirectory, String token,
                                        boolean isMatchingKey, SkipContentMatcher.MATCH_TYPE matchType ) {

        List<SkipPropertyMatcher> propertyMatchersForThisFile = this.propertyMatchersMap.get( filePathInThisDirectory );

        // cycle all matchers about this file
        if( this.propertyMatchersMap.containsKey( filePathInThisDirectory ) ) {
            for( SkipPropertyMatcher matcher : this.propertyMatchersMap.get( filePathInThisDirectory ) ) {
                // there is already a matcher for this file
                // add the new condition
                if( isMatchingKey ) {
                    matcher.addKeyCondition( token, matchType );
                } else {
                    matcher.addValueCondition( token, matchType );
                }
                return;
            }
        }

        // no appropriate matcher is present for this file, add one
        if( propertyMatchersForThisFile == null ) {
            propertyMatchersForThisFile = new ArrayList<>();
            this.propertyMatchersMap.put( filePathInThisDirectory, propertyMatchersForThisFile );
        }

        SkipPropertyMatcher newMatcher = new SkipPropertyMatcher( directoryAlias, filePathInThisDirectory );
        if( isMatchingKey ) {
            newMatcher.addKeyCondition( token, matchType );
        } else {
            newMatcher.addValueCondition( token, matchType );
        }
        propertyMatchersForThisFile.add( newMatcher );
    }

    public List<SkipXmlNodeMatcher> getXmlNodeMatchers( String filePath ) {

        return xmlMatchersMap.get( filePath );
    }

    public Map<String, List<SkipXmlNodeMatcher>> getXmlNodeMatchersMap() {

        return xmlMatchersMap;
    }

    public void addSkipXmlNodeAttributeMatcher( String directoryAlias, String filePathInThisDirectory,
                                                String nodeXpath, String attributeKey, String attributeValue,
                                                SkipContentMatcher.MATCH_TYPE matchType ) {

        List<SkipXmlNodeMatcher> xmlMatchersForThisFile = getSkipXmlNodeAttributeMatcher( filePathInThisDirectory );

        SkipXmlNodeMatcher newMatcher = new SkipXmlNodeMatcher( directoryAlias, filePathInThisDirectory );
        newMatcher.addNodeAttributeMatcher( nodeXpath, attributeKey, attributeValue, matchType );
        xmlMatchersForThisFile.add( newMatcher );
    }

    public void addSkipXmlNodeValueMatcher( String directoryAlias, String filePathInThisDirectory,
                                            String nodeXpath, String value,
                                            SkipContentMatcher.MATCH_TYPE matchType ) {

        List<SkipXmlNodeMatcher> xmlMatchersForThisFile = getSkipXmlNodeAttributeMatcher( filePathInThisDirectory );

        SkipXmlNodeMatcher newMatcher = new SkipXmlNodeMatcher( directoryAlias, filePathInThisDirectory );
        newMatcher.addNodeValueMatcher( nodeXpath, value, matchType );
        xmlMatchersForThisFile.add( newMatcher );
    }

    private List<SkipXmlNodeMatcher> getSkipXmlNodeAttributeMatcher( String filePathInThisDirectory ) {

        // cycle all matchers about this file
        if( this.xmlMatchersMap.containsKey( filePathInThisDirectory ) ) {
            return this.xmlMatchersMap.get( filePathInThisDirectory );
        }

        // no appropriate matcher is present for this file, add one
        List<SkipXmlNodeMatcher> xmlMatchersForThisFile = this.xmlMatchersMap.get( filePathInThisDirectory );
        xmlMatchersForThisFile = new ArrayList<>();
        this.xmlMatchersMap.put( filePathInThisDirectory, xmlMatchersForThisFile );

        return xmlMatchersForThisFile;
    }
   
    public Map<String, List<SkipIniMatcher>> getIniMatchersMap() {

        return iniMatchersMap;
    }

    public List<SkipIniMatcher> getIniMatchers( String filePath ) {

        return iniMatchersMap.get( filePath );
    }

    public void addSkipIniMatcher( String directoryAlias, String filePathInThisDirectory, String section,
                                   String token, MATCH_ENTITY matchEntity,
                                   MATCH_TYPE matchType ) {

        List<SkipIniMatcher> iniMatchersForThisFile = this.iniMatchersMap.get( filePathInThisDirectory );

        // cycle all matchers about this file
        if( this.iniMatchersMap.containsKey( filePathInThisDirectory ) ) {
            for( SkipIniMatcher matcher : this.iniMatchersMap.get( filePathInThisDirectory ) ) {
                // there is already a matcher for this file
                // add the new condition
                if( matchEntity == MATCH_ENTITY.SECTION ) {
                    matcher.addSectionCondition( section, matchType );
                } else if( matchEntity == MATCH_ENTITY.KEY ) {
                    matcher.addKeyCondition( section, token, matchType );
                } else { // matchEntityType == MATCH_ENTITY.VALUE
                    matcher.addValueCondition( section, token, matchType );
                }
                return;
            }
        }

        // no appropriate matcher is present for this file, add one
        if( iniMatchersForThisFile == null ) {
            iniMatchersForThisFile = new ArrayList<>();
            this.iniMatchersMap.put( filePathInThisDirectory, iniMatchersForThisFile );
        }

        SkipIniMatcher newMatcher = new SkipIniMatcher( directoryAlias, filePathInThisDirectory );
        if( matchEntity == MATCH_ENTITY.SECTION ) {
            newMatcher.addSectionCondition( section, matchType );
        } else if( matchEntity == MATCH_ENTITY.KEY ) {
            newMatcher.addKeyCondition( section, token, matchType );
        } else { // matchEntityType == MATCH_ENTITY.VALUE
            newMatcher.addValueCondition( section, token, matchType );
        }
        iniMatchersForThisFile.add( newMatcher );
    }

    public List<SkipTextLineMatcher> getTextLineMatchers( String filePath ) {

        return textMatchersMap.get( filePath );
    }

    public Map<String, List<SkipTextLineMatcher>> getTextLineMatchersMap() {

        return textMatchersMap;
    }

    public void addSkipTextLineMatcher( String directoryAlias, String filePathInThisDirectory, String line,
                                        MATCH_TYPE matchType ) {

        List<SkipTextLineMatcher> textMatchersForThisFile = this.textMatchersMap.get( filePathInThisDirectory );

        // cycle all matchers about this file
        if( this.textMatchersMap.containsKey( filePathInThisDirectory ) ) {
            for( SkipTextLineMatcher matcher : this.textMatchersMap.get( filePathInThisDirectory ) ) {
                // there is already a matcher for this file
                // add the new condition
                matcher.addLineCondition( line, matchType );
                return;
            }
        }

        // no appropriate matcher is present for this file, add one
        if( textMatchersForThisFile == null ) {
            textMatchersForThisFile = new ArrayList<>();
            this.textMatchersMap.put( filePathInThisDirectory, textMatchersForThisFile );
        }

        SkipTextLineMatcher newMatcher = new SkipTextLineMatcher( directoryAlias, filePathInThisDirectory );
        newMatcher.addLineCondition( line, matchType );
        textMatchersForThisFile.add( newMatcher );
    }
}
