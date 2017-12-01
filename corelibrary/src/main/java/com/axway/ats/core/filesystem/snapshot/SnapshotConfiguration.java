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
package com.axway.ats.core.filesystem.snapshot;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The configuration values used when taking a snapshot.
 * It is serializable, because it is send to remote instances as well
 */
public class SnapshotConfiguration implements Serializable {

    private static final long serialVersionUID     = 1L;

    private boolean           checkModificationTime;
    private boolean           checkSize;
    private boolean           checkMD5;
    private boolean           checkPermissions;
    private boolean           supportHidden;

    // settings for working with file content
    // Property files
    private boolean           checkPropertiesFilesContent;
    private Set<String>       propertiesFileExtensions;

    // XML files
    private boolean           checkXmlFilesContent;
    private Set<String>       xmlFileExtensions;

    // INI files    
    private boolean           checkIniFilesContent;
    private char              iniFilesStartComment = '#';
    private char              iniFilesStartSection = '[';
    private char              iniFilesDelimiter    = '=';
    private Set<String>       iniFileExtensions;

    // TEXT files
    private boolean           checkTextFilesContent;
    private Set<String>       textFileExtensions;

    public SnapshotConfiguration() {
        // set default values

        propertiesFileExtensions = new HashSet<>();
        propertiesFileExtensions.add(".properties");

        xmlFileExtensions = new HashSet<>();
        xmlFileExtensions.add(".xml");

        iniFileExtensions = new HashSet<>();
        iniFileExtensions.add(".ini");

        textFileExtensions = new HashSet<>();
        textFileExtensions.add(".txt");

        iniFilesStartComment = '#';
        iniFilesStartSection = '[';
        iniFilesDelimiter = '=';
    }

    public SnapshotConfiguration newCopy() {

        SnapshotConfiguration copy = new SnapshotConfiguration();

        copy.checkModificationTime = checkModificationTime;
        copy.checkSize = checkSize;
        copy.checkMD5 = checkMD5;
        copy.checkPermissions = checkPermissions;
        copy.supportHidden = supportHidden;

        copy.checkPropertiesFilesContent = checkPropertiesFilesContent;
        copy.propertiesFileExtensions = propertiesFileExtensions;

        copy.checkXmlFilesContent = checkXmlFilesContent;
        copy.xmlFileExtensions = xmlFileExtensions;

        copy.checkIniFilesContent = checkIniFilesContent;
        copy.iniFileExtensions = iniFileExtensions;

        copy.checkTextFilesContent = checkTextFilesContent;
        copy.textFileExtensions = textFileExtensions;

        return copy;
    }

    public enum FileType {
        REGULAR, XML, PROPERTIES, INI, TEXT
    }

    public boolean isCheckModificationTime() {

        return checkModificationTime;
    }

    public void setCheckModificationTime( boolean checkModificationTime ) {

        this.checkModificationTime = checkModificationTime;
    }

    public boolean isCheckSize() {

        return checkSize;
    }

    public SnapshotConfiguration setCheckSize( boolean checkSize ) {

        this.checkSize = checkSize;

        // return this instance, so we can use it in constructor call
        return this;
    }

    public boolean isCheckMD5() {

        return checkMD5;
    }

    public SnapshotConfiguration setCheckMD5( boolean checkMD5 ) {

        this.checkMD5 = checkMD5;

        // return this instance, so we can use it in constructor call
        return this;
    }

    public boolean isCheckPermissions() {

        return checkPermissions;
    }

    public void setCheckPermissions( boolean checkPermissions ) {

        this.checkPermissions = checkPermissions;
    }

    public boolean isSupportHidden() {

        return supportHidden;
    }

    public void setSupportHidden( boolean supportHidden ) {

        this.supportHidden = supportHidden;
    }

    public boolean isCheckPropertiesFilesContent() {

        return checkPropertiesFilesContent;
    }

    public void setCheckPropertiesFilesContent( boolean checkPropertyFilesContent ) {

        this.checkPropertiesFilesContent = checkPropertyFilesContent;
    }

