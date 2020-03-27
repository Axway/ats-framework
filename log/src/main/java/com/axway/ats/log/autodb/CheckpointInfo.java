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
package com.axway.ats.log.autodb;

/**
 * The info we need while processing a checkpoint
 */
public class CheckpointInfo {

    private String name;                // checkpoint name
    private int    checkpointSummaryId; // id from the checkpoint summary table
    private long   checkpointId;        // id from the checkpoint details table
    private long   startTimestamp;      // the checkpoint start time

    public CheckpointInfo() {

    }

    public CheckpointInfo( String name, int checkpointSummaryId, long checkpointId, long startTimestamp ) {

        this.name = name;
        this.checkpointSummaryId = checkpointSummaryId;
        this.checkpointId = checkpointId;
        this.startTimestamp = startTimestamp;
    }

    public String getName() {

        return name;
    }

    public int getCheckpointSummaryId() {

        return checkpointSummaryId;
    }

    public long getCheckpointId() {

        return checkpointId;
    }

    public long getStartTimestamp() {

        return startTimestamp;
    }

    public boolean isRunning() {

        return startTimestamp > 0;
    }

    /**
     * Hash code and equals currently only look for name property as this is the
     * important key for the set of each thread's running checkpoints.
     */
    @Override
    public int hashCode() {

        return (name == null)
                              ? 0
                              : name.hashCode();
    }

    /**
     * Hash code and equals currently only look for name property as this is the
     * important key for the set of each thread's running checkpoints.
     */
    @Override
    public boolean equals( Object obj ) {

        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CheckpointInfo other = (CheckpointInfo) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}
