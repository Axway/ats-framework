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

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.common.filesystem.snapshot.equality.FileSystemEqualityState;
import com.axway.ats.core.filesystem.snapshot.matchers.FileMatchersContainer;
import com.axway.ats.core.filesystem.snapshot.matchers.FindRules;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipContentMatcher.MATCH_TYPE;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipIniMatcher;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipIniMatcher.MATCH_ENTITY;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipPropertyMatcher;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipTextLineMatcher;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipXmlNodeMatcher;
import com.axway.ats.core.filesystem.snapshot.types.FileSnapshot;
import com.axway.ats.core.filesystem.snapshot.types.IniFileSnapshot;
import com.axway.ats.core.filesystem.snapshot.types.PropertiesFileSnapshot;
import com.axway.ats.core.filesystem.snapshot.types.TextFileSnapshot;
import com.axway.ats.core.filesystem.snapshot.types.XmlFileSnapshot;
import com.axway.ats.core.utils.IoUtils;

public class DirectorySnapshot implements Serializable {

    private static final long              serialVersionUID  = 1L;

    private static final Logger            log               = LogManager.getLogger(DirectorySnapshot.class);

    // absolute path to this directory
    private String                         path;

    // <path, sub-directory snapshot>
    private Map<String, DirectorySnapshot> subdirSnapshots   = new HashMap<String, DirectorySnapshot>();
    // <path, file snapshot>
    private Map<String, FileSnapshot>      fileSnapshots     = new HashMap<String, FileSnapshot>();

    // container for all matchers for all files in this directory
    private FileMatchersContainer          matchersContainer = new FileMatchersContainer();

    // sub-directories to skip
    private Set<Map<String, Boolean>>      skippedSubDirs    = new HashSet<Map<String, Boolean>>();

    // keeps info how equal both directories are 
    private FileSystemEqualityState        equality;

    DirectorySnapshot( String path,
                       FileSystemEqualityState equality ) {

        this.path = IoUtils.normalizeUnixDir(path.trim());
        this.equality = equality;
    }

    DirectorySnapshot( String path,
                       FileMatchersContainer matchersContainer,
                       FileSystemEqualityState equality ) {

        this.path = IoUtils.normalizeUnixDir(path.trim());
        this.equality = equality;
        if (matchersContainer != null) {
            this.matchersContainer = matchersContainer;
        }
    }

    DirectorySnapshot( String path,
                       Map<String, FindRules> fileRules,
                       FileSystemEqualityState equality ) {

        this.path = IoUtils.normalizeUnixDir(path.trim());
        this.equality = equality;
        this.matchersContainer.addFileAttributesMap(fileRules);
    }

    /**
     * Create an instance from a file
     * @param dirNode
     * @param equality
     */
    static DirectorySnapshot fromFile(
                                       Element dirNode,
                                       FileSystemEqualityState equality ) {

        // this dir
        String dirPath = dirNode.getAttributes().getNamedItem("path").getNodeValue();

        DirectorySnapshot dirSnapshot = new DirectorySnapshot(dirPath, equality);

        // its file find rules
        List<Element> fileRuleNodes = SnapshotUtils.getChildrenByTagName(dirNode,
                                                                         LocalFileSystemSnapshot.NODE_FILE_RULE);
        for (Element fileRuleNode : fileRuleNodes) {
            dirSnapshot.matchersContainer.addFileAttributes(fileRuleNode.getAttribute("file"),
                                                            FindRules.getFromString(fileRuleNode.getAttribute("rules")));
        }

        // its skipped sub-directories
        List<Element> skippedSubDirNodes = SnapshotUtils.getChildrenByTagName(dirNode,
                                                                              LocalFileSystemSnapshot.NODE_SKIPPED_DIRECTORY);
        for (Element skippedSubDirNode : skippedSubDirNodes) {

            Map<String, Boolean> mapEntry = new HashMap<>();
            mapEntry.put(skippedSubDirNode.getAttribute("name"),
                         Boolean.parseBoolean(skippedSubDirNode.getAttribute("lastTokenIsRegex")));
            dirSnapshot.skippedSubDirs.add(mapEntry);
        }

        // its files
        List<Element> fileNodes = SnapshotUtils.getChildrenByTagName(dirNode,
                                                                     LocalFileSystemSnapshot.NODE_FILE);
        for (Element fileNode : fileNodes) {
            FileSnapshot fileSnapshot = FileSnapshot.fromFile(fileNode);

            dirSnapshot.fileSnapshots.put(IoUtils.getFileName(fileSnapshot.getPath()), fileSnapshot);
        }

        // its subdirs
        List<Element> subdirNodes = SnapshotUtils.getChildrenByTagName(dirNode,
                                                                       LocalFileSystemSnapshot.NODE_DIRECTORY);
        for (Element subdirNode : subdirNodes) {

            DirectorySnapshot subdirSnapshot = DirectorySnapshot.fromFile(subdirNode, equality);
            dirSnapshot.subdirSnapshots.put(SnapshotUtils.getDirPathLastToken(subdirSnapshot.getPath()),
                                            subdirSnapshot);
        }

        return dirSnapshot;
    }

