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
package com.axway.ats.agent.core.loading;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

public class ReversedDelegationClassLoader extends URLClassLoader {

    public ReversedDelegationClassLoader( URL[] urls,
                                          ClassLoader parent ) {

        super(urls, parent);
    }

    @Override
    public void addURL(
                        URL url ) {

        super.addURL(url);
    }

    @Override
    public Class<?> loadClass(
                               final String name ) throws ClassNotFoundException {

        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                //then try to load the class
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                //If still not found, then invoke the parent's loadClass 
                //in order to find the class.
                ClassLoader parent = getParent();

                if (parent != null) {
                    c = parent.loadClass(name);
                } else {
                    throw new ClassNotFoundException(name);
                }
            }
        }
        return c;
    }

    @Override
    public InputStream getResourceAsStream(
                                            String name ) {

        URL url = getResource(name);
        try {
            if (url != null) {
                URLConnection urlConnection = url.openConnection();
                urlConnection.setUseCaches(false);
                return urlConnection.getInputStream();
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }
}
