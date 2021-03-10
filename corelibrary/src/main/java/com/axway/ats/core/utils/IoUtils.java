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
package com.axway.ats.core.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.systemproperties.AtsSystemProperties;

/**
 * Utility class for working with files, folders, input-output operations.
 *
 * Note: the code is static and not synchronized! 
 */
public class IoUtils {

    private static final Logger log = LogManager.getLogger(IoUtils.class);

    public static final String FORWARD_SLASH = "/";

    private static final int INTERNAL_BUFFER_SIZE = 4 * 1024;                       // 4KB

    /**
     * Return the name of a file by truncating the file path from the full file name
     *
     * @param source the source file
     * @return the file name or null if srcFile is null or empty string
     */
    public static String getFileName( String source ) {

        if (StringUtils.isNullOrEmpty(source)) {
            // this is not a valid file
            return source;
        } else {
            source = normalizeUnixFile(source);
            int index = source.lastIndexOf('/');
            if (index < 0) {
                // this is just a file name, not a full file name
                return source;
            } else {
                return source.substring(index + 1);
            }
        }
    }

    /**
     * Return the file path by truncating the file name from the full file name
     *
     * @param source the source file
     * @return <blockquote>the file path<br>
     *         <i>null</i> if provided path is null or empty string or does not contain a directory
     */
    public static String getFilePath( String source ) {

        if (StringUtils.isNullOrEmpty(source)) {
            // this is not a valid file
            return null;
        } else {
            source = normalizeUnixFile(source);
            int index = source.lastIndexOf('/');
            if (index < 0) {
                // this is just a file name, not a full file name
                return null;
            } else {
                return source.substring(0, index + 1);
            }
        }
    }

    /**
     * Replaces any '\\' or '/' characters with the ones for this system
     *
     * @param source the file path
     * @return the properly formatted file path
     */
    public static String normalizeFilePath( String source ) {

        return normalizeFilePath(source, OperatingSystemType.getCurrentOsType());
    }

    /**
     * Replaces any '\\' or '/' characters with the ones for the target system
     *
     * @param source the file path
     * @param osType the system to format for
     * @return the properly formatted file path
     */
    public static String normalizeFilePath( String source, OperatingSystemType osType ) {

        if (source == null) {
            return null;
        }

        String fileSeparator = osType.isWindows()
                               ? "\\"
                               : "/";
        String opositeFileSeparator = fileSeparator.equals("/")
                                      ? "\\"
                                      : FORWARD_SLASH;

        return source.replace(opositeFileSeparator, fileSeparator);
    }

    /**
     * 1. Replaces any '\\' or '/' characters with the ones for this system
     * <p>
     * 2. Appends system specific file separator character at the end if not present
     *
     * @param source the directory path
     * @return the properly formatted directory path
     */
    public static String normalizeDirPath( String source ) {

        return normalizeDirPath(source, OperatingSystemType.getCurrentOsType());
    }

    /**
     * 1. Replaces any '\\' or '/' characters with the ones for the target system
     * <p>
     * 2. Appends system specific file separator character at the end if not present
     *
     * @param source the directory path
     * @param osType the system to format for
     * @return the properly formatted directory path
     */
    public static String normalizeDirPath( String source, OperatingSystemType osType ) {

        String fileSeparator = osType.isWindows()
                               ? "\\"
                               : "/";

        source = normalizeFilePath(source, osType);
        if (source != null && !source.endsWith(fileSeparator)) {
            source = source + fileSeparator;
        }
        return source;
    }

    /**
     * 1. Replaces any '\\' characters with '/' <br>
     * 2. Appends '/' at the end if not present
     *
     * @param source the source directory
     * @return the properly formatted directory
     */
    public static String normalizeUnixDir( String source ) {

        if (StringUtils.isNullOrEmpty(source)) {
            // this is not a valid directory - do not touch it
            return source;
        } else {
            source = source.replace('\\', '/');
            if (source.endsWith(FORWARD_SLASH)) {
                return source;
            } else {
                return source + FORWARD_SLASH;
            }
        }
    }