    String getPath() {

        return this.path;
    }

    void skipSubDirectory(
                           String dirPath,
                           boolean lastTokenIsRegex ) {

        /*
         * This method is called only for top level directories.
         *
         * Prior to taking the snapshot, we do not know the content of this root directory.
         * So here we just remember the dirs that are to be skipped.
         *
         * Currently the only rule for a directory is to SKIP it
         */
        Map<String, Boolean> entry = new HashMap<>();
        entry.put(dirPath, lastTokenIsRegex);
        skippedSubDirs.add(entry);
    }

    Set<Map<String, Boolean>> getSkippedSubDirectories() {

        return skippedSubDirs;
    }

    void addFindRules(
                       String filePathInThisDirectory,
                       int... rules ) {

        /*
         * This method is called only for top level directories.
         *
         * Prior to taking the snapshot, we do not know the content of this root directory.
         * So here we just remember the rules, but we will apply them later on the sub directories.
         */
        this.matchersContainer.addFileAttributes(filePathInThisDirectory, new FindRules(rules));
    }

    void addSkipPropertyMatcher( String directoryAlias, String filePathInThisDirectory,
                                 SkipPropertyMatcher matcher ) {

        // keys
        Map<String, MATCH_TYPE> keysMap = matcher.getKeysMap();
        for (String key : keysMap.keySet()) {
            addSkipPropertyMatcher(directoryAlias, filePathInThisDirectory, key, true, keysMap.get(key));
        }

        // values
        Map<String, MATCH_TYPE> valuesMap = matcher.getValuesMap();
        for (String value : valuesMap.keySet()) {
            addSkipPropertyMatcher(directoryAlias, filePathInThisDirectory, value, false,
                                   valuesMap.get(value));
        }
    }

    void addSkipPropertyMatcher( String directoryAlias, String filePathInThisDirectory, String token,
                                 boolean isMatchingKey, SkipPropertyMatcher.MATCH_TYPE matchType ) {

        matchersContainer.addSkipPropertyMatcher(directoryAlias, filePathInThisDirectory, token,
                                                 isMatchingKey, matchType);
    }

    void addSkipXmlNodeMatcher( String directoryAlias, String filePathInThisDirectory,
                                SkipXmlNodeMatcher matcher ) {

        for (SkipXmlNodeMatcher.NodeValueMatcher nodeMatcher : matcher.getNodeValueMatchers()) {
            addSkipXmlNodeMatcher(directoryAlias, filePathInThisDirectory, nodeMatcher.getXpath(),
                                  nodeMatcher.getValue(), nodeMatcher.getMatchType());
        }
    }

    void addSkipXmlNodeMatcher( String directoryAlias, String filePathInThisDirectory, String nodeXpath,
                                String value, SkipXmlNodeMatcher.MATCH_TYPE matchType ) {

        matchersContainer.addSkipXmlNodeValueMatcher(directoryAlias, filePathInThisDirectory, nodeXpath,
                                                     value, matchType);
    }

    void addSkipXmlNodeMatcher( String directoryAlias, String filePathInThisDirectory,
                                String nodeXpath, String attributeKey, String attributeValue,
                                SkipXmlNodeMatcher.MATCH_TYPE matchType ) {

        matchersContainer.addSkipXmlNodeAttributeMatcher(directoryAlias, filePathInThisDirectory, nodeXpath,
                                                         attributeKey, attributeValue, matchType);
    }

