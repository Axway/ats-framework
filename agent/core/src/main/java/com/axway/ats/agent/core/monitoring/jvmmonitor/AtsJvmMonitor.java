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
package com.axway.ats.agent.core.monitoring.jvmmonitor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.performance.monitor.PerformanceMonitor;
import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.core.monitoring.SystemMonitorDefinitions;
import com.axway.ats.core.monitoring.UnsupportedReadingException;
import com.axway.ats.core.utils.StringUtils;

/**
 * The ATS JVM monitor
 */
public class AtsJvmMonitor extends PerformanceMonitor {

    private static final Logger       log              = LogManager.getLogger(AtsJvmMonitor.class);

    // the key is the jvm port, the values are the mbeans
    private Map<String, MBeanWrapper> mbeanWrappers    = new HashMap<String, MBeanWrapper>();
    private MBeanServerConnection     connection;

    private List<JvmReadingInstance>  readingInstances = new ArrayList<JvmReadingInstance>();

    @Override
    public void init(
                      ReadingBean[] readings ) throws Exception {

        log.info("Initializing the ATS JVM Monitor");

        initJMXConnection(readings);

        // more time is needed for the initialization
        Thread.sleep(200);

        // create the actual reading instances
        createReadingInstances(readings);
    }

    private void initJMXConnection(
                                    ReadingBean[] readings ) throws Exception {

        // we are looking for all jmx port and put them in the mbeanWrappers map
        int jmxPort;
        for (int i = 0; i < readings.length; i++) {
            try {
                jmxPort = Integer.parseInt(readings[i].getParameter("JMX_PORT"));
                mbeanWrappers.put(readings[i].getParameter("JMX_PORT"), new MBeanWrapper(jmxPort));
            } catch (Exception e) {
                final String msg = "Error initializing the JMX monitor. We could not extract a valid JMX port number.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        }
    }

    @Override
    public void deinit() throws Exception {

    }

    @Override
    public List<ReadingBean> pollNewDataForFirstTime() throws Exception {

        return doPoll(true);
    }

    @Override
    public List<ReadingBean> pollNewData() throws Exception {

        return doPoll(false);
    }

    public List<ReadingBean> doPoll(
                                     boolean isFirstTime ) throws Exception {

        List<ReadingBean> redingsResult = new ArrayList<ReadingBean>();

        // poll the reading instances
        redingsResult.addAll(pollReadingInstances(readingInstances));

        return redingsResult;
    }

    private List<ReadingBean> pollReadingInstances(
                                                    List<JvmReadingInstance> readingInstances ) throws Exception {

        List<ReadingBean> redingsResult = new ArrayList<ReadingBean>();

        for (JvmReadingInstance readingInstance : readingInstances) {
            float value = readingInstance.poll();

            ReadingBean newResult = readingInstance.getNewCopy();
            newResult.setValue(String.valueOf(value));
            redingsResult.add(newResult);
        }

        return redingsResult;
    }

    private void createReadingInstances(
                                         ReadingBean[] readings ) throws UnsupportedReadingException {

        readingInstances = new ArrayList<JvmReadingInstance>();

        for (ReadingBean reading : readings) {

            String readingName = reading.getName();

            JvmReadingInstance readingInstance = null;
            if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__CPU_USAGE)) {
                readingInstance = getCpuUsage(connection, reading);
            } else if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__MEMORY_HEAP)) {
                readingInstance = getHeap(connection, reading);
            } else if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__MEMORY_HEAP_YOUNG_GENERATION_EDEN)) {
                readingInstance = getHeapEden(connection, reading);
            } else if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__MEMORY_HEAP_YOUNG_GENERATION_SURVIVOR)) {
                readingInstance = getHeapSurvivor(connection, reading);
            } else if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__MEMORY_HEAP_OLD_GENERATION)) {
                readingInstance = getHeapOldGen(connection, reading);
            } else if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__MEMORY_PERMANENT_GENERATION)) {
                readingInstance = getHeapPermGen(connection, reading);
            } else if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__MEMORY_CODE_CACHE)) {
                readingInstance = getHeapCodeCache(connection, reading);
            } else if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__CLASSES_COUNT)) {
                readingInstance = getClassesCount(connection, reading);
            } else if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__THREADS_COUNT)) {
                readingInstance = getThreadsCount(connection, reading);
            } else if (readingName.equalsIgnoreCase(SystemMonitorDefinitions.READING_JVM__THREADS_DAEMON_COUNT)) {
                readingInstance = getDaemonThreadsCount(connection, reading);
            } else if (!StringUtils.isNullOrEmpty(reading.getParameter("MBEAN_NAME"))) {
                readingInstance = getCustomMBeanProperty(connection, reading);
            } else {
                throw new UnsupportedReadingException(readingName);
            }

            readingInstances.add(readingInstance);
        }
    }

    private JvmReadingInstance getCpuUsage(
                                            MBeanServerConnection connection,
                                            ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      1.0F) {
            private static final long serialVersionUID = 1L;

            private boolean           firstPoll        = true;

            @Override
            public void init() {

                mBeanName = mbeanWrapper.getObjectName("java.lang:type=OperatingSystem");
            }

            @Override
            public float poll() {

                double dValue = 100
                                * fixDoubleValueInPercents((Double) mbeanWrapper.getMBeanAttribute(mBeanName,
                                                                                                   "ProcessCpuLoad"));

                if (firstPoll && dValue == -1) {
                    // for some reason the first call here after starting the agent returns -1
                    firstPoll = false;
                    return 0F;
                }

                // return a float with 2 digits after the decimal point
                return new BigDecimal(dValue).setScale(2, BigDecimal.ROUND_DOWN).floatValue();
            }
        };
    }

    private JvmReadingInstance getHeap(
                                        MBeanServerConnection connection,
                                        ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      0) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() {

                applyMemoryNormalizationFactor();

                mBeanName = mbeanWrapper.getObjectName("java.lang:type=Memory");
            }

            @Override
            public float poll() {

                CompositeData attribute = (CompositeData) mbeanWrapper.getMBeanAttribute(mBeanName,
                                                                                         "HeapMemoryUsage");
                return fixLongValue(Long.valueOf( (attribute).get("used").toString()))
                       * normalizationFactor;
            }
        };
    }

    private JvmReadingInstance getHeapEden(
                                            MBeanServerConnection connection,
                                            ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      0) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() {

                applyMemoryNormalizationFactor();

                mBeanName = mbeanWrapper.getObjectNames(".*Eden Space.*", true).iterator().next();
            }

            @Override
            public float poll() {

                CompositeData attribute = (CompositeData) mbeanWrapper.getMBeanAttribute(mBeanName,
                                                                                         "Usage");

                return fixLongValue(Long.valueOf( (attribute).get("used").toString()))
                       * normalizationFactor;
            }
        };
    }

    private JvmReadingInstance getHeapSurvivor(
                                                MBeanServerConnection connection,
                                                ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      0) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() {

                applyMemoryNormalizationFactor();

                mBeanName = mbeanWrapper.getObjectNames(".*Survivor Space.*", true).iterator().next();
            }

            @Override
            public float poll() {

                CompositeData attribute = (CompositeData) mbeanWrapper.getMBeanAttribute(mBeanName,
                                                                                         "Usage");

                return fixLongValue(Long.valueOf( (attribute).get("used").toString()))
                       * normalizationFactor;
            }
        };
    }

    private JvmReadingInstance getHeapOldGen(
                                              MBeanServerConnection connection,
                                              ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      0) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() {

                applyMemoryNormalizationFactor();

                // Java 7/8 returns the Bean name as type,name, while java 9+ name,type so here we check for both formats
                mBeanName = mbeanWrapper.getObjectNames(".*(type=MemoryPool,name=.*Old Gen.*)|.*(name=.*Old Gen.*,type=MemoryPool).*", true).iterator().next();
            }

            @Override
            public float poll() {

                CompositeData attribute = (CompositeData) mbeanWrapper.getMBeanAttribute(mBeanName,
                                                                                         "Usage");

                return fixLongValue(Long.valueOf( (attribute).get("used").toString()))
                       * normalizationFactor;
            }
        };
    }

    private JvmReadingInstance getHeapPermGen(
                                               MBeanServerConnection connection,
                                               ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      0) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() {

                applyMemoryNormalizationFactor();

                mBeanName = mbeanWrapper.getObjectNames(".*(type=MemoryPool,name=Metaspace)|.*(name=Metaspace,type=MemoryPool).*", true).iterator().next();
            }

            @Override
            public float poll() {

                CompositeData attribute = (CompositeData) mbeanWrapper.getMBeanAttribute(mBeanName,
                                                                                         "Usage");

                return fixLongValue(Long.valueOf( (attribute).get("used").toString()))
                       * normalizationFactor;
            }
        };
    }

    private JvmReadingInstance getHeapCodeCache(
                                                 MBeanServerConnection connection,
                                                 ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      0) {
            private static final long serialVersionUID = 1L;

            /**
             * This is set of all of the ObjectName(s) (Beans) that represent the heap size
             * */
            Set<ObjectName>           heaps            = null;

            @Override
            public void init() {

                applyMemoryNormalizationFactor();

                String javaVersion = System.getProperty("java.version");

                if (javaVersion.startsWith("1.")) {
                    mBeanName = mbeanWrapper.getObjectName("java.lang:name=Code Cache,type=MemoryPool");
                } else {
                    heaps = mbeanWrapper.getObjectNames(".*CodeHeap.*", false);
                    // save the first as a bean, because our logic requires this to be not null
                    mBeanName = heaps.iterator().next();
                }

            }

            @Override
            public float poll() {

                long value = 0;
                if (heaps != null) {

                    Iterator<ObjectName> it = heaps.iterator();
                    while (it.hasNext()) {
                        CompositeData attribute = (CompositeData) mbeanWrapper.getMBeanAttribute(it.next(),
                                                                                                 "Usage");
                        value += Long.valueOf( (attribute).get("used").toString());
                    }

                } else {
                    CompositeData attribute = (CompositeData) mbeanWrapper.getMBeanAttribute(mBeanName,
                                                                                             "Usage");
                    value = Long.valueOf( (attribute).get("used").toString());
                }

                return fixLongValue(value)
                       * normalizationFactor;
            }
        };
    }

    private JvmReadingInstance getClassesCount(
                                                MBeanServerConnection connection,
                                                ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      1) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() {

                mBeanName = mbeanWrapper.getObjectName("java.lang:type=ClassLoading");
            }

            @Override
            public float poll() {

                return fixLongValue(Long.valueOf(mbeanWrapper.getMBeanAttribute(mBeanName,
                                                                                "LoadedClassCount")
                                                             .toString()))
                       * normalizationFactor;
            }
        };
    }

    private JvmReadingInstance getThreadsCount(
                                                MBeanServerConnection connection,
                                                ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      1) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() {

                mBeanName = mbeanWrapper.getObjectName("java.lang:type=Threading");
            }

            @Override
            public float poll() {

                return fixLongValue(Long.valueOf(mbeanWrapper.getMBeanAttribute(mBeanName, "ThreadCount")
                                                             .toString()))
                       * normalizationFactor;
            }
        };
    }

    private JvmReadingInstance getDaemonThreadsCount(
                                                      MBeanServerConnection connection,
                                                      ReadingBean reading ) {

        String jvmPort = reading.getParameter("JMX_PORT");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jvmPort);

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      getName(reading, jvmPort),
                                      reading.getUnit(),
                                      1) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() {

                mBeanName = mbeanWrapper.getObjectName("java.lang:type=Threading");
            }

            @Override
            public float poll() {

                return fixLongValue(Long.valueOf(mbeanWrapper.getMBeanAttribute(mBeanName,
                                                                                "DaemonThreadCount")
                                                             .toString()))
                       * normalizationFactor;
            }
        };
    }

    private JvmReadingInstance getCustomMBeanProperty(
                                                       MBeanServerConnection connection,
                                                       final ReadingBean reading ) {

        final Map<String, String> properties = reading.getParameters();
        String jmxPort = properties.get("JMX_PORT");
        properties.remove("JMX_PORT");
        final String mbeanName = properties.get("MBEAN_NAME");
        properties.remove("MBEAN_NAME");
        String alias = properties.get("PARAMETER_NAME__PROCESS_ALIAS");
        properties.remove("PARAMETER_NAME__PROCESS_ALIAS");
        final MBeanWrapper mbeanWrapper = mbeanWrappers.get(jmxPort);
        final String name = "[JVM] " + alias;

        return new JvmReadingInstance(connection,
                                      String.valueOf(reading.getDbId()),
                                      reading.getMonitorName(),
                                      name,
                                      reading.getUnit(),
                                      1) {
            private static final long serialVersionUID = 1L;

            @Override
            public void init() {

                mBeanName = mbeanWrapper.getObjectName(mbeanName);
            }

            @Override
            public float poll() {

                applyMemoryNormalizationFactor();

                Object data = mbeanWrapper.getMBeanAttribute(mBeanName, reading.getName());

                if (data instanceof Double) {

                    double dValue = 100 * fixDoubleValueInPercents((Double) data);
                    return new BigDecimal(dValue).setScale(2, BigDecimal.ROUND_DOWN).floatValue();

                } else if (data instanceof Long) {

                    return fixLongValue(Long.valueOf(data.toString())) * normalizationFactor;

                } else if (data instanceof Integer) {

                    return Integer.valueOf(data.toString());

                } else if (data instanceof CompositeData) {

                    List<Object> mbeanAttribues = new ArrayList<Object>(Arrays.asList(properties.keySet()
                                                                                                .toArray()));
                    mbeanAttribues.remove(reading.getName());
                    String result = getCompositeData(data, mbeanAttribues);

                    return fixLongValue(Long.valueOf(result)) * normalizationFactor;
                }
                throw new RuntimeException("This is a currently not supported: MBean '" + mBeanName
                                           + "' with attribute '" + reading.getName() + "' of type '"
                                           + data.getClass().getName() + "'");
            }
        };
    }

    private String getCompositeData(
                                     Object data,
                                     List<Object> list ) {

        if (data instanceof CompositeData) {
            if (list.isEmpty()) {
                throw new RuntimeException("Not enough attributes are given for this MBean to be monitored. Please specify all needed attributes");
            }
            String prop = list.get(0).toString();
            // we are removing the first element so we could take the next element on next iteration
            list.remove(0);
            return getCompositeData( ((CompositeData) data).get(prop), list);
        } else {
            return data.toString();
        }
    }

    private String getName(
                            ReadingBean reading,
                            String jvmPort ) {

        String readingName = reading.getName().substring(6);
        String jvmAlias = reading.getParameter(SystemMonitorDefinitions.PARAMETER_NAME__PROCESS_ALIAS);
        if (!StringUtils.isNullOrEmpty(jvmAlias)) {
            readingName = " [JVM] " + jvmAlias + ": " + readingName;
        } else {
            readingName = " [JVM] p" + jvmPort + ": " + readingName;
        }

        return readingName;
    }

    @Override
    public String getDescription() {

        return "ATS JVM Performance Monitor";
    }
}
