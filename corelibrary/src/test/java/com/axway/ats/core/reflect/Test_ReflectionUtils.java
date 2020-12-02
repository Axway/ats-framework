/*
 * Copyright 2019 Axway Software
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
package com.axway.ats.core.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

public class Test_ReflectionUtils {

    @Test
    public void test() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                       NoSuchMethodException, SecurityException, NoSuchFieldException {

        TestClassReflectionUtils srcInstance = new TestClassReflectionUtils(0, false, (byte) 0, (char) 0);
        Method m_copy = ReflectionUtils.getMethod(TestClassReflectionUtils.class,
                                                  "copy", new Class[]{}, true);
        TestClassReflectionUtils dstInstance = (TestClassReflectionUtils) ReflectionUtils.invokeMethod(m_copy,
                                                                                                       srcInstance,
                                                                                                       new Object[]{});

        Method m_isEqual = ReflectionUtils.getMethod(TestClassReflectionUtils.class, "isEqual",
                                                     new Class[]{ TestClassReflectionUtils.class }, true);
        Assert.assertTrue((boolean) ReflectionUtils.invokeMethod(m_isEqual, srcInstance, new Object[]{ dstInstance }));

        Assert.assertEquals("You cannot change me!",
                            ReflectionUtils.getFieldValue(srcInstance, "FINAL_STATIC_FIELD", false));
        Assert.assertEquals("You cannot change me!",
                            ReflectionUtils.getFieldValue(dstInstance, "FINAL_STATIC_FIELD", false));

        ReflectionUtils.setFieldValue(dstInstance, "FINAL_FIELD", "What u changed me?", true);
        Assert.assertEquals("What u changed me?",
                            ReflectionUtils.getFieldValue(dstInstance, "FINAL_FIELD", false));

        ReflectionUtils.setFieldValue(srcInstance, "staticField", "What u changed me?", true);
        Assert.assertEquals("What u changed me?",
                            ReflectionUtils.getFieldValue(dstInstance, "staticField", false));
        Assert.assertEquals("What u changed me?",
                            ReflectionUtils.getFieldValue(srcInstance, "staticField", false));

        ReflectionUtils.setFieldValue(dstInstance, "booleanField", true, true);

        m_isEqual = ReflectionUtils.getMethod(TestClassReflectionUtils.class, "isEqual",
                                              new Class[]{ TestClassReflectionUtils.class }, false);
        Assert.assertFalse((boolean) ReflectionUtils.invokeMethod(m_isEqual, srcInstance, new Object[]{ dstInstance }));

    }

}