    void addSkipIniMatcher( String directoryAlias, String filePathInThisDirectory, SkipIniMatcher matcher ) {

        // sections
        for (Entry<String, MATCH_TYPE> sectionEntity : matcher.getSectionsMap().entrySet()) {
            String section = sectionEntity.getKey();
            MATCH_TYPE matchType = sectionEntity.getValue();
            addSkipIniMatcher(directoryAlias, filePathInThisDirectory, section, null, MATCH_ENTITY.SECTION,
                              matchType);
        }

        // keys
        for (Entry<String, Map<String, MATCH_TYPE>> sectionEntity : matcher.getKeysMap().entrySet()) {
            String section = sectionEntity.getKey();
            for (Entry<String, MATCH_TYPE> propertyEntity : sectionEntity.getValue().entrySet()) {
                String key = propertyEntity.getKey();
                MATCH_TYPE matchType = propertyEntity.getValue();
                addSkipIniMatcher(directoryAlias, filePathInThisDirectory, section, key, MATCH_ENTITY.KEY,
                                  matchType);
            }
        }

        // values
        for (Entry<String, Map<String, MATCH_TYPE>> sectionEntity : matcher.getValuesMap().entrySet()) {
            String section = sectionEntity.getKey();
            for (Entry<String, MATCH_TYPE> propertyEntity : sectionEntity.getValue().entrySet()) {
                String value = propertyEntity.getKey();
                MATCH_TYPE matchType = propertyEntity.getValue();
                addSkipIniMatcher(directoryAlias, filePathInThisDirectory, section, value,
                                  MATCH_ENTITY.VALUE, matchType);
            }
        }
    }

    void addSkipIniMatcher( String directoryAlias, String filePathInThisDirectory, String section,
                            String token, MATCH_ENTITY matchEntity, MATCH_TYPE matchType ) {

        matchersContainer.addSkipIniMatcher(directoryAlias, filePathInThisDirectory, section, token,
                                            matchEntity, matchType);
    }

    void addSkipTextLineMatcher( String directoryAlias, String filePathInThisDirectory, String line,
                                 MATCH_TYPE matchType ) {

        matchersContainer.addSkipTextLineMatcher(directoryAlias, filePathInThisDirectory, line, matchType);
    }

    void addSkipTextLineMatcher( String directoryAlias, String filePathInThisDirectory,
                                 SkipTextLineMatcher matcher ) {

        for (Entry<String, MATCH_TYPE> entity : matcher.getMatchersMap().entrySet()) {
            String line = entity.getKey();
            MATCH_TYPE matchType = entity.getValue();
            addSkipTextLineMatcher(directoryAlias, filePathInThisDirectory, line, matchType);
        }
    }

    void takeSnapshot( SnapshotConfiguration configuration ) {

        log.debug("Add directory " + this.path);

        // do some cleanup - in case user call this method more than once
        subdirSnapshots.clear();
        fileSnapshots.clear();

        if (!new File(this.path).exists()) {
            throw new FileSystemSnapshotException("Directory '" + this.path + "' does not exist");
        }

        // take the snapshot now by traversing all files and sub-directories
        for (File fsEntity : new File(this.path).listFiles()) {
            if (!fsEntity.isHidden() || configuration.isSupportHidden()) {
                if (fsEntity.isDirectory()) {
                    boolean skipSubDir = false;
                    // make a directory snapshot
                    String unixDirName = IoUtils.normalizeUnixDir(fsEntity.getName()); // skipped dirs are also in UnixDir (ends with '/') format
                    Iterator<Map<String, Boolean>> it = skippedSubDirs.iterator();
                    while (it.hasNext()) {
                        // each set entry is a map, that contains exactly one key-value pair
                        Map<String, Boolean> setEntry = it.next();
                        String subDirName = setEntry.keySet().iterator().next();
                        boolean lastTokenIsRegex = setEntry.get(subDirName);
                        if (lastTokenIsRegex) {
                            Pattern p = Pattern.compile(subDirName);
                            Matcher m = p.matcher(unixDirName);
                            if (m.find()) {
                                skipSubDir = true;// skip this sub directory
                                break;
                            }
                        } else {
                            if (subDirName.equals(unixDirName)) {
                                skipSubDir = true;// skip this sub directory
                                break;
                            }
                        }
                    }
                    if (!skipSubDir) {
                        DirectorySnapshot subdirSnapshot = generateSubDirectorySnapshot(unixDirName, fsEntity);
                        subdirSnapshot.takeSnapshot(configuration);
                        subdirSnapshots.put(fsEntity.getName(), subdirSnapshot);
                    }
                } else {
                    // make a file snapshot
                    FileSnapshot fileSnapshot = generateFileSnapshot(configuration, fsEntity);
                    if (fileSnapshot != null) { // if the file is not skipped
                        log.debug("Add " + fileSnapshot.toString());
                        fileSnapshots.put(fsEntity.getName(), fileSnapshot);
                    }
                }
            } else {
                log.debug("The hidden " + (fsEntity.isDirectory()
                                                                  ? "directory"
                                                                  : "file")
                          + " '" + fsEntity.getAbsolutePath() + "' will not be processed");
            }
        }
    }

