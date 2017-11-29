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
package com.axway.ats.core.filesystem.snapshot.matchers;

public abstract class SkipContentMatcher {

    public enum MATCH_TYPE {
        TEXT, CONTAINS_TEXT, REGEX;

        public boolean isPlainText() {

            return this == TEXT;
        }

        public boolean isContainingText() {

            return this == CONTAINS_TEXT;
        }

        public boolean isRegex() {

            return this == REGEX;
        }

        public String getDescription() {

            if( this == TEXT ) {
                return "exact text";
            } else if( this == CONTAINS_TEXT ) {
                return "contains text";
            } else {
                return "regullar expression";
            }
        }
    }

    public String directoryAlias;
    public String filePath;

    public SkipContentMatcher( String directoryAlias, String filePath ) {

        this.directoryAlias = directoryAlias;
        this.filePath = filePath;
    }

    public String getFilePath() {

        return filePath;
    }

    public void setFilePath( String filePath ) {

        this.filePath = filePath;
    }

    public String getDirectoryAlias() {

        return directoryAlias;
    }
}
