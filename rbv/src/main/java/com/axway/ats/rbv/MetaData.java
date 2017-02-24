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
package com.axway.ats.rbv;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.axway.ats.rbv.model.NoSuchMetaDataKeyException;
import com.axway.ats.rbv.model.RbvException;

public class MetaData {

    private HashMap<String, Object> properties;

    public MetaData() {

        properties = new HashMap<String, Object>();
    }

    public void putProperty(
                             String name,
                             Object value ) throws RbvException {

        properties.put( name, value );
    }

    public Object getProperty(
                               String name ) throws RbvException {

        if( !properties.containsKey( name ) ) {
            throw new NoSuchMetaDataKeyException( name );
        }
        return properties.get( name );
    }

    /**
     * @return the {@link Set} of keys identifying all of the properties in this {@link MetaData}
     */
    public Set<String> getKeys() {

        return this.properties.keySet();
    }

    @Override
    public String toString() {

        StringBuffer outputBuffer = new StringBuffer();
        Iterator<Entry<String, Object>> entryIterator = properties.entrySet().iterator();

        outputBuffer.append( "{ " );

        while( entryIterator.hasNext() ) {
            Entry<String, Object> entry = entryIterator.next();

            outputBuffer.append( entry.getKey() + " : " );

            Object value = entry.getValue();
            if( value != null ) {
                outputBuffer.append( entry.getValue().toString() );
            } else {
                outputBuffer.append( "null" );
            }

            if( entryIterator.hasNext() ) {
                outputBuffer.append( " , " );
            }
        }

        outputBuffer.append( " }" );

        return outputBuffer.toString();
    }

    @Override
    public boolean equals(
                           Object object ) {

        //it is ok to use instanceof here, as all subclasses of
        //MetaData should be based on the properties map
        if( object instanceof MetaData ) {
            MetaData metaData = ( MetaData ) object;
            return this.properties.equals( metaData.properties );
        }

        return false;
    }

    @Override
    public int hashCode() {

        return properties.hashCode();
    }
}
