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
package com.axway.ats.uiengine.elements.html;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Cookie {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z");

    private String                        name;
    private String                        value;
    private String                        domain;
    private String                        path;
    private Date                          expiry;
    private boolean                       isSecure;

    /**
     * @param name the name of the cookie. May not be null or an empty string
     * @param value the cookie value. May not be null
     * @param domain the domain the cookie is visible to
     * @param path the path the cookie is visible to. If left blank or set to null, will be set to "/"
     * @param expiry the cookie's expiration date. May be null
     */
    public Cookie( String name,
                   String value,
                   String domain,
                   String path,
                   Date expiry,
                   boolean isSecure ) {

        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.expiry = expiry;
        this.isSecure = isSecure;
    }

    public String getName() {

        return name;
    }

    public String getValue() {

        return value;
    }

    public String getDomain() {

        return domain;
    }

    public String getPath() {

        return path;
    }

    public Date getExpiry() {

        return expiry;
    }

    public boolean isSecure() {

        return isSecure;
    }

    /**
     * Two cookies are equal if the name and value match
     */
    @Override
    public boolean equals(
                           Object o ) {

        if (this == o) {
            return true;
        }
        if (! (o instanceof Cookie)) {
            return false;
        }

        Cookie cookie = (Cookie) o;

        if (!name.equals(cookie.name)) {
            return false;
        }
        return ! (value != null
                                ? !value.equals(cookie.value)
                                : cookie.value != null);
    }

    @Override
    public int hashCode() {

        return name.hashCode();
    }

    @Override
    public String toString() {

        return name + "=" + value + (expiry == null
                                                    ? ""
                                                    : "; expires=" + DATE_FORMATTER.format(expiry))
               + ("".equals(path)
                                  ? ""
                                  : "; path=" + path)
               + (domain == null
                                 ? ""
                                 : "; domain=" + domain)
               + (isSecure
                           ? ";secure;"
                           : "");
    }

}
