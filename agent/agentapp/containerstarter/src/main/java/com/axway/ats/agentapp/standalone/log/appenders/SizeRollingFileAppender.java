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
package com.axway.ats.agentapp.standalone.log.appenders;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Layout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.CountingQuietWriter;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

public class SizeRollingFileAppender extends RollingFileAppender {

    private long nextRollover = 0L;

    /**
    Instantiate a SizeRollingFileAppender and open the file designated by
    <code>filename</code>. The opened filename will become the ouput
    destination for this appender.
    
    <p>If the <code>append</code> parameter is true, the file will be
    appended to. Otherwise, the file desginated by
    <code>filename</code> will be truncated before being opened.
    */
    public SizeRollingFileAppender( Layout layout,
                                    String filename,
                                    boolean append ) throws IOException {

        super(layout, filename, append);
    }

    /**
    Implements the usual roll over behaviour.
    
    <p>If <code>MaxBackupIndex</code> is positive, then files
    {<code>File.1</code>, ..., <code>File.MaxBackupIndex -1</code>}
    are renamed to {<code>File.2</code>, ...,
    <code>File.MaxBackupIndex</code>}. Moreover, <code>File</code> is
    renamed <code>File.1</code> and closed. A new <code>File</code> is
    created to receive further log output.
    
    <p>If <code>MaxBackupIndex</code> is equal to zero, then the
    <code>File</code> is truncated with no backup files created.
    
    */
    @Override
    public// synchronization not necessary since doAppend is alreasy synched
    void rollOver() {

        File target;
        File file;

        if (qw != null) {
            long size = ((CountingQuietWriter) qw).getCount();
            LogLog.debug("rolling over count=" + size);
            //   if operation fails, do not roll again until
            //      maxFileSize more bytes are written
            nextRollover = size + maxFileSize;
        }
        LogLog.debug("maxBackupIndex=" + maxBackupIndex);

        boolean renameSucceeded = true;
        // If maxBackups <= 0, then there is no file renaming to be done.
        if (maxBackupIndex > 0) {
            // Delete the oldest file, to keep Windows happy.
            file = new File(fileName.substring(0, fileName.lastIndexOf('.')) + "." + maxBackupIndex
                            + ".log");
            if (file.exists())
                renameSucceeded = file.delete();

            // Map {(maxBackupIndex - 1), ..., 2, 1} to {maxBackupIndex, ..., 3, 2}
            for (int i = maxBackupIndex - 1; i >= 1 && renameSucceeded; i--) {
                file = new File(fileName.substring(0, fileName.lastIndexOf('.')) + "." + i + ".log");
                if (file.exists()) {
                    target = new File(fileName.substring(0, fileName.lastIndexOf('.')) + "." + (i + 1)
                                      + ".log");
                    LogLog.debug("Renaming file " + file + " to " + target);
                    renameSucceeded = file.renameTo(target);
                }
            }

            if (renameSucceeded) {
                // Rename fileName to fileName.1
                target = new File(fileName.substring(0, fileName.lastIndexOf('.')) + "." + 1 + ".log");

                this.closeFile(); // keep windows happy.

                file = new File(fileName);
                LogLog.debug("Renaming file " + file + " to " + target);
                renameSucceeded = file.renameTo(target);
                //
                //   if file rename failed, reopen file with append = true
                //
                if (!renameSucceeded) {
                    try {
                        this.setFile(fileName, true, bufferedIO, bufferSize);
                    } catch (IOException e) {
                        LogLog.error("setFile(" + fileName + ", true) call failed.", e);
                    }
                }
            }
        }

        //
        //   if all renames were successful, then
        //
        if (renameSucceeded) {
            try {
                // This will also close the file. This is OK since multiple
                // close operations are safe.
                this.setFile(fileName, false, bufferedIO, bufferSize);
                nextRollover = 0;
            } catch (IOException e) {
                LogLog.error("setFile(" + fileName + ", false) call failed.", e);
            }
        }
    }

    /**
    This method differentiates RollingFileAppender from its super
    class.
    
    @since 0.9.0
    */
    @Override
    protected void subAppend(
                              LoggingEvent event ) {

        super.subAppend(event);
        if (fileName != null && qw != null) {
            long size = ((CountingQuietWriter) qw).getCount();
            if (size >= maxFileSize && size >= nextRollover) {
                rollOver();
            }
        }
    }

}