    /**
     * 1. Replaces any '\\' characters with '/' <br>
     * 2. Appends '/' at the end if not present <br>
     * 3. Adds '"' character at the start and at the end of the string
     *
     * @param source the source directory
     * @return the properly formatted directory
     */
    public static String normalizeDoubleQuotedUnixDir( String source ) {

        if (StringUtils.isNullOrEmpty(source)) {
            // this is not a valid directory - do not touch it
            return source;
        } else {
            source = source.replace('\\', '/');
            if (source.endsWith(FORWARD_SLASH)) {
                return String.format("\"%1$s\"", source);
            } else {
                return String.format("\"%1$s\"", source + FORWARD_SLASH);
            }
        }
    }

    /**
     * Replaces any '\\' characters with '/'
     *
     * @param source the source file
     * @return the properly formatted file
     */
    public static String normalizeUnixFile( String source ) {

        if (StringUtils.isNullOrEmpty(source)) {
            // this is not a valid file - do not touch it
            return source;
        } else {
            return source.replace('\\', '/');
        }
    }

    /**
     * 1. Replaces any '\\' characters with '/' <br>
     * 2. Adds '"' character at the start and at the end of the string
     *
     * @param source the source file
     * @return the properly formatted file
     */
    public static String normalizeDoubleQuotedUnixFile( String source ) {

        if (StringUtils.isNullOrEmpty(source)) {
            // this is not a valid file - do not touch it
            return source;
        } else {
            return String.format("\"%1$s\"", source.replace('\\', '/'));
        }
    }

    /**
     * Utility to silently close a {@link Closeable} item (in/out stream, writer, etc.) if not null.
     * Does not throw IO exception that might arise. It is just logged.
     * @param closeable stream to close
     * @return <code>false</code> if close operation had been issued but not completed successfully
     */
    public static boolean closeStream(
            Closeable closeable ) {

        return closeStream(closeable, null);
    }

    /**
     * Utility to silently close a {@link Closeable} item (in/out stream, writer, etc.) if not null.
     * Does not throw IO exception that might arise. It is just logged.
     * @param closeable stream to close
     * @param message a message to present to the user if the operation fail
     * @return <code>false</code> if close operation had been issued but not completed successfully
     */
    public static boolean closeStream( Closeable closeable, String message ) {

        if (closeable == null) { // nothing to do
            return true;
        } else {
            boolean closed = false;
            try {
                closeable.close();
                closed = true;
            } catch (IOException e) {
                log.warn((message == null ? "Could not close a stream" : message), e);
            }
            return closed;
        }
    }

