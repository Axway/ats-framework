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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.common.filesystem.snapshot.equality.FileSystemEqualityState;
import com.axway.ats.core.filesystem.snapshot.matchers.FindRules;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipContentMatcher.MATCH_TYPE;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipIniMatcher.MATCH_ENTITY;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

public class LocalFileSystemSnapshot implements IFileSystemSnapshot, Serializable {

    private static final long              serialVersionUID             = 1L;

    private static Logger                  log                          = Logger.getLogger(LocalFileSystemSnapshot.class);

    /**
     * Skip checking the size of a file
     */
    @PublicAtsApi
    public static final int                SKIP_FILE_SIZE               = FindRules.SKIP_FILE_SIZE;
    /**
     * Skip checking the last modification time of a file
     */
    @PublicAtsApi
    public static final int                SKIP_FILE_MODIFICATION_TIME  = FindRules.SKIP_FILE_MODIFICATION_TIME;
    /**
     * Skip checking the MD5 sum of a file
     */
    @PublicAtsApi
    public static final int                SKIP_FILE_MD5                = FindRules.SKIP_FILE_MD5_SUM;
    /**
     * Skip checking the permissions attribute of a file
     */
    @PublicAtsApi
    public static final int                SKIP_FILE_PERMISSIONS        = FindRules.SKIP_FILE_PERMISSIONS;

    /**
     * Must check the size of a file. This value will override the global settings when used
     */
    @PublicAtsApi
    public static final int                CHECK_FILE_SIZE              = FindRules.CHECK_FILE_SIZE;
    /**
     * Must check the last modification timestamp of a file. This value will override the global settings when used
     */
    @PublicAtsApi
    public static final int                CHECK_FILE_MODIFICATION_TIME = FindRules.CHECK_FILE_MODIFICATION_TIME;
    /**
     * Must check the MD5 sum of a file. This value will override the global settings when used
     */
    @PublicAtsApi
    public static final int                CHECK_FILE_MD5               = FindRules.CHECK_FILE_MD5_SUM;
    /**
     * Must check the permissions attribute of a file. This value will override the global settings when used
     */
    @PublicAtsApi
    public static final int                CHECK_FILE_PERMISSIONS       = FindRules.CHECK_FILE_PERMISSIONS;

    // XML nodes used when saving/loading snapshots from files
    static final String                    NODE_FILE_SYSTEM             = "FILE_SYSTEM_SNAPSHOT";
    static final String                    NODE_DIRECTORY               = "DIRECTORY";
    static final String                    NODE_SKIPPED_DIRECTORY       = "SKIPPED_DIRECTORY";
    static final String                    NODE_FILE                    = "FILE";
    static final String                    NODE_FILE_RULE               = "FILE_RULES";

    // the snapshot name
    private String                         name;

    // the time this snapshot is made
    private long                           snapshotTimestamp            = -1;

    // list of directories in this snapshot
    private Map<String, DirectorySnapshot> dirSnapshots                 = new HashMap<String, DirectorySnapshot>();

    // this structure can say how equal are the snapshots
    private FileSystemEqualityState        equality;

    // define what which attributes we want to check
    private SnapshotConfiguration          configuration;

    // when a snapshot is taken on a remote host, we remember that host
    // we need it when reading file content to compare
    private String                         remoteAgent;

    public LocalFileSystemSnapshot( String name, SnapshotConfiguration configuration ) {

        if (StringUtils.isNullOrEmpty(name)) {
            throw new FileSystemSnapshotException("Invalid snapshot name '" + name + "'");
        }

        this.name = name.trim();
        this.configuration = configuration;
    }

    public IFileSystemSnapshot newSnapshot( String newSnapshotName ) {

        LocalFileSystemSnapshot newFileSystemSnapshot = new LocalFileSystemSnapshot(newSnapshotName,
                                                                                    configuration);

        for (String dirAlias : dirSnapshots.keySet()) {
            newFileSystemSnapshot.addDirectory(dirAlias, dirSnapshots.get(dirAlias).getPath());
        }

        return newFileSystemSnapshot;
    }

