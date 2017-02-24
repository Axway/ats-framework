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
package com.axway.ats.core.filesystem.snapshot;

import java.io.Serializable;

/**
 * The configuration values used when taking a snapshot.
 * It is serializable, because it is send to remote instances as well
 */
public class SnapshotConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean           checkModificationTime;
    private boolean           checkSize;
    private boolean           checkMD5;
    private boolean           checkPermissions;
    private boolean           supportHidden;

    public boolean isCheckModificationTime() {

        return checkModificationTime;
    }

    public void setCheckModificationTime(
                                          boolean checkModificationTime ) {

        this.checkModificationTime = checkModificationTime;
    }

    public boolean isCheckSize() {

        return checkSize;
    }

    public void setCheckSize(
                              boolean checkSize ) {

        this.checkSize = checkSize;
    }

    public boolean isCheckMD5() {

        return checkMD5;
    }

    public void setCheckMD5(
                             boolean checkMD5 ) {

        this.checkMD5 = checkMD5;
    }

    public boolean isCheckPermissions() {

        return checkPermissions;
    }

    public void setCheckPermissions(
                                     boolean checkPermissions ) {

        this.checkPermissions = checkPermissions;
    }

    public boolean isSupportHidden() {

        return supportHidden;
    }

    public void setSupportHidden(
                                  boolean supportHidden ) {

        this.supportHidden = supportHidden;
    }

}
