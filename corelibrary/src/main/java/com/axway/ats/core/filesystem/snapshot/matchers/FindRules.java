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

import java.io.Serializable;

import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;

public class FindRules implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int NONE = 0;

	public static final int SEARCH_FILENAME_AS_REGEX = 0x01;

	public static final int SKIP_FILE_PATH = SEARCH_FILENAME_AS_REGEX << 1;
	public static final int SKIP_FILE_SIZE = SKIP_FILE_PATH << 1;
	public static final int SKIP_FILE_MODIFICATION_TIME = SKIP_FILE_SIZE << 1;
	public static final int SKIP_FILE_MD5_SUM = SKIP_FILE_MODIFICATION_TIME << 1;
	public static final int SKIP_FILE_PERMISSIONS = SKIP_FILE_MD5_SUM << 1;

	public static final int CHECK_FILE_SIZE = SKIP_FILE_PERMISSIONS << 1;
	public static final int CHECK_FILE_MODIFICATION_TIME = CHECK_FILE_SIZE << 1;
	public static final int CHECK_FILE_MD5_SUM = CHECK_FILE_MODIFICATION_TIME << 1;
	public static final int CHECK_FILE_PERMISSIONS = CHECK_FILE_MD5_SUM << 1;
	public static final int CHECK_FILE_ALL_ATTRIBUTES = CHECK_FILE_SIZE | CHECK_FILE_MODIFICATION_TIME
			| CHECK_FILE_MD5_SUM | CHECK_FILE_PERMISSIONS;

	private int rules = NONE;

    public FindRules( int... rules ) {

        for( int rule : rules ) {
            this.rules = this.rules | rule;
        }
    }

    /* SEARCH RULES */
    public boolean isSearchFilenameByRegex() {

        return ( this.rules & SEARCH_FILENAME_AS_REGEX ) > 0;
    }

    /* SKIP RULES */
    public boolean isSkipFilePath() {

        return ( this.rules & SKIP_FILE_PATH ) > 0;
    }

    public boolean isSkipFileSize() {

        return ( this.rules & SKIP_FILE_SIZE ) > 0;
    }

    public boolean isSkipFileModificationTime() {

        return ( this.rules & SKIP_FILE_MODIFICATION_TIME ) > 0;
    }

    public boolean isSkipFileMd5() {

        return ( this.rules & SKIP_FILE_MD5_SUM ) > 0;
    }

    public boolean isSkipFilePermissions() {

        return ( this.rules & SKIP_FILE_PERMISSIONS ) > 0;
    }

    /* CHECK RULES */
    public boolean isCheckFileSize() {

        return ( this.rules & CHECK_FILE_SIZE ) > 0;
    }

    public boolean isCheckFileModificationTime() {

        return ( this.rules & CHECK_FILE_MODIFICATION_TIME ) > 0;
    }

    public boolean isCheckFileMd5() {

        return ( this.rules & CHECK_FILE_MD5_SUM ) > 0;
    }

    public boolean isCheckFilePermissions() {

        return ( this.rules & CHECK_FILE_PERMISSIONS ) > 0;
    }

    public boolean hasCheckRule() {

        return isCheckFileSize() || isCheckFileModificationTime() || isCheckFileMd5()
               || isCheckFilePermissions();
    }

    public String getAsString() {

        StringBuilder sb = new StringBuilder();
        if( isSearchFilenameByRegex() ) {
            sb.append( "find_by_regex," );
        }
        if( isSkipFilePath() ) {
            sb.append( "skip_path," );
        }
        if( isSkipFileSize() ) {
            sb.append( "skip_size," );
        }
        if( isSkipFileModificationTime() ) {
            sb.append( "skip_mod_time," );
        }
        if( isSkipFileMd5() ) {
            sb.append( "skip_MD5," );
        }
        if( isSkipFilePermissions() ) {
            sb.append( "skip_permissions," );
        }
        return sb.substring( 0, sb.length() - 1 );
    }

    public static FindRules getFromString(
                                           String str ) {

        FindRules parsedRules = new FindRules( NONE );
        for( String token : str.split( "," ) ) {

            if( "find_by_regex".equals( token ) ) {
                parsedRules.rules |= SEARCH_FILENAME_AS_REGEX;
            } else if( "skip_path".equals( token ) ) {
                parsedRules.rules |= SKIP_FILE_PATH;
            } else if( "skip_size".equals( token ) ) {
                parsedRules.rules |= SKIP_FILE_SIZE;
            } else if( "skip_mod_time".equals( token ) ) {
                parsedRules.rules |= SKIP_FILE_MODIFICATION_TIME;
            } else if( "skip_MD5".equals( token ) ) {
                parsedRules.rules |= SKIP_FILE_MD5_SUM;
            } else if( "skip_permissions".equals( token ) ) {
                parsedRules.rules |= SKIP_FILE_PERMISSIONS;
            } else {
                throw new FileSystemSnapshotException( "Error parsing file compare rule: Uknown value '" + token + "'" );
            }
        }

        return parsedRules;
    }

    public int getRules() {

        return rules;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        if( isSearchFilenameByRegex() ) {
            sb.append( "SEARCH file path by regex," );
        }

        if( isSkipFilePath() ) {
            sb.append( "SKIP file path," );
        }
        if( isSkipFileSize() ) {
            sb.append( "SKIP file size," );
        }
        if( isSkipFileModificationTime() ) {
            sb.append( "SKIP file modification time," );
        }
        if( isSkipFileMd5() ) {
            sb.append( "SKIP file MD5," );
        }
        if( isSkipFilePermissions() ) {
            sb.append( "SKIP file permissions," );
        }

        if( isCheckFileSize() ) {
            sb.append( "CHECK file size," );
        }
        if( isCheckFileModificationTime() ) {
            sb.append( "CHECK file modification time," );
        }
        if( isCheckFileMd5() ) {
            sb.append( "CHECK file MD5," );
        }
        if( isCheckFilePermissions() ) {
            sb.append( "CHECK file permissions," );
        }

        if( sb.length() == 0 ) {
            return "";
        } else {
            return sb.substring( 0, sb.length() - 1 );
        }
    }
}
