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
package com.axway.ats.uiengine.utilities.mobile;

import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.filesystem.model.IFileSystemOperations;
import com.axway.ats.core.process.LocalProcessExecutor;
import com.axway.ats.core.process.model.IProcessExecutor;
import com.axway.ats.core.system.LocalSystemOperations;
import com.axway.ats.core.system.model.ISystemOperations;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.MobileDriver;
import com.axway.ats.uiengine.exceptions.MobileOperationException;
import com.axway.ats.uiengine.exceptions.NotSupportedOperationException;

public class MobileDeviceUtils {

    private static final Pattern          LS_ENTRY_PATTERN = Pattern.compile("[^\\s]{9,15}\\s+\\d+\\s+\\d+\\s+(\\d*)\\s+([\\d\\-]+\\s+[\\d:]+)\\s+(.*)");
    private static final SimpleDateFormat LS_DATE_FORMAT   = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private MobileDriver                  mobileDriver;

    private String                        iOSUserHomeDir;
    private String                        iOSDeviceId;
    private String                        iOSApplicationId;
    private String                        iOSApplicationDataPath;

    public MobileDeviceUtils( MobileDriver uiDriver ) {

        this.mobileDriver = uiDriver;
    }

    /**
    *
    * @param path a path on the mobile device. It can be a directory or file (if you want to check
    * for specific file existence)
    * <br/>
    * <b>Note for iOS:</b> You have to specify a relative path to the application data folder<br/>
    * For example: <i>"Documents/MyAppFiles"</i><br/>
    * and we'll internally search for files in:
    *  <i>"/Users/&lt;username&gt;/Library/Developer/CoreSimulator/Devices/&lt;device_id&gt;/data/Containers/Data/Application/&lt;app_id&gt;/Documents/MyAppFiles/"</i><br/>
    * which is the iOS Simulator application data folder path
    *
    * @return {@link FileInfo} array with all the files and folders from the target path
    */
    public FileInfo[] listFiles( String path ) {

        List<FileInfo> files = new ArrayList<FileInfo>();

        if (this.mobileDriver.isAndroidAgent()) {
            String[] commandArguments = new String[]{ "shell", "ls", "-lan", path };
            try {
                IProcessExecutor processExecutor = executeAdbCommand(commandArguments, true);
                String result = processExecutor.getStandardOutput();
                if (!StringUtils.isNullOrEmpty(result)) {
                    String[] lines = result.split("[\r\n]+");
                    for (String line : lines) {
                        Matcher m = LS_ENTRY_PATTERN.matcher(line);
                        if (m.matches()) {
                            /*
                             * group 1 -> file size
                             * group 2 -> modification date and time
                             * group 3 -> file name
                             */
                            String fileName = m.group(3);
                            String absolutePath = new String(IoUtils.normalizeUnixDir(path)
                                                             + fileName).replace("//", "/");
                            if (line.startsWith("l") && line.contains("->")) { // a link
                                absolutePath = fileName.substring(fileName.lastIndexOf("->") + 2).trim(); // after '->'
                                fileName = fileName.substring(0, fileName.indexOf("->")).trim(); // before '->'
                            }
                            String fileSize = m.group(1);
                            FileInfo file = new FileInfo(fileName, absolutePath,
                                                         line.startsWith("d") || fileSize.isEmpty()
                            /* OR because second option is for symbolic links */ );
                            if (!fileSize.isEmpty()) {
                                file.setSize(Long.parseLong(fileSize));
                            }
                            file.setModificationDate(LS_DATE_FORMAT.parse(m.group(2)));
                            files.add(file);
                        }
                    }
                }
            } catch (Exception e) {
                throw new MobileOperationException("Unable to list files for path '" + path + "'", e);
            }

        } else { // iOS case supposed

            IFileSystemOperations fileSystemOperations = getFileSystemOperatoinsImpl();
            String[] filePaths = fileSystemOperations.findFiles(getiOSApplicationDataPath() + path, ".*",
                                                                true, true, false);
            for (String filePath : filePaths) {

                boolean isDirectory = filePath.endsWith("/");
                if (isDirectory) {
                    filePath = filePath.substring(0, filePath.length() - 1);
                }
                String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                FileInfo file = new FileInfo(fileName, filePath, isDirectory);
                file.setSize(fileSystemOperations.getFileSize(filePath));
                files.add(file);
            }
        }

        return files.toArray(new FileInfo[files.size()]);
    }

