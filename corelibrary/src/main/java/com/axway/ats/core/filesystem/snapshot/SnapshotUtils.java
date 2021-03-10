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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.common.filesystem.snapshot.equality.FileSystemEqualityState;
import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipIniMatcher;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipPropertyMatcher;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipTextLineMatcher;
import com.axway.ats.core.filesystem.snapshot.matchers.SkipXmlNodeMatcher;
import com.axway.ats.core.filesystem.snapshot.types.FileSnapshot;
import com.axway.ats.core.filesystem.snapshot.types.IniFileSnapshot;
import com.axway.ats.core.filesystem.snapshot.types.PropertiesFileSnapshot;
import com.axway.ats.core.filesystem.snapshot.types.TextFileSnapshot;
import com.axway.ats.core.filesystem.snapshot.types.XmlFileSnapshot;
import com.axway.ats.core.utils.IoUtils;

public class SnapshotUtils {

    private static final Logger           log         = LogManager.getLogger(SnapshotUtils.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

    public static String dateToString( long timeInMillis ) {

        return DATE_FORMAT.format(new Date(timeInMillis));
    }

    public static long stringToDate( String timeString ) {

        try {
            return DATE_FORMAT.parse(timeString).getTime();
        } catch (ParseException e) {
            throw new FileSystemSnapshotException("Cannot parse date '" + timeString + "'");
        }
    }

    public static String getDirPathLastToken( String path ) {

        String[] tokens = path.split(IoUtils.FORWARD_SLASH);
        return tokens[tokens.length - 1];
    }

    static void checkDirSnapshotsTopLevel( String thisSnapshotName,
                                           Map<String, DirectorySnapshot> thisDirSnapshots,
                                           String thatSnapshotName,
                                           Map<String, DirectorySnapshot> thatDirSnapshots,
                                           FileSystemEqualityState equality ) {

        // we will make another HashMap, because in checkDirSnapshotsInternal() we are removing
        // map keys using map.keySet().remove( key ), which actually removes an entry from the Map,
        // which we don't want
        thisDirSnapshots = new HashMap<String, DirectorySnapshot>(thisDirSnapshots);
        thatDirSnapshots = new HashMap<String, DirectorySnapshot>(thatDirSnapshots);

        // all directories from first snapshot are searched in the second
        // the found directories are removed from the internal lists
        checkDirSnapshotsInternal(thisSnapshotName, thisDirSnapshots, thatSnapshotName, thatDirSnapshots,
                                  false, equality, false);

        // now do the same, but replace the snapshots
        checkDirSnapshotsInternal(thatSnapshotName, thatDirSnapshots, thisSnapshotName, thisDirSnapshots,
                                  false, equality, true);
    }

    static void checkDirSnapshotsDeepLevel( String thisSnapshotName,
                                            Map<String, DirectorySnapshot> thisDirSnapshots,
                                            String thatSnapshotName,
                                            Map<String, DirectorySnapshot> thatDirSnapshots,
                                            FileSystemEqualityState equality ) {

        // we will make another HashMap, because in checkDirSnapshotsInternal() we are removing
        // map keys using map.keySet().remove( key ), which actually removes an entry from the Map,
        // which we don't want
        thisDirSnapshots = new HashMap<String, DirectorySnapshot>(thisDirSnapshots);
        thatDirSnapshots = new HashMap<String, DirectorySnapshot>(thatDirSnapshots);

        // all directories from first snapshot are searched in the second
        // the found directories are removed from the internal lists
        checkDirSnapshotsInternal(thisSnapshotName, thisDirSnapshots, thatSnapshotName, thatDirSnapshots,
                                  true, equality, false);

        // now do the same, but replace the snapshots
        checkDirSnapshotsInternal(thatSnapshotName, thatDirSnapshots, thisSnapshotName, thisDirSnapshots,
                                  true, equality, true);
    }

    private static void checkDirSnapshotsInternal( String firstSnapshotName,
                                                   Map<String, DirectorySnapshot> firstDirSnapshots,
                                                   String secondSnapshotName,
                                                   Map<String, DirectorySnapshot> secondDirSnapshots,
                                                   boolean checkDirName, FileSystemEqualityState equality,
                                                   boolean areSnapshotsReversed ) {

        Set<String> firstDirAliases = firstDirSnapshots.keySet();
        Set<String> secondDirAliases = secondDirSnapshots.keySet();
        Set<String> processedDirAliases = new HashSet<String>();

        // search for FIRST dirs in SECOND dirs
        for (String dirAlias : firstDirAliases) {
            log.debug("Check directories with alias \"" + dirAlias + "\"");

            DirectorySnapshot firstDir = firstDirSnapshots.get(dirAlias);
            DirectorySnapshot secondDir = secondDirSnapshots.get(dirAlias);

            if (secondDir == null) {
                // Second snapshot does not have a directory to match
                if (!areSnapshotsReversed) {
                    equality.addDifference(new FileTrace(firstSnapshotName, firstDir.getPath(),
                                                         secondSnapshotName, null, "dir", false));
                } else {
                    equality.addDifference(new FileTrace(secondSnapshotName, null, firstSnapshotName,
                                                         firstDir.getPath(), "dir", false));
                }
            } else {
                log.debug("Compare [" + firstSnapshotName + "] " + firstDir.getPath() + " and ["
                          + secondSnapshotName + "] " + secondDir.getPath());

                // Before comparing directories, we need to make sure both instances have all matchers.
                // This way the user does not need to add same matchers twice for each snapshot instance.
                mergeSkipContentMatchers(firstDir, secondDir);

                if (!areSnapshotsReversed) {
                    firstDir.compare(firstSnapshotName, secondSnapshotName, secondDir, checkDirName,
                                     equality);
                } else {
                    firstDir.compare(secondSnapshotName, firstSnapshotName, secondDir, checkDirName,
                                     equality);
                }
            }

            processedDirAliases.add(dirAlias);
        }

        // remove the processed dirs from the internal lists, we do not want to deal with them anymore
        for (String checkedDirAlias : processedDirAliases) {
            firstDirAliases.remove(checkedDirAlias);
            secondDirAliases.remove(checkedDirAlias);
        }
    }

    private static void mergeSkipContentMatchers( DirectorySnapshot firstDir, DirectorySnapshot secondDir ) {

        // merger all FIRST matchers in SECOND
        for (Map.Entry<String, List<SkipPropertyMatcher>> entry : firstDir.getPropertyMatchersPerFile()
                                                                          .entrySet()) {
            for (SkipPropertyMatcher matcher : entry.getValue()) {
                secondDir.addSkipPropertyMatcher(matcher.getDirectoryAlias(), entry.getKey(), matcher);
            }
        }
        for (Map.Entry<String, List<SkipXmlNodeMatcher>> entry : firstDir.getXmlNodeMatchersPerFile()
                                                                         .entrySet()) {
            for (SkipXmlNodeMatcher matcher : entry.getValue()) {
                secondDir.addSkipXmlNodeMatcher(matcher.getDirectoryAlias(), entry.getKey(), matcher);
            }
        }
        for (Map.Entry<String, List<SkipIniMatcher>> entry : firstDir.getIniMatchersPerFile().entrySet()) {
            for (SkipIniMatcher matcher : entry.getValue()) {
                secondDir.addSkipIniMatcher(matcher.getDirectoryAlias(), entry.getKey(), matcher);
            }
        }
        for (Map.Entry<String, List<SkipTextLineMatcher>> entry : firstDir.getTextLineMatchersPerFile()
                                                                          .entrySet()) {
            for (SkipTextLineMatcher matcher : entry.getValue()) {
                secondDir.addSkipTextLineMatcher(matcher.getDirectoryAlias(), entry.getKey(), matcher);
            }
        }

        // merger all SECOND matchers in FIRST
        for (Map.Entry<String, List<SkipPropertyMatcher>> entry : secondDir.getPropertyMatchersPerFile()
                                                                           .entrySet()) {
            for (SkipPropertyMatcher matcher : entry.getValue()) {
                firstDir.addSkipPropertyMatcher(matcher.getDirectoryAlias(), entry.getKey(), matcher);
            }
        }
        for (Map.Entry<String, List<SkipXmlNodeMatcher>> entry : secondDir.getXmlNodeMatchersPerFile()
                                                                          .entrySet()) {
            for (SkipXmlNodeMatcher matcher : entry.getValue()) {
                firstDir.addSkipXmlNodeMatcher(matcher.getDirectoryAlias(), entry.getKey(), matcher);
            }
        }
        for (Map.Entry<String, List<SkipIniMatcher>> entry : secondDir.getIniMatchersPerFile().entrySet()) {
            for (SkipIniMatcher matcher : entry.getValue()) {
                firstDir.addSkipIniMatcher(matcher.getDirectoryAlias(), entry.getKey(), matcher);
            }
        }
        for (Map.Entry<String, List<SkipTextLineMatcher>> entry : secondDir.getTextLineMatchersPerFile()
                                                                           .entrySet()) {
            for (SkipTextLineMatcher matcher : entry.getValue()) {
                firstDir.addSkipTextLineMatcher(matcher.getDirectoryAlias(), entry.getKey(), matcher);
            }
        }
    }

    static void checkFileSnapshots( String firstSnapshotName,
                                    final Map<String, FileSnapshot> firstFileSnapshots,
                                    String secondSnapshotName,
                                    final Map<String, FileSnapshot> secondFileSnapshots,
                                    FileSystemEqualityState equality ) {

        // compare each FIRST file with its counterpart in SECOND files
        for (String fileAlias : firstFileSnapshots.keySet()) {
            log.debug("Check file \"" + fileAlias + "\"");

            FileSnapshot firstFile = firstFileSnapshots.get(fileAlias);
            FileSnapshot secondFile = secondFileSnapshots.get(fileAlias);

            if (secondFile == null) {
                // SECOND file is not found
                equality.addDifference(new FileTrace(firstSnapshotName, firstFile.getPath(),
                                                     secondSnapshotName, null, firstFile.getFileType(),
                                                     true));
            } else {
                // BOTH files are found, compare them
                FileTrace fileTrace = new FileTrace(firstSnapshotName, firstFile.getPath(),
                                                    secondSnapshotName, secondFile.getPath(),
                                                    firstFile.getFileType(), true);

                // When comparing file content, we must make sure both instances are of same class.
                // If user has specified matchers for one of the file snapshots only, we have to extend the 
                // other snapshot to the wider class containing content check matchers
                firstFile = extendSnapshotInstaceIfNeeded(firstFile, secondFile);
                secondFile = extendSnapshotInstaceIfNeeded(secondFile, firstFile);

                firstFile.compare(secondFile, equality, fileTrace);
            }
        }

        // now all compares here are done, but we want to check if SECOND has files that are not present in FIRST
        // if we find such file - it is a problem
        for (String fileAlias : secondFileSnapshots.keySet()) {
            FileSnapshot firstFile = firstFileSnapshots.get(fileAlias);
            FileSnapshot secondFile = secondFileSnapshots.get(fileAlias);

            if (firstFile == null) {
                // FIRST file is not found
                equality.addDifference(new FileTrace(firstSnapshotName, null, secondSnapshotName,
                                                     secondFile.getPath(), secondFile.getFileType(),
                                                     true));
            }
        }
    }

    private static FileSnapshot extendSnapshotInstaceIfNeeded( FileSnapshot snapshotToExtend,
                                                               FileSnapshot snapshotToExtendTo ) {

        if (snapshotToExtendTo instanceof PropertiesFileSnapshot
            && snapshotToExtend.getClass() == FileSnapshot.class) {
            snapshotToExtend = ((PropertiesFileSnapshot) snapshotToExtendTo).getNewInstance(snapshotToExtend);
        } else if (snapshotToExtendTo instanceof XmlFileSnapshot
                   && snapshotToExtend.getClass() == FileSnapshot.class) {
            snapshotToExtend = ((XmlFileSnapshot) snapshotToExtendTo).getNewInstance(snapshotToExtend);
        } else if (snapshotToExtendTo instanceof IniFileSnapshot
                   && snapshotToExtend.getClass() == FileSnapshot.class) {
            snapshotToExtend = ((IniFileSnapshot) snapshotToExtendTo).getNewInstance(snapshotToExtend);
        } else if (snapshotToExtendTo instanceof TextFileSnapshot
                   && snapshotToExtend.getClass() == FileSnapshot.class) {
            snapshotToExtend = ((TextFileSnapshot) snapshotToExtendTo).getNewInstance(snapshotToExtend);
        }

        return snapshotToExtend;
    }

    static boolean isFileFromThisDirectory( String dirPath, String filePath ) {

        if (filePath.startsWith(dirPath)) {
            // the file is from this dir or a sub-dir

            String filePathRest = filePath.substring(dirPath.length());
            return !filePathRest.contains(IoUtils.FORWARD_SLASH);
        }

        return false;
    }

    static List<Element> getChildrenByTagName( Element parent, String name ) {

        List<Element> nodeList = new ArrayList<Element>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                nodeList.add((Element) child);
            }
        }

        return nodeList;
    }
}
