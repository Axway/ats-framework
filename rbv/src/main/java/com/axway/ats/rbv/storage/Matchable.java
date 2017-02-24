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
package com.axway.ats.rbv.storage;

import java.util.List;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;

/**
 * This is the base interface that has to be implemented
 * by all classes which provide information used for matching (meta data)
 * 
 */
public interface Matchable {

    /**
     * Open the IMatchable for reading. You have to do that
     * before any other operation is performed.
     * 
     * @throws RbvStorageException 
     *  if the instance has already been opened
     *  or another error occured
     */
    public void open() throws RbvStorageException;

    /**
     * Close the IMatchable instance. Once closed, you'll have
     * to call open() agains
     * 
     * @throws RbvStorageException
     *  if the instance has not been opened
     *  or another error occured
     */
    public void close() throws RbvStorageException;

    /**
     * Get all the meta data for the current IMatchable instance
     * 
     * @return  List of MetaData instances
     * 
     * @throws RbvException
     *  if the instance has not been opened
     *  or another error occured
     */
    public List<MetaData> getAllMetaData() throws RbvException;

    /**
     * Get only the new meta data since the last poll
     * for the current IMatchable instance
     * 
     * @return  List of MetaData instances
     * 
     * @throws RbvException
     *  if the instance has not been opened
     *  or another error occured
     */
    public List<MetaData> getNewMetaData() throws RbvException;

    /**
     * Get the description of the IMatchable instance
     * 
     * @return  a string describing the particular instance
     */
    public String getDescription();

    /**
     * Get the count of all and the new meta data
     * 
     * @return  String representation of the all and the new meta data counts
     * 
     * @throws RbvStorageException
     */
    public String getMetaDataCounts() throws RbvStorageException;
}