    public void setRemoteAgent( String remoteAgent ) {

        this.remoteAgent = remoteAgent;
    }

    @Override
    public void setName( String newName ) {

        /*
         * This method is called only when loading a snapshot from a file.
         * In such case, user can overwrite the snapshot name coming from the file.
         */
        if (StringUtils.isNullOrEmpty(name)) {
            // user wants to use the snapshot name as comes from the file
            return;
        } else {
            // user wants to overwrite the snapshot name coming from the file.
            this.name = newName;
        }
    }

    @Override
    public void addDirectory( String directoryAlias, String directoryPath ) {

        directoryAlias = parseDirectoryAlias(directoryAlias);
        directoryPath = parseDirectoryPath(directoryPath, false);

        if (dirSnapshots.keySet().contains(directoryAlias)) {
            throw new FileSystemSnapshotException("There is already a directory with alias '"
                                                  + directoryAlias + "' for snapshot '" + name + "'");
        }

        dirSnapshots.put(directoryAlias, new DirectorySnapshot(directoryPath, equality));
    }

    @Override
    public void skipDirectory( String rootDirectoryAlias, String relativeDirectoryPath ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeDirectoryPath = makePathRelative(parseDirectoryPath(relativeDirectoryPath, false));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.skipSubDirectory(relativeDirectoryPath, false);
    }

    @Override
    public void skipDirectoryByRegex( String rootDirectoryAlias, String relativeDirectoryPath ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeDirectoryPath = makePathRelative(parseDirectoryPath(relativeDirectoryPath, true));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.skipSubDirectory(relativeDirectoryPath, true);
    }