    public boolean isCheckXmlFilesContent() {

        return checkXmlFilesContent;
    }

    public void setCheckXmlFilesContent( boolean checkXmlFilesContent ) {

        this.checkXmlFilesContent = checkXmlFilesContent;
    }

    public boolean isCheckIniFilesContent() {

        return checkIniFilesContent;
    }

    public void setCheckIniFilesContent( boolean iniFilesCheckContent ) {

        this.checkIniFilesContent = iniFilesCheckContent;
    }

    public char getIniFileStartCommentCharacter() {

        return iniFilesStartComment;
    }

    public void setIniFileStartCommentCharacter( char iniFilesStartCommentLine ) {

        this.iniFilesStartComment = iniFilesStartCommentLine;
    }

    public char getIniFileStartSectionCharacter() {

        return iniFilesStartSection;
    }

    public void setIniFileStartSectionCharacter( char iniFilesStartSectionLine ) {

        this.iniFilesStartSection = iniFilesStartSectionLine;
    }

    public char getIniFileDelimiterCharacter() {

        return iniFilesDelimiter;
    }

    public void setIniFileDelimiterCharacter( char iniFilesPropertyDelimiter ) {

        this.iniFilesDelimiter = iniFilesPropertyDelimiter;
    }

    public boolean isCheckTextFilesContent() {

        return checkTextFilesContent;
    }

    public void setCheckTextFilesContent( boolean textFilesCheckContent ) {

        this.checkTextFilesContent = textFilesCheckContent;
    }

    /**
     * Set file extensions that will be treated as Properties files.
     * Default extension is '.properties'
     * @param extensions new extensions
     */
    public void setPropertiesFileExtensions( String[] extensions ) {

        propertiesFileExtensions.clear();
        for (String extension : extensions) {
            propertiesFileExtensions.add(extension);
        }
    }

    /**
     * Set file extensions that will be treated as XML files.
     * Default extension is '.xml'
     * @param extensions new extensions
     */
    public void setXmlFileExtensions( String[] extensions ) {

        xmlFileExtensions.clear();
        for (String extension : extensions) {
            xmlFileExtensions.add(extension);
        }
    }

    /**
     * Set file extensions that will be treated as INI files.
     * Default extension is '.ini'
     * @param extensions new extensions
     */
    public void setIniFileExtensions( String[] extensions ) {

        iniFileExtensions.clear();
        for (String extension : extensions) {
            iniFileExtensions.add(extension);
        }
    }

    /**
     * Set file extensions that will be treated as TEXT files.
     * Default extension is '.text'
     * @param extensions new extensions
     */
    public void setTextFileExtensions( String[] extensions ) {

        textFileExtensions.clear();
        for (String extension : extensions) {
            textFileExtensions.add(extension);
        }
    }

    /**
     * @param filePath
     * @return what type of file we are dealing with
     */
    public FileType getFileType( String filePath ) {

        for (String xmlExt : xmlFileExtensions) {
            if (filePath.endsWith(xmlExt)) {
                // it is XML file
                return checkXmlFilesContent
                                            ? FileType.XML
                                            : FileType.REGULAR;
            }
        }

        for (String propertiesExt : propertiesFileExtensions) {
            if (filePath.endsWith(propertiesExt)) {
                // it is Properties file
                return checkPropertiesFilesContent
                                                   ? FileType.PROPERTIES
                                                   : FileType.REGULAR;
            }
        }

        for (String iniExt : iniFileExtensions) {
            if (filePath.endsWith(iniExt)) {
                // it is INI file
                return checkIniFilesContent
                                            ? FileType.INI
                                            : FileType.REGULAR;
            }
        }

        for (String textExt : textFileExtensions) {
            if (filePath.endsWith(textExt)) {
                // it is INI file
                return checkTextFilesContent
                                             ? FileType.TEXT
                                             : FileType.REGULAR;
            }
        }

        // it is a regular file
        return FileType.REGULAR;
    }
}