    /**
    *
    * @param filePath file absolute path
    * <br/>
    * <b>Note for iOS:</b> You can specify relative path too, by skipping the root slash '/' at the beginning
    *   and pass the path relative to the application data folder<br/>
    * For example: <i>"Documents/MyAppFiles/IMG_0001.PNG"</i><br/>
    * and we'll internally search for:
    *  <i>"/Users/&lt;username&gt;/Library/Developer/CoreSimulator/Devices/&lt;device_id&gt;/data/Containers/Data/Application/&lt;app_id&gt;/Documents/MyAppFiles/IMG_0001.PNG"</i><br/>
    * which is the iOS Simulator application data folder path
    *
    * @return the MD5 sum of the specified file
    */
    public String getMD5Sum( String filePath ) {

        if (this.mobileDriver.isAndroidAgent()) {

            String[] commandArguments = new String[]{ "shell", "md5", filePath };
            try {
                IProcessExecutor processExecutor = executeAdbCommand(commandArguments, true);
                String result = processExecutor.getStandardOutput();
                if (!StringUtils.isNullOrEmpty(result)) {
                    result = result.trim();
                    return result.substring(0, result.indexOf(' '));
                }
            } catch (Exception e) {
                throw new MobileOperationException("Unable to calculate md5 sum of file '" + filePath + "'",
                                                   e);
            }
        } else { // iOS app. supposed

            if (!filePath.startsWith("/")) {
                filePath = getiOSApplicationDataPath() + filePath;
            }
            return getFileSystemOperatoinsImpl().computeMd5Sum(filePath, Md5SumMode.BINARY);
        }
        return null;
    }

    /**
     *
     * @param directoryPath the directory for deletion
     * @param recursively whether to delete the internal directories recursively
     *
     * <b>Note for iOS:</b> You can specify relative directory path too, by skipping the root slash '/' at the beginning
     *   and pass the path relative to the application data folder<br/>
     * For example: <i>"Documents/MyAppFiles"</i><br/>
     * and we'll internally search for:
     *  <i>"/Users/&lt;username&gt;/Library/Developer/CoreSimulator/Devices/&lt;device_id&gt;/data/Containers/Data/Application/&lt;app_id&gt;/Documents/MyAppFiles/"</i><br/>
     * which is the iOS Simulator application data folder path
     */
    public void deleteDirectory( String directoryPath, boolean recursively ) {

        if (this.mobileDriver.isAndroidAgent()) {

            String[] commandArguments = null;
            if (recursively) {
                commandArguments = new String[]{ "shell", "rm", "-rf", directoryPath };
            } else {
                commandArguments = new String[]{ "shell", "rm", "-f", directoryPath };
            }
            try {
                executeAdbCommand(commandArguments, true);
            } catch (Exception e) {
                throw new MobileOperationException("Unable to " + (recursively
                                                                               ? "recursively "
                                                                               : "")
                                                   + "delete directory '" + directoryPath + "'", e);
            }
        } else { // iOS app. supposed

            if (!directoryPath.startsWith("/")) {
                directoryPath = getiOSApplicationDataPath() + directoryPath;
            }
            getFileSystemOperatoinsImpl().deleteDirectory(directoryPath, recursively);
        }
    }