    @Override
    public void skipFile( String rootDirectoryAlias, String relativeFilePath, int... skipRules ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));
        skipRules = parseFindRules(skipRules);

        skipRules = skipMd5IfWantToSkipFileSize(skipRules);

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        if (skipRules.length == 0) {
            skipRules = new int[]{ FindRules.SKIP_FILE_PATH };
        }
        dirSnapshot.addFindRules(relativeFilePath, skipRules);
    }

    @Override
    public void skipPropertyWithKey( String rootDirectoryAlias, String relativeFilePath, String key,
                                     String matchType ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.addSkipPropertyMatcher(rootDirectoryAlias, relativeFilePath, key, true,
                                           MATCH_TYPE.valueOf(matchType));
    }

    @Override
    public void skipPropertyWithValue( String rootDirectoryAlias, String relativeFilePath, String value,
                                       String matchType ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.addSkipPropertyMatcher(rootDirectoryAlias, relativeFilePath, value, false,
                                           MATCH_TYPE.valueOf(matchType));
    }

    @Override
    public void skipNodeByAttribute( String rootDirectoryAlias, String relativeFilePath, String nodeXpath,
                                     String attributeKey, String attributeValue,
                                     String attributeValueMatchType ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.addSkipXmlNodeMatcher(rootDirectoryAlias, relativeFilePath, nodeXpath, attributeKey,
                                          attributeValue,
                                          MATCH_TYPE.valueOf(attributeValueMatchType));
    }

    @Override
    public void skipNodeByValue( String rootDirectoryAlias, String relativeFilePath, String nodeXpath,
                                 String value, String matchType ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.addSkipXmlNodeMatcher(rootDirectoryAlias, relativeFilePath, nodeXpath, value,
                                          MATCH_TYPE.valueOf(matchType));
    }

    @Override
    public void skipIniSection( String rootDirectoryAlias, String relativeFilePath, String section,
                                String matchType ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.addSkipIniMatcher(rootDirectoryAlias, relativeFilePath, section, null,
                                      MATCH_ENTITY.SECTION,
                                      MATCH_TYPE.valueOf(matchType));
    }

    @Override
    public void skipIniPropertyWithKey( String rootDirectoryAlias, String relativeFilePath, String section,
                                        String propertyKey, String matchType ) {

        if (StringUtils.isNullOrEmpty(propertyKey)) {
            throw new FileSystemSnapshotException("The matching key could not be null or empty.");
        }

        if (StringUtils.isNullOrEmpty(section)) {
            throw new FileSystemSnapshotException("The ini file section could not be null or empty.");
        }

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.addSkipIniMatcher(rootDirectoryAlias, relativeFilePath, section, propertyKey,
                                      MATCH_ENTITY.KEY,
                                      MATCH_TYPE.valueOf(matchType));
    }

    @Override
    public void skipIniPropertyWithValue( String rootDirectoryAlias, String relativeFilePath, String section,
                                          String propertyValue, String matchType ) {

        if (StringUtils.isNullOrEmpty(propertyValue)) {
            throw new FileSystemSnapshotException("The matching value could not be null or empty.");
        }

        if (StringUtils.isNullOrEmpty(section)) {
            throw new FileSystemSnapshotException("The ini file section could not be null or empty.");
        }

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.addSkipIniMatcher(rootDirectoryAlias, relativeFilePath, section, propertyValue, MATCH_ENTITY.VALUE,
                                      MATCH_TYPE.valueOf(matchType));
    }

    @Override
    public void skipTextLine( String rootDirectoryAlias, String relativeFilePath, String line,
                              String matchType ) {

        if (StringUtils.isNullOrEmpty(line)) {
            throw new FileSystemSnapshotException("The matching string could not be null or empty.");
        }

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        dirSnapshot.addSkipTextLineMatcher(rootDirectoryAlias, relativeFilePath, line,
                                           MATCH_TYPE.valueOf(matchType));
    }

    /**
     * When it is expected to have the files' sizes different, we know for sure
     * the MD5 sum is different too.
     *
     * This method makes sure we will not compare the MD5 sum in such case,
     * even user forgot to explicitly request it.
     *
     * @param skipRules
     * @return
     */
    private int[] skipMd5IfWantToSkipFileSize( int[] skipRules ) {

        boolean wantToSkipFileSize = false;
        boolean alreadyWantToSkipFileMd5 = false;

        for (int rule : skipRules) {
            if (rule == SKIP_FILE_SIZE) {
                wantToSkipFileSize = true;
            } else if (rule == SKIP_FILE_MD5) {
                alreadyWantToSkipFileMd5 = true;
            }
        }

        if (wantToSkipFileSize && !alreadyWantToSkipFileMd5) {
            // we should add a skipping file-MD5 rule
            int[] newSkipRules = Arrays.copyOf(skipRules, skipRules.length + 1);
            newSkipRules[skipRules.length] = SKIP_FILE_MD5; // add new last element

            return newSkipRules;
        } else {
            // no need to touch
            return skipRules;
        }
    }

    @Override
    public void skipFileByRegex( String rootDirectoryAlias, String relativeFilePath, int... skipRules ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));
        skipRules = parseFindRules(skipRules);

        skipRules = skipMd5IfWantToSkipFileSize(skipRules);

        Set<Integer> allSkipRules = new HashSet<Integer>();

        // file will be searched by a name regex
        allSkipRules.add(FindRules.SEARCH_FILENAME_AS_REGEX);

        if (skipRules.length == 0) {
            // User did not specify a particular file attribute to be skipped.
            // So the intention is to skip the whole file check
            allSkipRules.add(FindRules.SKIP_FILE_PATH);
        }

        // Add any rules(if any) requested by the user.
        // For example the user may want to skip file modification time on the matched files
        for (int skipRule : skipRules) {
            allSkipRules.add(skipRule);
        }

        // some ugly conversion back to int array
        skipRules = new int[allSkipRules.size()];
        int i = 0;
        for (int skipRule : allSkipRules) {
            skipRules[i] = skipRule;
            i++;
        }

        skipFile(rootDirectoryAlias, relativeFilePath, skipRules);
    }

    @Override
    public void checkFile( String rootDirectoryAlias, String relativeFilePath, int... checkRules ) {

        rootDirectoryAlias = parseDirectoryAlias(rootDirectoryAlias);
        relativeFilePath = makePathRelative(parseFilePath(relativeFilePath));
        checkRules = parseFindRules(checkRules);

        DirectorySnapshot dirSnapshot = getDirectorySnapshot(rootDirectoryAlias);

        if (checkRules.length == 0) {
            checkRules = new int[]{ FindRules.CHECK_FILE_ALL_ATTRIBUTES };
        }
        dirSnapshot.addFindRules(relativeFilePath, checkRules);
    }

    private String parseDirectoryAlias( String directoryAlias ) {

        if (StringUtils.isNullOrEmpty(directoryAlias)) {
            throw new FileSystemSnapshotException("Invalid directory alias '" + directoryAlias + "'");
        }
        return directoryAlias.trim();
    }

    private String parseDirectoryPath( String directoryPath, boolean lastTokenIsRegex ) {

        if (StringUtils.isNullOrEmpty(directoryPath)) {
            throw new FileSystemSnapshotException("Invalid directory path '" + directoryPath + "'");
        }
        if (lastTokenIsRegex) {
            int lastFileSeparatorCharIdx = directoryPath.lastIndexOf(File.separator) + 1;
            // lastFileSeparatorCharIdx is -1 if File.separator is not found, but by adding 1, it will be 0
            // That's why the next check is against 0 and not -1
            if (lastFileSeparatorCharIdx == 0) {
                return directoryPath;
            }
            String lastToken = directoryPath.substring(lastFileSeparatorCharIdx);
            directoryPath = IoUtils.normalizeUnixDir(directoryPath.substring(0, directoryPath.lastIndexOf(lastToken))
                                                                  .trim());
            return directoryPath + lastToken;
        } else {
            return IoUtils.normalizeUnixDir(directoryPath.trim());
        }
    }

    /**
     * Remove leading slash(-es) as path is treated relative
     * @param path in UNIX style
     * @return
     */
    private String makePathRelative( String path ) {

        while (path.startsWith(IoUtils.FORWARD_SLASH)) { // cut leading slashes
            path = path.substring(1);
        }
        return path;
    }

    private DirectorySnapshot getDirectorySnapshot( String rootDirectoryAlias ) {

        DirectorySnapshot dirSnapshot = this.dirSnapshots.get(rootDirectoryAlias);
        if (dirSnapshot == null) {
            throw new FileSystemSnapshotException("There is no directory snapshot with alias '"
                                                  + rootDirectoryAlias + "'");
        } else {
            return dirSnapshot;
        }
    }

    private String parseFilePath( String filePath ) {

        if (StringUtils.isNullOrEmpty(filePath)) {
            throw new FileSystemSnapshotException("Invalid file path '" + filePath + "'");
        }
        return IoUtils.normalizeUnixFile(filePath.trim());
    }

    private int[] parseFindRules( int... findRules ) {

        if (findRules == null || findRules.length == 0) {
            return new int[0];
        }

        // search for invalid rules
        for (int rule : findRules) {
            switch (rule) {

                // user wants to skip some file attributes
                case SKIP_FILE_SIZE:
                case SKIP_FILE_MODIFICATION_TIME:
                case SKIP_FILE_MD5:
                case SKIP_FILE_PERMISSIONS:

                    // user wants to check some file attributes
                case CHECK_FILE_SIZE:
                case CHECK_FILE_MODIFICATION_TIME:
                case CHECK_FILE_MD5:
                case CHECK_FILE_PERMISSIONS:

                    // the following values are not exposed directly to the user, but are valid ones
                case FindRules.SEARCH_FILENAME_AS_REGEX:
                case FindRules.SKIP_FILE_PATH:
                    break;

                default:
                    throw new FileSystemSnapshotException("Invalid FIND RULE: " + rule
                                                          + ". Please use one of the public "
                                                          + this.getClass().getSimpleName() + ".SKIP_* or"
                                                          + this.getClass().getSimpleName()
                                                          + ".CHECK_* constants");
            }
        }

        // make sure max 1 instance of same rule is available
        Set<Integer> newFindRules = new HashSet<Integer>();
        for (int rule : findRules) {
            newFindRules.add(rule);
        }
        findRules = new int[newFindRules.size()];
        int i = 0;
        for (int rule : newFindRules) {
            findRules[i++] = rule;
        }

        return findRules;
    }

    @Override
    public void takeSnapshot() {

        this.snapshotTimestamp = System.currentTimeMillis();

        log.debug("Start taking file system snapshot, snapshot name is " + name);

        for (String dirAlias : dirSnapshots.keySet()) {
            dirSnapshots.get(dirAlias).takeSnapshot(configuration);
        }

        log.debug("End taking file system snapshot, snapshot name is " + name);
    }

    public void compare( LocalFileSystemSnapshot that ) {

        if (that == null) {
            throw new FileSystemSnapshotException("Snapshot to compare is null");
        }
        if (this.name.equals(that.name)) {
            throw new FileSystemSnapshotException("You are trying to compare snapshots with same name: "
                                                  + this.name);
        }
        if (this.snapshotTimestamp == -1) {
            throw new FileSystemSnapshotException("You are trying to compare snapshots but [" + this.name
                                                  + "] snapshot is still not created");
        }
        if (that.snapshotTimestamp == -1) {
            throw new FileSystemSnapshotException("You are trying to compare snapshots but [" + that.name
                                                  + "] snapshot is still not created");
        }

        log.debug("Comparing snapshots [" + this.name + "] taken on "
                  + SnapshotUtils.dateToString(this.snapshotTimestamp) + " and [" + that.name
                  + "] taken on " + SnapshotUtils.dateToString(that.snapshotTimestamp));

        this.equality = new FileSystemEqualityState(this.name, that.name);
        // carry the remote agents to all that might need it
        this.equality.setFirstAtsAgent(this.remoteAgent);
        this.equality.setSecondAtsAgent(that.remoteAgent);

        SnapshotUtils.checkDirSnapshotsTopLevel(this.name, this.dirSnapshots, that.name, that.dirSnapshots,
                                                equality);

        if (equality.getDifferences().size() > 0) {
            // there are some unexpected differences
            throw new FileSystemSnapshotException(equality);
        } else {
            log.debug("Successful verification");
        }
    }

    @Override
    public void loadFromFile( String sourceFile ) throws FileSystemSnapshotException {

        log.info("Load snapshot from file " + sourceFile + " - START");

        // first clean up the current instance, in case some snapshot was taken before
        this.dirSnapshots.clear();

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(sourceFile));
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            throw new FileSystemSnapshotException("Error reading backup file " + sourceFile, e);
        }

        Element fileSystemNode = doc.getDocumentElement();
        if (!NODE_FILE_SYSTEM.equals(fileSystemNode.getNodeName())) {
            throw new FileSystemSnapshotException("Bad backup file. Root node name is expeced to be '"
                                                  + NODE_FILE_SYSTEM + "', but it is '"
                                                  + fileSystemNode.getNodeName() + "'");
        }

        // the file system snapshot
        log.info("Loading snapshot with name [" + this.name + "]");
        this.snapshotTimestamp = SnapshotUtils.stringToDate(fileSystemNode.getAttribute("time"));

        // the root directories
        List<Element> dirNodes = SnapshotUtils.getChildrenByTagName(fileSystemNode, NODE_DIRECTORY);
        for (Element dirNode : dirNodes) {
            String dirAlias = dirNode.getAttributes().getNamedItem("alias").getNodeValue();

            this.dirSnapshots.put(dirAlias,
                                  DirectorySnapshot.fromFile(dirNode,
                                                             new FileSystemEqualityState(this.name, null)));
        }

        // Copy all rules (from sub-directories) to the top level directory snapshot
        // Because on takeSnapshot() the root folder must contain all the Rules, and its current
        // sub-dir snapshots are deleted, and then added according to those Rules
        for (DirectorySnapshot topLevelDirSnapshot : this.dirSnapshots.values()) {
            for (DirectorySnapshot subDirSnapshot : topLevelDirSnapshot.getDirSnapshots().values()) {
                for (String fileName : subDirSnapshot.getFileRules().keySet()) {
                    String fileAbsPath = fileName;
                    if (!fileAbsPath.startsWith(subDirSnapshot.getPath())) {
                        fileAbsPath = subDirSnapshot.getPath() + fileName;
                    }
                    topLevelDirSnapshot.addFindRules(fileAbsPath,
                                                     subDirSnapshot.getFileRules()
                                                                   .get(fileName)
                                                                   .getRules());
                }
                for (Map<String, Boolean> mapEntry : subDirSnapshot.getSkippedSubDirectories()) {
                    String skippedSubDirPath = mapEntry.keySet().iterator().next();
                    boolean isLastTokenRegex = mapEntry.get(skippedSubDirPath);
                    topLevelDirSnapshot.skipSubDirectory(skippedSubDirPath, isLastTokenRegex);
                }
            }
        }

        log.info("Load snapshot from file " + sourceFile + " - END");
    }

    @Override
    public void toFile( String backupFile ) {

        log.info("SAVE TO FILE " + backupFile + " - START");

        // create the directory if does not exist
        File dirPath = new File(IoUtils.getFilePath(backupFile));
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }

        Document dom;
        try {
            dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new FileSystemSnapshotException("Error creating DOM parser for " + backupFile, e);
        }

        // TODO - add DTD or schema for manual creation and easy validation
        Element fileSystemNode = dom.createElement(NODE_FILE_SYSTEM);
        fileSystemNode.setAttribute("name", this.name);
        fileSystemNode.setAttribute("time", SnapshotUtils.dateToString(this.snapshotTimestamp));
        dom.appendChild(fileSystemNode);

        for (String dirSnapshotName : this.dirSnapshots.keySet()) {

            Element dirSnapshotNode = dom.createElement(NODE_DIRECTORY);
            fileSystemNode.appendChild(dirSnapshotNode);

            dirSnapshotNode.setAttribute("alias", dirSnapshotName);
            this.dirSnapshots.get(dirSnapshotName).toFile(dom, dirSnapshotNode);
        }

        // save the XML file
        OutputStream fos = null;
        try {
            OutputFormat format = new OutputFormat(dom);
            format.setIndenting(true);
            format.setIndent(4);
            format.setLineWidth(1000);

            fos = new FileOutputStream(new File(backupFile));
            XMLSerializer serializer = new XMLSerializer(fos, format);
            serializer.serialize(dom);
        } catch (Exception e) {
            throw new FileSystemSnapshotException("Error saving " + backupFile, e);
        } finally {
            IoUtils.closeStream(fos, "Error closing IO stream to file used for file system snapshot backup "
                                     + backupFile);
        }

        log.info("SAVE TO FILE " + backupFile + " - END");
    }

    @Override
    public String getDescription() {

        StringBuilder sb = new StringBuilder();
        sb.append("snapshot [" + this.name + "] taken at "
                  + SnapshotUtils.dateToString(this.snapshotTimestamp) + "\n");

        for (DirectorySnapshot d : dirSnapshots.values()) {
            sb.append(d.toString());
        }
        return sb.toString();
    }
}
