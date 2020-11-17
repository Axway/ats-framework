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
package com.axway.ats.common.performance.monitor.beans;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shares some data between different reading types.
 */
public class SharedReadingBean extends ReadingBean {

    private static final long          serialVersionUID = 1L;

    private Logger                     log              = LogManager.getLogger(this.getClass());

    // format the given float CPU usage values, output 4 digits after decimal point
    // which are later multiplied by 100
    private static final DecimalFormat CPU_USAGE_FORMATTER;
    static {
        CPU_USAGE_FORMATTER = new DecimalFormat("#.####");
        CPU_USAGE_FORMATTER.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    }

    private static final byte MAX_NUMBER_LOGGED_WARNINGS = 10;
    private byte              loggedWarnings             = 0;

    protected float           normalizationFactor;

    public SharedReadingBean( String monitorClass,
                              String name,
                              String unit,
                              float normalizationFactor ) {

        super(monitorClass, name, unit);

        this.normalizationFactor = normalizationFactor;
    }

    /**
     * Invalid double values set to "-1.0"
     *
     * @param value double value
     * @return the double value if it is &gt;= 0 and is not NaN or Infinite, else returns -1
     */
    protected double fixDoubleValue(
                                     double value ) {

        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
            logWarning("Invalid value for " + getName() + " [expected a value >= 0]: " + value);
            return -1.0D;
        } else {
            loggedWarnings = 0; // reset logged warnings counter
            return value;
        }
    }

    /**
     * Invalid double percent values set to "-0.01"
     *
     * @param value double value
     * @return the double value if it is &gt;= 0 and is not NaN or Infinite, else returns -0.01
     */
    protected float fixDoubleValueInPercents(
                                              double value ) {

        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
            logWarning("Invalid value for " + getName() + " [expected a value in range 0.0 - 100.0]: "
                       + value);
            return -0.01F;
        } else {
            loggedWarnings = 0; // reset logged warnings counter
            return Float.parseFloat(CPU_USAGE_FORMATTER.format(value)) * normalizationFactor;
        }
    }

    /**
     * Negative long values set to "-1"
     * 
     * @param value long value
     * @return the long value if it is &gt;= 0, else returns -1
     */
    protected long fixLongValue(
                                 long value ) {

        if (value < 0L) {
            logWarning("Invalid value for " + getName() + " [expected value >= 0]: " + value);
            return -1L;
        } else {
            loggedWarnings = 0; // reset logged warnings counter
            return value;
        }
    }

    /**
    *
    * @param warnMessage warning message
    */
    private void logWarning(
                             String warnMessage ) {

        if (loggedWarnings < MAX_NUMBER_LOGGED_WARNINGS) {

            if (++loggedWarnings == MAX_NUMBER_LOGGED_WARNINGS) {
                warnMessage += "\nThis is the " + MAX_NUMBER_LOGGED_WARNINGS
                               + "th and last time we are logging polling warning for this statistic.";
            }
            log.warn(warnMessage);
        }
    }

    protected void applyMemoryNormalizationFactor() {

        if (unit.startsWith("Byte")) {
            normalizationFactor = 1.0F;
        } else if (unit.startsWith("KB")) {
            normalizationFactor = 1.0F / 1024.0F;
        } else if (unit.startsWith("MB")) {
            normalizationFactor = 1.0F / (1024.0F * 1024.0F);
        } else {
            normalizationFactor = 1.0F;
            logUnknownUnitError(name, new String[]{ "Byte", "KB", "MB" }, "Byte");
        }
    }

    private void logUnknownUnitError(
                                      String readingName,
                                      String[] validUnits,
                                      String defaultUnit ) {

        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Unknown unit for ");
        errorMessage.append(readingName);
        errorMessage.append(". Valid units are [");
        boolean first = true;
        for (String validUnit : validUnits) {
            if (first) {
                first = false;
            } else {
                errorMessage.append(",");
            }
            errorMessage.append(validUnit);
        }
        errorMessage.append("]. We will use the default '");
        errorMessage.append(defaultUnit);
        errorMessage.append("'");
        log.warn(errorMessage);
    }
}