    /**
     *
     * @param input {@link InputStream}. The input stream is closed after copying.
     * @param output {@link OutputStream}. The output stream is closed after copying.
     * @param closeInputStream Whether to close the input stream after copying is complete.
     * @param closeOutputStream Whether to close the output stream after copying is complete.
     * @param maxSize The maximum number of bytes that will be copied ( -1 for unlimited )
     * @return the number of bytes copied
     * @throws IOException
     */
    public static long copyStream(
            InputStream input,
            OutputStream output,
            boolean closeInputStream,
            boolean closeOutputStream,
            long maxSize ) throws IOException {

        byte[] buffer = new byte[INTERNAL_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        try {
            while ((n = input.read(buffer)) != -1) {
                output.write(buffer, 0, n);
                count += n;
                if (maxSize > 0 && count > maxSize) {
                    throw new IOException("Unable to copy input stream. Max size of " + maxSize + " bytes reached.");
                }
            }
        } finally {
            if (closeInputStream) {
                closeStream(input);
            }
            if (closeOutputStream) {
                closeStream(output);
            }
        }
        return count;
    }

    public static long copyStream( InputStream input,
                                   OutputStream output,
                                   boolean closeInputStream,
                                   boolean closeOutputStream ) throws IOException {

        return copyStream(input, output, closeInputStream, closeOutputStream, -1);
    }

    public static long copyStream(
            InputStream input,
            OutputStream output ) throws IOException {

        return copyStream(input, output, true, true, -1);
    }

    /**
     *
     * @param zipFilePath the zip file path
     * @param outputDirPath output directory
     * @param isTempDirectory whether the directory is temporary or not. Temporary means that
     *  it will be automatically deleted only if the virtual machine terminates normally.
     * @throws IOException
     */
    public static void unzip(
            String zipFilePath,
            String outputDirPath,
            boolean isTempDirectory ) throws IOException {

        File outputDir = new File(outputDirPath);
        outputDir.mkdirs();
        if (isTempDirectory) {
            outputDir.deleteOnExit();
        }

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {

                ZipEntry zipEntry = entries.nextElement();
                File entryDestination = new File(outputDirPath, zipEntry.getName());
                if (isTempDirectory) {
                    entryDestination.deleteOnExit();
                }

                if (zipEntry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();

                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = zipFile.getInputStream(zipEntry);
                        out = new FileOutputStream(entryDestination);

                        copyStream(in, out);
                    } finally {
                        closeStream(out);
                        closeStream(in);
                    }
                }
            }
        }
    }

    /**
     * Loads a file and returns it as an InputStream.
     * <p><em>Note:</em> The returned stream is full in-memory copy {@link ByteArrayInputStream}
     * so it should be used only for relatively small files.
     * </p>
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static InputStream readFile(
            String filePath ) throws IOException {

        // fix the encoded characters
        filePath = URLDecoder.decode(filePath, "UTF-8");

        File file = new File(filePath);
        InputStream fis = null;

        try {
            fis = new FileInputStream(file);
            byte[] readBytes = new byte[(int) file.length()];
            fis.read(readBytes);
            return new ByteArrayInputStream(readBytes);
        } finally {
            closeStream(fis);
        }
    }

    /**
     * Loads a file from a jar and returns it as an {@link InputStream}
     * <p><em>Note:</em> The returned stream is full in-memory copy and should be used with small files only.</p>
     * <p><strong>EXAMPLE:</strong><br>
     * If you want to load "file:/C:/folder/myJarFile.jar!/com/test/myFile.xml"<br>
     * then jarContainingTheFileToRead must be "file:/C:/folder/myJarFile.jar!/com/test/myFile.xml"
     * or "file:/C:/folder/myJarFile.jar"<br>
     * and fileToLoad must be "/com/test/myFile.xml" or "com/test/myFile.xml"
     * </p>
     *
     * @param jarContainingTheFileToRead full path to JAR optionally including the path to the file in JAR
     * @param fileToLoad full path to the file inside JAR
     * @return InputStream for the in-JAR file
     * @throws IOException
     */
    public static InputStream readFileFromJar(
            String jarContainingTheFileToRead,
            String fileToLoad ) throws IOException {

        // fix the encoded characters
        jarContainingTheFileToRead = URLDecoder.decode(jarContainingTheFileToRead, "UTF-8");
        if (jarContainingTheFileToRead.startsWith("file:")) {
            // cut the file prefix
            jarContainingTheFileToRead = jarContainingTheFileToRead.substring("file:".length());
        }
        // we are opening a jar file, cut any info after the jar filename
        if (jarContainingTheFileToRead.indexOf('!') != -1) {
            jarContainingTheFileToRead = jarContainingTheFileToRead.substring(0,
                                                                              jarContainingTheFileToRead.indexOf('!'));
        }

        // remove the root prefix "/" if there is such
        if (fileToLoad.startsWith("/")) {
            fileToLoad = fileToLoad.substring(1);
        }

        // normalize the separator of internal java packages - this is not dependent to the local OS
        fileToLoad = fileToLoad.replace("\\", "/");

        log.debug("Searching for '" + fileToLoad + "' into '" + jarContainingTheFileToRead + "'");

        JarFile jarFile = new JarFile(jarContainingTheFileToRead);
        ZipEntry jarEntry = jarFile.getEntry(fileToLoad);
        if (jarEntry != null) {
            log.debug("Reading file '" + jarEntry.getName() + "'");
            try {
                // Note: jarEntry.getSize() is not used in order to load the file at once as it does not guarantee that
                // will return always the extracted(normal) byte size
                byte[] byteArr = loadInputStreamToMemory(jarFile.getInputStream(jarEntry));
                return new ByteArrayInputStream(byteArr);
            } finally {
                try {
                    jarFile.close(); // closes also all streams opened from this JAR file
                } catch (IOException e) {
                    log.debug("Problem while trying to close open streams from ZIP file '"
                              + jarContainingTheFileToRead + "'", e);
                }
            }
        } else {
            closeStream(jarFile,
                        "Error ocurred while trying to close handle to file " + jarContainingTheFileToRead);
            throw new FileNotFoundException("'" + fileToLoad + "' not found in '"
                                            + jarContainingTheFileToRead + "'");
        }
    }

    /**
     * Load the whole content from the input stream into memory in order to release quickly resources.
     * Can be used to load small files.
     * @param inputStream
     * @return content as byte array
     * @throws IOException
     */
    private static byte[] loadInputStreamToMemory(
            InputStream inputStream ) throws IOException {

        if (inputStream == null) {
            throw new IllegalArgumentException("Null parameter passed");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[INTERNAL_BUFFER_SIZE];
        int bytesRead = inputStream.read(buff);
        while (bytesRead > -1) {
            baos.write(buff, 0, bytesRead);
            bytesRead = inputStream.read(buff);
        }
        return baos.toByteArray();
    }

    /**
     * Convert the {@link InputStream} to {@link String} and close the passed stream
     *
     * @param inputStream the input stream
     * @return a string representation of the input data
     * @throws IOException
     */
    public static String streamToString(
            InputStream inputStream ) throws IOException {

        try {
            return new String(loadInputStreamToMemory(inputStream));
        } finally {
            IoUtils.closeStream(inputStream);
        }
    }

    /**
     * Get file chunk
     *
     * @param fileName the file name
     * @param fromLine from line number
     * @param toLine to line number
     * @return file chunk between fromLine and toLine
     */
    public static String getFileChunk(
            String fileName,
            int fromLine,
            int toLine ) {

        return getFileChunk(fileName, fromLine, toLine, 10);
    }

    /**
     * Get file chunk
     *
     * @param fileName the file name
     * @param fromLine from line number
     * @param toLine to line number
     * @param averageLineLength average line length. Useful for performance improvement
     * @return file chunk between fromLine and toLine
     */
    public static String getFileChunk(
            String fileName,
            int fromLine,
            int toLine,
            int averageLineLength ) {

        StringBuilder sb = new StringBuilder((toLine - fromLine) * averageLineLength);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));

            int lineNumber = 1;
            String line;
            while ((line = br.readLine()) != null) {

                if (lineNumber >= fromLine) {

                    sb.append(line + AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
                }
                if (lineNumber >= toLine) {
                    break;
                }
                lineNumber++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to get a chunk of data from file: " + fileName, e);
        } finally {
            IoUtils.closeStream(br);
        }
        return sb.toString();
    }

    public static String readLineWithEOL(
            RandomAccessFile raf ) throws IOException {

        StringBuilder line = new StringBuilder();
        int c = -1;
        boolean eol = false;
        while (!eol) {
            switch (c = raf.read()) {
                case -1:
                    eol = true;
                    break;
                case '\n':
                    eol = true;
                    line.append('\n');
                    break;
                case '\r':
                    eol = true;
                    line.append('\r');
                    long cur = raf.getFilePointer();
                    if ((raf.read()) != '\n') {
                        raf.seek(cur);
                    } else {
                        line.append('\n');
                    }
                    break;
                default:
                    line.append((char) c);
                    break;
            }
        }
        if (c == -1 && line.length() == 0) {
            return null;
        }
        return line.toString();
    }

}