    /**
     *
     * @param filePath the file for deletion
     *
     * <b>Note for iOS:</b> You can specify relative file path too, by skipping the root slash '/' at the beginning
     *   and pass the path relative to the application data folder<br/>
     * For example: <i>"Documents/MyAppFiles/fileToDelete"</i><br/>
     * and we'll internally search for:
     *  <i>"/Users/&lt;username&gt;/Library/Developer/CoreSimulator/Devices/&lt;device_id&gt;/data/Containers/Data/Application/&lt;app_id&gt;/Documents/MyAppFiles/fileToDelete"</i><br/>
     * which is the iOS Simulator application data folder path
     */
    public void deleteFile( String filePath ) {

        if (this.mobileDriver.isAndroidAgent()) {

            String[] commandArguments = new String[]{ "shell", "rm", filePath };
            try {
                executeAdbCommand(commandArguments, true);
            } catch (Exception e) {
                throw new MobileOperationException("Unable to delete file '" + filePath + "'", e);
            }
        } else { // iOS app. supposed

            if (!filePath.startsWith("/")) {
                filePath = getiOSApplicationDataPath() + filePath;
            }
            getFileSystemOperatoinsImpl().deleteFile(filePath);
        }
    }

    /**
    *
    * @param directoryPath the directory for creation
    */
    public void createDirectory( String directoryPath ) {

        if (this.mobileDriver.isAndroidAgent()) {

            String[] commandArguments = new String[]{ "shell", "mkdir", "-p", directoryPath };
            try {
                executeAdbCommand(commandArguments, true);
            } catch (Exception e) {
                throw new MobileOperationException("Unable to create directory '" + directoryPath + "'", e);
            }
        } else {

            throw new NotSupportedOperationException("Currently 'createDirectory' operation for iOS is not implemented");
        }
    }

    /**
     *
     * @param localFilePath local file path
     * @param deviceFilePath file path on the device
     */
    public void copyFileTo( String localFilePath, String deviceFilePath ) {

        if (this.mobileDriver.isAndroidAgent()) {

            String[] commandArguments = new String[]{ "push", localFilePath, deviceFilePath };
            try {
                executeAdbCommand(commandArguments, true);
            } catch (Exception e) {
                throw new MobileOperationException("Unable to copy file '" + localFilePath + "' to '"
                                                   + deviceFilePath + "'", e);
            }
        } else {

            throw new NotSupportedOperationException("Currently 'copyFileTo' operation for iOS is not implemented");
        }
    }

    /**
     *
     * @param deviceFilePath file path on the device
     * @param localFilePath local file path
     */
    public void copyFileFrom( String deviceFilePath, String localFilePath ) {

        if (this.mobileDriver.isAndroidAgent()) {

            String[] commandArguments = new String[]{ "pull", deviceFilePath, localFilePath };
            try {
                executeAdbCommand(commandArguments, true);
            } catch (Exception e) {
                throw new MobileOperationException("Unable to copy file '" + deviceFilePath + "' to '"
                                                   + localFilePath + "'", e);
            }
        } else {

            throw new NotSupportedOperationException("Currently 'copyFileFrom' operation for iOS is not implemented");
        }
    }

    public boolean isAdbOnWindows() {

        return getSystemOperationsImpl().getOperatingSystemType().isWindows();
    }

    public IProcessExecutor executeAdbCommand( String[] commandArguments, boolean verifyExitCode ) {

        IProcessExecutor pe = null;
        try {
            if (getSystemOperationsImpl().getOperatingSystemType().isWindows()) {
                pe = getProcessExecutorImpl(this.mobileDriver.getAdbLocation() + "adb.exe",
                                            commandArguments);
            } else {
                pe = getProcessExecutorImpl(this.mobileDriver.getAdbLocation() + "adb", commandArguments);
            }
            pe.setWorkDirectory(this.mobileDriver.getAdbLocation());
            pe.execute();

            if (verifyExitCode && pe.getExitCode() != 0) {
                throw new MobileOperationException("Adb command failed (STDOUT: '" + pe.getStandardOutput()
                                                   + "', STDERR: '" + pe.getErrorOutput() + "')");
            }
        } catch (Exception e) {
            throw new MobileOperationException("Adb command failed", e);
        }
        return pe;
    }