    private DirectorySnapshot generateSubDirectorySnapshot( String unixDirName, File file ) {

        // extract the matchers for this sub-directory(if any)
        FileMatchersContainer subDirMatchersContainer = this.matchersContainer.getMatchersContainerForSubdir(unixDirName);
        DirectorySnapshot subDirSnapshot = new DirectorySnapshot(file.getAbsolutePath(),
                                                                 subDirMatchersContainer, equality);

        // pass sub-dir skipped directories
        for (Map<String, Boolean> skippedSubDir : skippedSubDirs) {
            String skippedSubDirPath = skippedSubDir.keySet().iterator().next();
            boolean lastTokenIsRegex = skippedSubDir.get(skippedSubDirPath);
            if (skippedSubDirPath.startsWith(unixDirName)) {
                // dirName ends with '/', so skippedSubDirPath contains sub-dir of dirName
                String subDirToSkip = skippedSubDirPath.substring(unixDirName.length());
                subDirSnapshot.skipSubDirectory(subDirToSkip, lastTokenIsRegex);
            }
        }

        return subDirSnapshot;
    }

    private FileSnapshot generateFileSnapshot(
                                               SnapshotConfiguration configuration,
                                               File file ) {

        // find file rules and check whether the file is skipped
        FindRules rules = null;
        for (String fileName : this.matchersContainer.fileAttributesMap.keySet()) {

            FindRules currentRules = this.matchersContainer.fileAttributesMap.get(fileName);
            if (currentRules.isSearchFilenameByRegex()) {
                if (file.getName().matches(fileName)) {
                    rules = currentRules;
                    break;
                }
            } else if (fileName.equals(file.getName())) {
                rules = currentRules;
                break;
            }
        }
        if (rules != null && rules.isSkipFilePath()) {
            // file is skipped
            return null;
        }

        // create the right type of file snapshot
        switch (configuration.getFileType(file.getName().toLowerCase())) {
            case PROPERTIES:
                return new PropertiesFileSnapshot(configuration, file.getAbsolutePath(), rules,
                                                  this.matchersContainer.getPropertyMatchers(file.getName()));
            case XML:
                return new XmlFileSnapshot(configuration, file.getAbsolutePath(), rules,
                                           this.matchersContainer.getXmlNodeMatchers(file.getName()));
            case INI:
                return new IniFileSnapshot(configuration, file.getAbsolutePath(), rules,
                                           this.matchersContainer.getIniMatchers(file.getName()));
            case TEXT:
                return new TextFileSnapshot(configuration, file.getAbsolutePath(), rules,
                                            this.matchersContainer.getTextLineMatchers(file.getName()));
            default:
                return new FileSnapshot(configuration, file.getAbsolutePath(), rules);
        }
    }

