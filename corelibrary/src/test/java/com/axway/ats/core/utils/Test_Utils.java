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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.axway.ats.core.BaseTest;

public class Test_Utils extends BaseTest {

    @Test
    public void testCommandLineAgrumentsParser_1() {

        String[] expectedArgs = new String[]{ "/bin/bash", "-c", "java -jar atsAgent.jar" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/bin/bash -c \"java -jar atsAgent.jar\"");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_2() {

        String[] expectedArgs = new String[]{ "cmd", "/C", "java -jar atsAgent.jar" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("cmd /C 'java -jar atsAgent.jar'");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);

    }

    @Test
    public void testCommandLineAgrumentsParser_3() {

        String[] expectedArgs = new String[]{ "cmd.exe", "/C", "java", "-jar", "atsAgent.jar" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("cmd.exe /C java -jar atsAgent.jar");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);

    }

    @Test
    public void testCommandLineAgrumentsParser_4() {

        String[] expectedArgs = new String[]{ "/bin/bash", "-c", "/tmp/test.sh", "start" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/bin/bash -c /tmp/test.sh start");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_5() {

        String[] expectedArgs = new String[]{ "/bin/bash", "-c", "/tmp/test script.sh", "start", "-test",
                                              "last test arg" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/bin/bash -c \"/tmp/test script.sh\" start -test \"last test arg\"");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_6() {

        String[] expectedArgs = new String[]{ "/bin/bash", "-c", "/tmp/test script.sh", "start", "-test",
                                              "last test arg" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/bin/bash -c \"/tmp/test script.sh\" start -test 'last test arg'");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_7() {

        String[] expectedArgs = new String[]{ "/bin/bash", "-c", "/tmp/test script.sh start", "-test",
                                              "last test arg" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/bin/bash -c '/tmp/test script.sh start' -test 'last test arg'");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_8() {

        String[] expectedArgs = new String[]{ "/etc/init.d/test-script.local", "restart" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/etc/init.d/test-script.local restart");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_9() {

        String[] expectedArgs = new String[]{ "/etc/init.d/test-script.local" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/etc/init.d/test-script.local");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_10() {

        String[] expectedArgs = new String[]{ "ps", "aux" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("ps aux");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_11() {

        String[] expectedArgs = new String[]{ "ls" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("ls");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_12() {

        String[] expectedArgs = new String[]{ "ps", "aux", "|", "grep", "java" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("ps aux | grep java");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_13() {

        String[] expectedArgs = new String[]{ "/bin/bash", "-c", "ps aux   | grep java" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/bin/bash -c \"ps aux   | grep java\"");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_14() {

        String[] expectedArgs = new String[]{ "/bin/bash", "-c", "cd \"/tmp/test space\" | ls -l" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/bin/bash -c 'cd \"/tmp/test space\" | ls -l'");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testCommandLineAgrumentsParser_15() {

        String[] expectedArgs = new String[]{ "/bin/bash", "-c", "cd '/tmp/test space' | ls -l", "-test",
                                              "ls \"/tmp/other test dir/dir/\"" };
        String[] actualArgs = StringUtils.parseCommandLineArguments("/bin/bash -c \"cd '/tmp/test space' | ls -l\" -test 'ls \"/tmp/other test dir/dir/\"'");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testIPv6Compression_1() {

        String expectedIPv6Addr = "fe80::baac:6fff:fe2b:5bdd%2";
        String actualIPv6Addr = HostUtils.compressIPv6Address("fe80:0:0:0:baac:6fff:fe2b:5bdd%2");

        Assert.assertEquals(expectedIPv6Addr, actualIPv6Addr);
    }

    @Test
    public void testIPv6Compression_2() {

        String expectedIPv6Addr = "::baac:6fff:fe2b:5bdd";
        String actualIPv6Addr = HostUtils.compressIPv6Address("0:0:0:0:baac:6fff:fe2b:5bdd");

        Assert.assertEquals(expectedIPv6Addr, actualIPv6Addr);
    }

    @Test
    public void testIPv6Compression_3() {

        String expectedIPv6Addr = "6f::baac:2b:5bdd";
        String actualIPv6Addr = HostUtils.compressIPv6Address("006f:0000:0:000:00:baac:002b:5bdd");

        Assert.assertEquals(expectedIPv6Addr, actualIPv6Addr);
    }

    @Test
    public void testIPv6Compression_4() {

        String expectedIPv6Addr = "::1";
        String actualIPv6Addr = HostUtils.compressIPv6Address("0:0:0:0:0:0:0:1");

        Assert.assertEquals(expectedIPv6Addr, actualIPv6Addr);
    }

    @Test
    public void testIPv6Compression_5() {

        String expectedIPv6Addr = "fe80::e103%eth1";
        String actualIPv6Addr = HostUtils.compressIPv6Address("fe80:0:0:0:000:0000:00:e103%eth1");

        Assert.assertEquals(expectedIPv6Addr, actualIPv6Addr);
    }

    @Test
    public void testIPv6Compression_6() {

        String expectedIPv6Addr = "fe80::ff0:0:0:e100%2";
        String actualIPv6Addr = HostUtils.compressIPv6Address("fe80:00:0000:0:0ff0:0000:00:e100%2");

        Assert.assertEquals(expectedIPv6Addr, actualIPv6Addr);
    }

    @Test
    public void testSplittingAddressHostAndPort_1() {

        String[] expectedArgs = new String[]{ "[fe80:00:0000:0:0ff0:0000:00:e100%2]", "1234" };
        String[] actualArgs = HostUtils.splitAddressHostAndPort("[fe80:00:0000:0:0ff0:0000:00:e100%2]:1234");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testSplittingAddressHostAndPort_2() {

        String[] expectedArgs = new String[]{ "[::e100%eth0]", "8089" };
        String[] actualArgs = HostUtils.splitAddressHostAndPort("[::e100%eth0]:8089");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testSplittingAddressHostAndPort_3() {

        String[] expectedArgs = new String[]{ "::e100%eth0" };
        String[] actualArgs = HostUtils.splitAddressHostAndPort("::e100%eth0");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testSplittingAddressHostAndPort_4() {

        String[] expectedArgs = new String[]{ "6f::baac:2b:5bdd" };
        String[] actualArgs = HostUtils.splitAddressHostAndPort("6f::baac:2b:5bdd");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testSplittingAddressHostAndPort_5() {

        String[] expectedArgs = new String[]{ "196.168.0.5" };
        String[] actualArgs = HostUtils.splitAddressHostAndPort("196.168.0.5");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testSplittingAddressHostAndPort_6() {

        String[] expectedArgs = new String[]{ "196.168.0.5", "8090" };
        String[] actualArgs = HostUtils.splitAddressHostAndPort("196.168.0.5:8090");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    @Test
    public void testSplittingAddressHostAndPort_7() {

        String[] expectedArgs = new String[]{ "test.auto-lab.int", "8088" };
        String[] actualArgs = HostUtils.splitAddressHostAndPort("test.auto-lab.int:8088");

        Assert.assertArrayEquals(getArrayNotEqualsMsg(expectedArgs, actualArgs), expectedArgs,
                                 actualArgs);
    }

    private String getArrayNotEqualsMsg( String[] expectedArgs, String[] actualArgs ) {

        return "The expected array: " + Arrays.asList(expectedArgs) + " is different from the actual: "
               + Arrays.asList(actualArgs);
    }

    @Test
    public void testLocalHost_HostLocality() {

        String host = "localhost";
        HostUtils.setHostLocality(host, true);
        Assert.assertTrue(HostUtils.isLocalHost(host));

        host = "localhost";
        HostUtils.setHostLocality(host, false);
        // this was failing up to 3.11.1-1-15
        Assert.assertFalse(HostUtils.isLocalHost(host));
        HostUtils.setHostLocality(host, true); // revert

        host = "127.0.0.1";
        HostUtils.setHostLocality(host, false);
        Assert.assertFalse(HostUtils.isLocalHost(host));
        HostUtils.setHostLocality(host, true); // revert

        host = "192.168.1.1"; // 10.11.12.13
        HostUtils.setHostLocality(host, false);
        Assert.assertFalse(HostUtils.isLocalHost(host));
        HostUtils.setHostLocality(host, true); // revert

        host = "10.11.12.13"; // 10.11.12.13
        HostUtils.setHostLocality(host, false);
        Assert.assertFalse(HostUtils.isLocalHost(host));

        host = "my.temp.domain";
        HostUtils.setHostLocality(host, false);
        Assert.assertFalse(HostUtils.isLocalHost(host));

        host = "my.temp.domain";
        HostUtils.setHostLocality(host, false);
        Assert.assertFalse(HostUtils.isLocalAtsAgent(host + ":8089"));
    }

}
