/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.core.system;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.system.SystemOperationException;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.process.LocalProcessExecutor;
import com.axway.ats.core.reflect.ReflectionUtils;
import com.axway.ats.core.system.model.ISystemInputOperations;
import com.axway.ats.core.system.model.ISystemOperations;
import com.axway.ats.core.threads.ThreadsPerCaller;
import com.axway.ats.core.utils.ClasspathUtils;
import com.axway.ats.core.utils.HostUtils;

public class LocalSystemOperations implements ISystemOperations {

    private static final Logger                  log                         = LogManager.getLogger(LocalSystemOperations.class);

    private static final String                  DATE_FORMAT                 = "MM/dd/yy HH:mm:ss";

    private SystemInputOperations                inputOperations;

    private final static String                  DEFAULT_SCREENSHOT_FILE_EXT = "png";

    private final static Map<Character, Integer> ASCII_TO_MAPPING;
    private final static Map<Character, Integer> ASCII_TO_WITH_SHIFT_MAPPING;

    static {
        ASCII_TO_MAPPING = new HashMap<Character, Integer>();
        ASCII_TO_MAPPING.put('`', KeyEvent.VK_BACK_QUOTE);
        ASCII_TO_MAPPING.put('\'', KeyEvent.VK_QUOTE);

        ASCII_TO_WITH_SHIFT_MAPPING = new HashMap<Character, Integer>();
        ASCII_TO_WITH_SHIFT_MAPPING.put('~', KeyEvent.VK_BACK_QUOTE);
        ASCII_TO_WITH_SHIFT_MAPPING.put('!', KeyEvent.VK_1);
        ASCII_TO_WITH_SHIFT_MAPPING.put('@', KeyEvent.VK_2);
        ASCII_TO_WITH_SHIFT_MAPPING.put('#', KeyEvent.VK_3);
        ASCII_TO_WITH_SHIFT_MAPPING.put('$', KeyEvent.VK_4);
        ASCII_TO_WITH_SHIFT_MAPPING.put('%', KeyEvent.VK_5);
        ASCII_TO_WITH_SHIFT_MAPPING.put('^', KeyEvent.VK_6);
        ASCII_TO_WITH_SHIFT_MAPPING.put('&', KeyEvent.VK_7);
        ASCII_TO_WITH_SHIFT_MAPPING.put('*', KeyEvent.VK_8);
        ASCII_TO_WITH_SHIFT_MAPPING.put('(', KeyEvent.VK_9);
        ASCII_TO_WITH_SHIFT_MAPPING.put(')', KeyEvent.VK_0);
        ASCII_TO_WITH_SHIFT_MAPPING.put('_', KeyEvent.VK_MINUS);
        ASCII_TO_WITH_SHIFT_MAPPING.put('+', KeyEvent.VK_EQUALS);
        ASCII_TO_WITH_SHIFT_MAPPING.put('{', KeyEvent.VK_OPEN_BRACKET);
        ASCII_TO_WITH_SHIFT_MAPPING.put('}', KeyEvent.VK_CLOSE_BRACKET);
        ASCII_TO_WITH_SHIFT_MAPPING.put(':', KeyEvent.VK_SEMICOLON);
        ASCII_TO_WITH_SHIFT_MAPPING.put('"', KeyEvent.VK_QUOTE);
        ASCII_TO_WITH_SHIFT_MAPPING.put('|', KeyEvent.VK_BACK_SLASH);
        ASCII_TO_WITH_SHIFT_MAPPING.put('<', KeyEvent.VK_COMMA);
        ASCII_TO_WITH_SHIFT_MAPPING.put('>', KeyEvent.VK_PERIOD);
        ASCII_TO_WITH_SHIFT_MAPPING.put('?', KeyEvent.VK_SLASH);
    }

    /**
     * Get Operating System type/name
     *
     * @return the {@link OperatingSystemType}
     */
    public OperatingSystemType getOperatingSystemType() {

        return OperatingSystemType.getCurrentOsType();
    }

    @Override
    public String getSystemProperty( String propertyName ) {

        return System.getProperty(propertyName);
    }