    void compare(
                  String thisSnapshotName,
                  String thatSnapshotName,
                  DirectorySnapshot that,
                  boolean checkDirName,
                  FileSystemEqualityState equality ) {

        // check the last entity in the dir path
        if (checkDirName) {
            String thisDirName = SnapshotUtils.getDirPathLastToken(this.path);
            String thatDirName = SnapshotUtils.getDirPathLastToken(that.path);

            if (!thisDirName.equals(thatDirName)) {
                throw new FileSystemSnapshotException("Directory name " + thisDirName + " of " + this.path
                                                      + " is not the same as directory name " + thatDirName + " of "
                                                      + that.path);
            }
        }

        // check the files in this directory
        SnapshotUtils.checkFileSnapshots(thisSnapshotName,
                                         this.fileSnapshots,
                                         thatSnapshotName,
                                         that.fileSnapshots,
                                         equality);

        // check the sub-directories
        SnapshotUtils.checkDirSnapshotsDeepLevel(thisSnapshotName,
                                                 this.subdirSnapshots,
                                                 thatSnapshotName,
                                                 that.subdirSnapshots,
                                                 equality);
    }

    void toFile(
                 Document dom,
                 Element dirSnapshotNode ) {

        // this dir
        dirSnapshotNode.setAttribute("path", this.path);

        // its file find rules
        for (String fileName : this.matchersContainer.fileAttributesMap.keySet()) {

            Element fileRuleNode = dom.createElement(LocalFileSystemSnapshot.NODE_FILE_RULE);
            dirSnapshotNode.appendChild(fileRuleNode);

            fileRuleNode.setAttribute("rules",
                                      this.matchersContainer.getFileAtrtibutes(fileName).getAsString());

            fileRuleNode.setAttribute("file", fileName);
        }

        // its skipped sub-directories
        for (Map<String, Boolean> mapEntry : this.skippedSubDirs) {
            String skippedSubDirPath = mapEntry.keySet().iterator().next();
            Boolean lastTokenIsRegex = mapEntry.get(skippedSubDirPath);
            Element skippedSubDirNode = dom.createElement(LocalFileSystemSnapshot.NODE_SKIPPED_DIRECTORY);
            dirSnapshotNode.appendChild(skippedSubDirNode);

            skippedSubDirNode.setAttribute("name", skippedSubDirPath);
            skippedSubDirNode.setAttribute("lastTokenIsRegex", String.valueOf(lastTokenIsRegex));
        }

        // its files
        for (String fileAlias : this.fileSnapshots.keySet()) {

            Element fileSnapshotNode = dom.createElement(LocalFileSystemSnapshot.NODE_FILE);
            dirSnapshotNode.appendChild(fileSnapshotNode);

            fileSnapshotNode.setAttribute("alias", fileAlias);
            fileSnapshotNode = this.fileSnapshots.get(fileAlias).toFile(dom, fileSnapshotNode);

        }

        // its subdirs
        for (String dirSnapshotName : this.subdirSnapshots.keySet()) {

            Element subdirSnapshotNode = dom.createElement(LocalFileSystemSnapshot.NODE_DIRECTORY);
            dirSnapshotNode.appendChild(subdirSnapshotNode);

            subdirSnapshotNode.setAttribute("alias", dirSnapshotName);
            this.subdirSnapshots.get(dirSnapshotName).toFile(dom, subdirSnapshotNode);
        }
    }

    public Map<String, DirectorySnapshot> getDirSnapshots() {

        return this.subdirSnapshots;
    }

    public Map<String, FindRules> getFileRules() {

        return this.matchersContainer.fileAttributesMap;
    }

    public Map<String, List<SkipPropertyMatcher>> getPropertyMatchersPerFile() {

        return this.matchersContainer.getPropertyMatchersMap();
    }

    public Map<String, List<SkipXmlNodeMatcher>> getXmlNodeMatchersPerFile() {

        return this.matchersContainer.getXmlNodeMatchersMap();
    }

    public Map<String, List<SkipIniMatcher>> getIniMatchersPerFile() {

        return this.matchersContainer.getIniMatchersMap();
    }

    public Map<String, List<SkipTextLineMatcher>> getTextLineMatchersPerFile() {

        return this.matchersContainer.getTextLineMatchersMap();
    }

    /**
     * This method is good for debug purpose
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("\n dir: " + this.path + "\n");

        for (FileSnapshot f : fileSnapshots.values()) {
            sb.append(f.toString() + "\n");
        }

        for (DirectorySnapshot d : subdirSnapshots.values()) {
            sb.append(d.toString());
        }
        return sb.toString();
    }
}