    private String getiOSApplicationDataPath() {

        if (iOSApplicationDataPath == null && !this.mobileDriver.isAndroidAgent()) {

            // get user home dir
            iOSUserHomeDir = getSystemOperationsImpl().getSystemProperty("user.home");
            if (!iOSUserHomeDir.startsWith("/")) {
                iOSUserHomeDir = "/" + iOSUserHomeDir;
            }
            iOSUserHomeDir = IoUtils.normalizeUnixDir(iOSUserHomeDir);

            // get booted device UUID
            IProcessExecutor pe = getProcessExecutorImpl("/bin/bash",
                                                         new String[]{ "-c",
                                                                       "xcrun simctl list | grep Booted | cut -d \\( -f2 | tr -d \\)" });
            pe.execute();
            iOSDeviceId = pe.getStandardOutput().trim();

            //FIXME: The next variables are retrieved for iOS Simulator,
            // so when we add support for iOS real devices, we have to touch this code

            // get current application ID in the simulator data structure
            // we suppose that the current application ID is the last modified directory in the device Applications folder
            pe = getProcessExecutorImpl("/bin/bash",
                                        new String[]{ "-c",
                                                      "ls -t " + iOSUserHomeDir
                                                            + "Library/Developer/CoreSimulator/Devices/"
                                                            + iOSDeviceId
                                                            + "/data/Containers/Data/Application | head -1" });
            pe.execute();
            iOSApplicationId = pe.getStandardOutput().trim();

            // now we can build the application data path using the device and application retrieved identifiers
            iOSApplicationDataPath = iOSUserHomeDir + "Library/Developer/CoreSimulator/Devices/" + iOSDeviceId
                                     + "/data/Containers/Data/Application/" + iOSApplicationId + "/";
        }
        return iOSApplicationDataPath;
    }

    /**
     *
     * @return {@link ISystemOperations} implementation instance
     */
    private ISystemOperations getSystemOperationsImpl() {

        if (this.mobileDriver.isWorkingRemotely()) {

            String remoteSystemOperationsClassName = "com.axway.ats.action.system.RemoteSystemOperations";
            try {
                Class<?> remoteSOClass = Class.forName(remoteSystemOperationsClassName);
                Constructor<?> constructor = remoteSOClass.getDeclaredConstructors()[0];
                return (ISystemOperations) constructor.newInstance(mobileDriver.getHost());
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate RemoteSystemOperations. Check whether the Action Library component is in the classpath.",
                                           e);
            }

        }
        return new LocalSystemOperations();
    }

    /**
     *
     * @return {@link IFileSystemOperations} implementation instance
     */
    private IFileSystemOperations getFileSystemOperatoinsImpl() {

        if (this.mobileDriver.isWorkingRemotely()) {

            String remoteFileSystemOperationsClassName = "com.axway.ats.action.filesystem.RemoteFileSystemOperations";
            try {
                Class<?> remoteFSOClass = Class.forName(remoteFileSystemOperationsClassName);
                Constructor<?> constructor = remoteFSOClass.getDeclaredConstructors()[0];
                return (IFileSystemOperations) constructor.newInstance(mobileDriver.getHost());
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate RemoteFileSystemOperations. Check whether the Action Library component is in the classpath.",
                                           e);
            }
        }
        return new LocalFileSystemOperations();
    }

    /**
    *
    * @return {@link IProcessExecutor} implementation instance
    */
    private IProcessExecutor getProcessExecutorImpl( String command, String... commandArguments ) {

        if (this.mobileDriver.isWorkingRemotely()) {

            String remoteProcessExecutorClassName = "com.axway.ats.action.processes.RemoteProcessExecutor";
            try {
                Class<?> remotePEOClass = Class.forName(remoteProcessExecutorClassName);
                Constructor<?> constructor = remotePEOClass.getDeclaredConstructors()[0];
                return (IProcessExecutor) constructor.newInstance(mobileDriver.getHost(), command,
                                                                  commandArguments);
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate RemoteProcessExecutor. Check whether the Action Library component is in the classpath.",
                                           e);
            }
        }
        return new LocalProcessExecutor(command, commandArguments);
    }

}
