/*
 * Copyright 2020 Axway Software
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
package com.axway.ats.agent.core.monitoring.systemmonitor.systeminformation;

public interface IFileSystem {

    public enum Type {
        TYPE_UNKNOWN, TYPE_NONE, TYPE_LOCAL_DISK, TYPE_NETWORK, TYPE_RAM_DISK, TYPE_CDROM, TYPE_SWAP;

        public static Type fromInt( int value ) {

            for (Type type : Type.values()) {
                if (type.ordinal() == value) {
                    return type;
                }
            }
            return null;
        }
    }

    Type getType();

    String getDevName();

}