    /**
     * Get current system time
     *
     * @param inMilliseconds if the time value is in milliseconds or using a specific Date formatter
     * @return the current system time
     */
    public String getTime( boolean inMilliseconds ) {

        Calendar calendar = Calendar.getInstance();

        String result = "";
        if (inMilliseconds) {
            result = String.valueOf(calendar.getTimeInMillis());
        } else {
            result = new SimpleDateFormat(DATE_FORMAT).format(calendar.getTime());
        }
        return result;
    }

    /**
     * Set the system time
     *
     * @param timestamp the timestamp
     * @param inMilliseconds whether the timestamp is in milliseconds or a formatted date string
     */
    public void setTime( String timestamp, boolean inMilliseconds ) {

        Calendar calendar = Calendar.getInstance();

        // parse the date
        if (inMilliseconds) {
            try {
                calendar.setTimeInMillis(Long.parseLong(timestamp));
            } catch (NumberFormatException e) {
                throw new SystemOperationException("Could not convert timestamp '" + timestamp
                                                   + "' to long");
            }
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            try {
                Date date = dateFormat.parse(timestamp);
                calendar.setTime(date);
            } catch (ParseException e) {
                throw new SystemOperationException("Could not parse timestamp '" + timestamp + "' to format "
                                                   + DATE_FORMAT);
            }
        }

        try {
            OperatingSystemType osType = getOperatingSystemType();
            if (osType == OperatingSystemType.WINDOWS) {
                setWindowsTime(calendar);
            } else {
                setUnixTime(calendar);
            }
        } catch (Exception e) {

            throw new SystemOperationException("Could not set time", e);
        }
    }

    /**
     * @return the current ATS version
     */
    public String getAtsVersion() {

        return AtsVersion.getAtsVersion();
    }

