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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.RbvException;

public class Test_MetaData extends BaseTest {

    @Test
    public void toStringRegularValue() throws RbvException {

        MetaData metaData = new MetaData();
        assertEquals( "{  }", metaData.toString() );

        metaData.putProperty( "test", "test_value" );
        metaData.putProperty( "test1", "test_value1" );

        String str = metaData.toString();
        str = str.substring( 1, str.length() - 1 );

        List<String> expected = new ArrayList<String>();
        expected.add( "test1 : test_value1" );
        expected.add( "test : test_value" );

        assertTrue( compareMixedLists( expected,
                                       new LinkedList<String>( Arrays.asList( str.split( "," ) ) ) ) );
    }

    private boolean compareMixedLists( List<String> expected, List<String> actual ) {

        for( String name : expected ) {
            for( int i = 0; i < actual.size(); i++ ) {
                if( actual.get( i ).trim().equals( name.trim() ) ) {
                    actual.remove( i );
                    break;
                } else if( i == actual.size() - 1 ) {
                    return false;
                }
            }
        }
        return actual.isEmpty();
    }

    @Test
    public void toStringNull() throws RbvException {

        MetaData metaData = new MetaData();

        metaData.putProperty( "test", null );
        assertEquals( "{ test : null }", metaData.toString() );
    }

    @Test
    public void putGetProperty() throws RbvException {

        MetaData metaData = new MetaData();
        metaData.putProperty( "test", "test_value" );
        assertEquals( "test_value", metaData.getProperty( "test" ) );
    }
}