    /**
     * Creates display screenshot and save it in an image file.<br>
     * The currently supported image formats/types are PNG, JPG, JPEG, GIF and BMP<br>
     * <br>
     * <b>NOTE:</b> For remote usage, the filePath value must be only the image file extension eg. ".PNG"
     *
     * @param filePath the screenshot image file path. If the file extension is not specified, the default format PNG will be used
     * @return the full path to the locally saved screenshot image file
     */
    public String createScreenshot( String filePath ) {

        try {
            if (filePath == null) {
                filePath = "." + DEFAULT_SCREENSHOT_FILE_EXT;
            }
            // extract and validate the image file type
            String fileType = null;
            int extensionIndex = filePath.lastIndexOf('.');
            if (extensionIndex >= 0 && filePath.length() > (extensionIndex + 1)) {
                fileType = filePath.substring(extensionIndex + 1);
                try {
                    ImageType.valueOf(fileType.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    throw new SystemOperationException("Unsupported image file type \"" + fileType
                                                       + "\". Use one of these: "
                                                       + Arrays.toString(ImageType.values()));
                }
            } else {
                log.info("The screenshot file extension was not specified, the default one '.png' will be used instead");
                fileType = DEFAULT_SCREENSHOT_FILE_EXT;
                filePath = filePath + "." + fileType;
            }

            File imageFile = null;
            if (filePath.indexOf('.') == 0) {

                imageFile = File.createTempFile("ats_", "." + fileType);
                imageFile.deleteOnExit();
                filePath = imageFile.getCanonicalPath();
            } else {

                imageFile = new File(filePath);
            }
            Rectangle area = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            RenderedImage renderedImage = new Robot().createScreenCapture(area);
            ImageIO.write(renderedImage, fileType, imageFile);

        } catch (Exception e) {

            throw new SystemOperationException("Could not write Screenshot image to file '" + filePath + "'",
                                               e);
        }
        return filePath;
    }

    /**
     *
     * @param host host address
     * @param port port number
     * @param timeout timeout value in milliseconds
     * @return <code>true</code> if the address is listening on this port
     * @throws SystemOperationException when the target host is unknown
     */
    public boolean isListening( String host, int port, int timeout ) {

        Socket socket = null;
        long endTime = System.currentTimeMillis() + timeout;
        try {
            do {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), timeout);
                    return true;
                } catch (UnknownHostException uhe) {
                    throw new SystemOperationException("Unknown host '" + host + "'", uhe);
                } catch (Exception e) {
                    // Connection refused
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ie) {}
                }
            } while (endTime > System.currentTimeMillis());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {}
            }
        }

        return false;
    }

    /**
     * Return system input operations instance
     */
    public ISystemInputOperations getInputOperations() {

        if (inputOperations == null) {
            inputOperations = new SystemInputOperations();
        }
        return inputOperations;
    }

    public String getHostname() {

        return HostUtils.getLocalHostName();
    }

    public String[] getClassPath() {

        return new ClasspathUtils().getClassPathArray();
    }

    public void logClassPath() {

        new ClasspathUtils().logClassPath();
    }

    public String[] getDuplicatedJars() {

        return new ClasspathUtils().getDuplicatedJars();
    }

    public void logDuplicatedJars() {

        new ClasspathUtils().logProblematicJars();
    }

    public void setAtsDbAppenderThreshold( Level threshold ) {

        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        Map<String, Appender> appenders = config.getAppenders();
        // there is a method called -> context.getConfiguration().getAppender(java.lang.String name)
        // maybe we should use this one to obtain Active and Passive DB appenders?
        if (appenders != null && appenders.size() > 0) {
            for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
                Appender appender = entry.getValue();
                if (appender != null) {
                    if (appender.getClass().getName().equals("com.axway.ats.log.appenders.ActiveDbAppender")) {
                        // assume that only ATS is going to attach filter to this appender
                        // so remove the previous one and attach the new one
                        if ( ((AbstractAppender) appender).hasFilter()) {
                            Filter currentFilter = ((AbstractAppender) appender).getFilter();
                            if (currentFilter != null) {
                                ((AbstractAppender) appender).removeFilter(currentFilter);
                            }
                        }
                        ((AbstractAppender) appender).addFilter(ThresholdFilter.createFilter(threshold, Result.ACCEPT,
                                                                                             Result.DENY));
                    }
                    if (appender.getClass().getName().equals("com.axway.ats.log.appenders.PassiveDbAppender")) {
                        String callerId = ThreadsPerCaller.getCaller();
                        String passiveDbAppenderCaller = (String) ReflectionUtils.getFieldValue(appender, "caller",
                                                                                                true);
                        if (callerId != null && callerId.equals(passiveDbAppenderCaller)) {
                            // assume that only ATS is going to attach filter to this appender
                            // so remove the previous one and attach the new one
                            if ( ((AbstractAppender) appender).hasFilter()) {
                                Filter currentFilter = ((AbstractAppender) appender).getFilter();
                                if (currentFilter != null) {
                                    ((AbstractAppender) appender).removeFilter(currentFilter);
                                }
                            }
                            ((AbstractAppender) appender).addFilter(ThresholdFilter.createFilter(threshold,
                                                                                                 Result.ACCEPT,
                                                                                                 Result.DENY));
                        }
                    }
                }
            }
        }
    }

    public void attachFileAppender( String filepath, String messageFormatPattern ) {

        try {
            PatternLayout patternLayout = PatternLayout.newBuilder().withPattern(messageFormatPattern).build();
            final LoggerContext context = LoggerContext.getContext(false);
            final Configuration config = context.getConfiguration();

            // this name may get too long
            String name = "file-appender-" + ThreadsPerCaller.getCaller() + filepath.replace("\\", "_");

            //TODO: a check should be made that such appender exists
            
            FileAppender fileAppender = FileAppender.newBuilder()
                                                    .setName(name)
                                                    .setLayout(patternLayout)
                                                    .withFileName(filepath)
                                                    .build();

            fileAppender.start();
            config.addAppender(fileAppender);
            // context.getRootLogger().addAppender(config.getAppender(fa.getName())); Is this needed?!?
            context.updateLoggers();

        } catch (Exception e) {
            throw new RuntimeException("Could not attach file appender '" + filepath + "'", e);
        }

    }

    /**
     * Set the time on a windows system
     *
     * @param calendar the time to set
     */
    private void setWindowsTime( Calendar calendar ) throws Exception {

        // we are using the default date format and the system specific time format

        // set the date
        String dateArguments = String.format("%1$02d-%2$02d-%3$02d", calendar.get(Calendar.DAY_OF_MONTH),
                                             calendar.get(Calendar.MONTH) + 1,
                                             calendar.get(Calendar.YEAR));

        LocalProcessExecutor processExecutor = new LocalProcessExecutor(HostUtils.LOCAL_HOST_IPv4,
                                                                        "cmd /c date " + dateArguments);
        processExecutor.execute();

        // set the time
        String timeArguments = SimpleDateFormat.getTimeInstance().format(calendar.getTime());
        processExecutor = new LocalProcessExecutor(HostUtils.LOCAL_HOST_IPv4,
                                                   "cmd /c time " + timeArguments);
        processExecutor.execute();
    }

    /**
     * Set the time on a UNIX box
     *
     * @param calendar the time to set
     */
    private void setUnixTime( Calendar calendar ) {

        // set the date
        //MMDDhhmm[[CC]YY][.ss]
        String dateArguments = String.format("%1$02d%2$02d%3$02d%4$02d%5$04d.%6$02d",
                                             calendar.get(Calendar.MONTH) + 1,
                                             calendar.get(Calendar.DAY_OF_MONTH),
                                             calendar.get(Calendar.HOUR_OF_DAY),
                                             calendar.get(Calendar.MINUTE), calendar.get(Calendar.YEAR),
                                             calendar.get(Calendar.SECOND));

        LocalProcessExecutor processExecutor = new LocalProcessExecutor(HostUtils.LOCAL_HOST_IPv4, "date",
                                                                        dateArguments);
        processExecutor.execute();
    }

    /**
     * System input operations. Simulating mouse and keyboard actions
     *
     */
    public class SystemInputOperations implements ISystemInputOperations {

        private Robot robot;

        /**
         * Move the mouse at (X,Y) screen position and then click the mouse button 1
         *
         * @param x the X coordinate
         * @param y the Y coordinate
         */
        public void clickAt( int x, int y ) {

            Robot robot = getRobot();
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.delay(500);
        }

        /**
         * Type some text
         *
         * @param text the text to type
         */
        public void type( String text ) {

            typeAll(text);
        }

        /**
         * Type some keys defined in java.awt.event.KeyEvent
         * @param keyCodes the special key codes
         */
        public void type( int... keyCodes ) {

            typeAll(null, keyCodes);
        }

        /**
         * Type some text but combine them with some keys defined in java.awt.event.KeyEvent
         * <br>It first presses the special key codes(for example Alt + Shift),
         * then it types the provided text and then it releases the special keys in
         * reversed order(for example Shift + Alt )
         *
         * @param text the text to type
         * @param keyCodes the special key codes
         */
        public void type( String text, int... keyCodes ) {

            typeAll(text, keyCodes);
        }

        /**
         * Press the TAB key
         */
        public void pressTab() {

            typeAll(null, KeyEvent.VK_TAB);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }

        /**
         * Press the SPACE key
         */
        public void pressSpace() {

            typeAll(null, KeyEvent.VK_SPACE);
        }

        /**
         * Press the ENTER key
         */
        public void pressEnter() {

            typeAll(null, KeyEvent.VK_ENTER);
        }

        /**
         * Press the Escape key
         */
        public void pressEsc() {

            typeAll(null, KeyEvent.VK_ESCAPE);
        }

        /**
         * Press Alt + F4 keys
         */
        public void pressAltF4() {

            typeAll(null, KeyEvent.VK_ALT, KeyEvent.VK_F4);
        }

        /**
         * Presses a given key. The key should be released using the keyRelease method.
         *
         * @param keyCode Key to press (e.g. {@link KeyEvent}.VK_A)
         */
        public void keyPress( int keyCode ) {

            try {
                getRobot().keyPress(keyCode);
            } catch (IllegalArgumentException iae) {
                throw new SystemOperationException("Error pressing '" + KeyEvent.getKeyText(keyCode)
                                                   + "' key", iae);
            }
        }

        /**
         * Releases a given key.
         *
         * @param keyCode Key to release (e.g. {@link KeyEvent}.VK_A)
         */
        public void keyRelease( int keyCode ) {

            try {
                getRobot().keyRelease(keyCode);
            } catch (IllegalArgumentException iae) {
                throw new SystemOperationException("Error releasing '" + KeyEvent.getKeyText(keyCode)
                                                   + "' key", iae);
            }
        }

        private Robot getRobot() {

            try {
                if (robot == null) {
                    robot = new Robot();
                }
                return robot;
            } catch (AWTException awte) {

                throw new SystemOperationException("Error initializing java Robot", awte);
            }
        }

        private void typeAll( String text, int... keyCodes ) {

            // tell the user what we are going to type
            String typeCommand = log(text, keyCodes);

            int lastPressedIndex = 0;
            try {

                // press the special keys
                for (int keyCode : keyCodes) {
                    keyPress(keyCode);
                    lastPressedIndex++;
                }

                if (text != null) {
                    for (int i = 0; i < text.length(); ++i) {
                        char charValue = text.charAt(i);
                        keyPressAndRelease(charValue, typeCommand);
                    }
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}

            } finally {

                // release the special keys in reversed order
                for (int i = lastPressedIndex - 1; i >= 0; i--) {
                    keyRelease(keyCodes[i]);
                }
            }
        }

        private void keyPressAndRelease( char charValue, String typeCommand ) {

            Robot robot = getRobot();

            int intValue = (int) charValue;
            boolean needsShifting = isShiftedChar(charValue);

            intValue = processSpecialCharacter(intValue);

            if (needsShifting) {
                robot.keyPress(KeyEvent.VK_SHIFT);
            }
            try {
                robot.keyPress(intValue);
                robot.keyRelease(intValue);
            } catch (Exception e) {
                throw new SystemOperationException("Error typing '" + KeyEvent.getKeyText(intValue)
                                                   + "' (char value is '" + charValue + "') as part of "
                                                   + typeCommand, e);
            } finally {
                if (needsShifting) {
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                }
            }
        }

        private boolean isShiftedChar( char charValue ) {

            boolean isShiftedChar = false;

            if (Character.isLetter(charValue)) {
                if (Character.isUpperCase(charValue)) {
                    isShiftedChar = true;
                }
            } else {
                isShiftedChar = ASCII_TO_WITH_SHIFT_MAPPING.containsKey(charValue);
            }
            return isShiftedChar;
        }

        private int processSpecialCharacter( int intValue ) {

            // fix lower case letters
            if (Character.isLetter(intValue) && Character.isLowerCase(intValue)) {
                intValue -= 0x20;
            } else {
                Character charValue = Character.valueOf((char) intValue);
                if (ASCII_TO_WITH_SHIFT_MAPPING.containsKey(charValue)) {

                    intValue = ASCII_TO_WITH_SHIFT_MAPPING.get(charValue);
                } else if (ASCII_TO_MAPPING.containsKey(charValue)) {

                    intValue = ASCII_TO_MAPPING.get(charValue);
                }
            }
            return intValue;
        }

        private String log( String text, int... keyCodes ) {

            StringBuilder typeInfo = new StringBuilder();
            if (keyCodes.length > 0) {
                boolean firstKey = true;
                for (int keyCode : keyCodes) {
                    if (firstKey) {
                        firstKey = false;
                    } else {
                        typeInfo.append(" + ");
                    }
                    typeInfo.append("'");
                    typeInfo.append(KeyEvent.getKeyText(keyCode));
                    typeInfo.append("'");
                }
            }
            if (text != null) {
                if (keyCodes.length > 0) {
                    typeInfo.append(" + ");
                }
                typeInfo.append("'");
                typeInfo.append(text);
                typeInfo.append("'");
            }

            log.info("Typing: " + typeInfo);
            return typeInfo.toString();
        }
    }

    private enum ImageType {

        PNG, JPG, JPEG, GIF, BMP;
    }

}
