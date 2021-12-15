/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.action.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.exceptions.JsonException;
import com.axway.ats.action.exceptions.RestException;
import com.axway.ats.action.json.JsonText;

public class Test_JsonText extends BaseTest {

    /*      One layer JSON object
                 {
                    "name"        : "Acme 1006", 
                    "description" : "Acme Corporation is a fictional corporation in Road Runner/Wile E. Coyote cartoons", 
                    "email"       : "support@acmecorp.com", 
                    "phone"       : "+1 877-564-7700", 
                    "integer"     : 100,
                    "float"       : 100.2,
                    "null_object" : null,
                    "enabled" : false
                }
     */
    private static final String body1 = "{ \"name\" : \"Acme 1006\","
                                        + " \"description\" : \"Acme Corporation is a fictional corporation in Road Runner/Wile E. Coyote cartoons\","
                                        + " \"email\" : \"support@acmecorp.com\","
                                        + " \"phone\" : \"+1 877-564-7700\"," + " \"integer\" : 100,"
                                        + " \"float\" : 100.2," + " \"null_object\" : null,"
                                        + " \"enabled\" : false}";

    /*      Deep JSON object
                {
                    "lotto":{
                        "lottoId":5,
                        "winning-numbers":[2,45,34,23,7,5,3],
                        "winners":[
                            {
                                "winnerId":23,
                                "numbers":[2,45,34,23,3,5]
                            },{
                               "winnerId":54,
                               "numbers":[52,3,12,11,18,22]
                            }
                        ]
                    }
                }
    */
    private static final String body2 = "{ \"lotto\":{ \"lottoId\":5, \"winning-numbers\":[2,45,34,23,7,5,3], \"winners\":[ { \"winnerId\":23, \"numbers\":[2,45,34,23,3,5] },{ \"winnerId\":54, \"numbers\":[52,3,12,11,18,22] } ] } }";

    /*      JSON array on top
             [
                 {
                     "id":"9fc1f799-cb49-440e-ae68-a5dd1d1f8dd1",
                     "summary":"Test",
                     "name":"API Test ",
                     "type":"rest",
                     "uri":"https://localhost:8075/api/portal/v1.0/discovery/swagger/api/API+Test+"
                 },
                 {
                     "id":"87f64e54-4215-4ef1-bec0-a15013fdb202",
                     "summary":"This API allows us to search gor stuff",
                     "name":"Acme Search API",
                     "type":"rest",
                     "uri":"https://localhost:8075/api/portal/v1.0/discovery/swagger/api/Acme+Search+API"
                 },
                 {
                    "string"       : "+1 877-564-7700", 
                    "integer"     : 100,
                    "float"       : 100.2,
                    "null_object" : null,
                    "enabled" : false
                 }
             ]
    */
    private static final String body3 = "[ {\"name\":\"API Test \",\"summary\":\"Test\",\"id\":\"9fc1f799-cb49-440e-ae68-a5dd1d1f8dd1\",\"uri\":\"https://localhost:8075/api/portal/v1.0/discovery/swagger/api/API+Test+\",\"type\":\"rest\"},"
                                        + " {\"name\":\"Acme Search API\",\"summary\":\"This API allows us to search gor stuff\",\"id\":\"87f64e54-4215-4ef1-bec0-a15013fdb202\",\"uri\":\"https://localhost:8075/api/portal/v1.0/discovery/swagger/api/Acme+Search+API\",\"type\":\"rest\"},"
                                        + "{\"string\":\"+1 877-564-7700\",\"integer\"     : 100,\"float\"       : 100.2,\"null_object\" : null,\"enabled\" : false } ]";

    /*      JSON with mutiple nested arrays
            {
                "lotto":{
                    "lottoId":5,
                    "winning-numbers":[2,45,34,23,7,5,3],
                    "winners":[
                        {
                            "winnerId":23,
                            "numbers":[2,45,34,23,3,5],
                            "address":[
                                {
                                    "city":"Paris",
                                    "street":"Main"
                                },
                                {
                                    "city":"London",
                                    "street":"Glory"
                                }
                            ]
                        },{
                           "winnerId":54,
                           "numbers":[52,3,12,11,18,22],
                           "address":[
                                {
                                    "city":"Sofia",
                                    "street":"Vasil Levski"
                                },
                                {
                                    "city":"Plovdiv",
                                    "street":"Hristo Botev"
                                }
                            ]
                        }
                    ]
                }
            }
    */
    private static final String body4 = "{ \"lotto\":"
                                        + "{ \"lottoId\":5, \"winning-numbers\":[2,45,34,23,7,5,3], \"winners\":[ "
                                        + "{ \"winnerId\":23, \"numbers\":[2,45,34,23,3,5], \"address\":[ { \"city\":\"Paris\", \"street\":\"Main\" }, { \"city\":\"London\", \"street\":\"Glory\" } ] },"
                                        + "{ \"winnerId\":54, \"numbers\":[52,3,12,11,18,22], \"address\":[ { \"city\":\"Sofia\", \"street\":\"Vasil Levski\" }, { \"city\":\"Plovdiv\", \"street\":\"Hristo Botev\" } ] } ] } }";

    @Test
    public void constructJsonText() {

        JsonText json1 = new JsonText().add("name", "Acme 1006")
                                       .add("description",
                                            "Acme Corporation is a fictional corporation in Road Runner/Wile E. Coyote cartoons")
                                       .add("email", "support@acmecorp.com")
                                       .add("phone", "+1 877-564-7700")
                                       .add("integer", 100)
                                       .add("float", 100.2)
                                       .add("null_object", null)
                                       .add("enabled", false);
        assertEquals(new JsonText(body1).toString(), json1.toString());
    }

    @Test
    public void constructDeepJsonText() {

        JsonText json = new JsonText().add("lotto",
                                           new JsonText().add("lottoId", 5)
                                                         .addArray("winning-numbers",
                                                                   new int[]{ 2, 45, 34, 23, 7, 5, 3 })
                                                         .addArray("winners",
                                                                   new JsonText[]{ new JsonText().add("winnerId",
                                                                                                      23)
                                                                                                 .addArray("numbers",
                                                                                                           new int[]{ 2,
                                                                                                                      45,
                                                                                                                      34,
                                                                                                                      23,
                                                                                                                      3,
                                                                                                                      5 }),
                                                                                   new JsonText().add("winnerId",
                                                                                                      54)
                                                                                                 .addArray("numbers",
                                                                                                           new int[]{ 52,
                                                                                                                      3,
                                                                                                                      12,
                                                                                                                      11,
                                                                                                                      18,
                                                                                                                      22 }) }));
        assertEquals(new JsonText(body2).toString(), json.toString());
    }

    @Test
    public void scoptToInternalPart_topElementIsJsonObject() {

        // JSON Object
        JsonText json = new JsonText(body2);
        JsonText internalJson = json.get("lotto");
        assertEquals(json.get("lotto").toString(), internalJson.toString());

        // JSON Object -> JSON Array
        internalJson = json.get("lotto/winners");
        assertEquals("[{\"winnerId\":23,\"numbers\":[2,45,34,23,3,5]},{\"winnerId\":54,\"numbers\":[52,3,12,11,18,22]}]",
                     internalJson.toString());

        // JSON Object -> JSON Array -> JSON Object 
        internalJson = json.get("lotto/winners[1]");
        assertEquals("{\"winnerId\":54,\"numbers\":[52,3,12,11,18,22]}", internalJson.toString());

        // JSON Object -> JSON Array -> JSON Object -> JSON Array
        internalJson = json.get("lotto/winners[1]/numbers");
        assertEquals("[52,3,12,11,18,22]", internalJson.toString());

        // Getting a Number as JsonText fails
        try {
            json.get("lotto/winners[1]/numbers[3]");
        } catch (RestException e) {
            assertEquals("java.lang.Integer cannot be cast to org.json.JSONObject",
                         e.getCause().getMessage());
        }

    }

    //    @Test
    public void scoptToInternalPart_topElementIsJsonArray() {

        JsonText json = new JsonText(body3);
        JsonText internalJson;

        // JSON Object
        internalJson = json.get("[2]");
        assertEquals("{\"enabled\":false,\"null_object\":null,\"integer\":100,\"string\":\"+1 877-564-7700\",\"float\":100.2}",
                     internalJson.toString());

        // NULL Object
        internalJson = json.get("[2]/null_object");
        assertNull(internalJson);
    }

    @Test
    public void getAllValueTypes() {

        JsonText json1 = new JsonText(body1);
        assertEquals("Acme 1006", json1.getString("name"));
        assertEquals(false, json1.getBoolean("enabled"));
        assertEquals(100, json1.getInt("integer"));
        assertEquals(100.2, json1.getFloat("float"), 0.1);
        assertEquals(null, json1.get("null_object"));

        JsonText json2 = new JsonText(body2);
        assertEquals(5, json2.getInt("lotto/lottoId"));
        assertEquals(54, json2.getInt("lotto/winners[1]/winnerId"));
        assertEquals(11, json2.getInt("lotto/winners[1]/numbers[3]"));
        assertEquals(45, json2.getInt("lotto/winning-numbers[1]"));
    }

    @Test
    public void getAllValueTypesNegative() {

        JsonText json1 = new JsonText(body1);

        try {
            assertEquals(false, json1.getBoolean(""));
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path");
        }

        assertEquals(false, json1.getBoolean("name"));

        try {
            assertEquals(100, json1.getInt(null));
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path");
        }

        try {
            assertEquals(100, json1.getInt("name"));
            fail();
        } catch (JsonException e) {
            checkError(e, ".*does not point to a Integer value.*");
        }

        try {
            assertEquals(100.2, json1.getFloat(null), 0.1);
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path");
        }
    }

    @Test
    public void addArray_stringValues() {

        JsonText json = new JsonText().addArray("name", new String[]{ "one", "two", "three" });

        assertEquals("{\"name\":[\"one\",\"two\",\"three\"]}", json.toString());
    }

    @Test
    public void addArray_intValues() {

        JsonText json = new JsonText().addArray("name", new int[]{ 2, 45, 34, 23, 7, 5, 3 });

        assertEquals("{\"name\":[2,45,34,23,7,5,3]}", json.toString());
    }

    @Test
    public void addArray_floatValues() {

        JsonText json = new JsonText().addArray("name",
                                                new float[]{ 2.2f, 45.1f, 34.0f, 23.3f, 7f, 5f, 3f });

        assertEquals("{\"name\":[2.2,45.1,34.0,23.3,7.0,5.0,3.0]}", json.toString());
    }

    @Test
    public void replaceValue() {

        String keyPath = "name";
        JsonText jsonText1 = new JsonText(body1);

        assertEquals("Acme 1006", jsonText1.getString(keyPath));
        assertEquals("this name was changed",
                     jsonText1.replace(keyPath, "this name was changed").getString(keyPath));

        JsonText jsonText2 = new JsonText(body2);

        keyPath = "lotto/lottoId";
        assertEquals(5, jsonText2.getInt(keyPath));
        assertEquals(15, jsonText2.replace(keyPath, 15).getInt(keyPath));

        keyPath = "lotto/winners[1]/winnerId";
        assertEquals(54, jsonText2.getInt(keyPath));
        assertEquals(64, jsonText2.replace(keyPath, 64).getInt(keyPath));

        keyPath = "lotto/winners[1]/numbers[2]";
        assertEquals(12, jsonText2.getInt(keyPath));
        assertEquals(13, jsonText2.replace(keyPath, 13).getInt(keyPath));
    }

    @Test( expected = JsonException.class)
    public void replaceValue_notExistingPath() {

        new JsonText(body1).replace("name123", "this name was changed");
    }

    @Test( expected = JsonException.class)
    public void replaceValue_nullPath() {

        new JsonText(body1).replace(null, "this name was changed");
    }

    @Test
    public void removeValue() {

        String keyPath = "name";
        JsonText jsonText1 = new JsonText(body1);

        assertNotNull(jsonText1.get(keyPath));
        try {
            jsonText1.remove(keyPath).get(keyPath);
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        JsonText jsonText2 = new JsonText(body2);

        keyPath = "lotto/lottoId";
        assertNotNull(jsonText2.get(keyPath));
        try {
            assertNull(jsonText2.remove(keyPath).get(keyPath));
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        keyPath = "lotto/winners[1]/winnerId";
        assertNotNull(jsonText2.get(keyPath));
        try {
            assertNull(jsonText2.remove(keyPath).get(keyPath));
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        keyPath = "lotto/winners[1]/numbers[2]";
        assertEquals(12, jsonText2.getInt(keyPath));
        assertEquals(11, jsonText2.remove(keyPath).getInt(keyPath));
        assertEquals(18, jsonText2.remove(keyPath).getInt(keyPath));
        assertEquals(22, jsonText2.remove(keyPath).getInt(keyPath));
        try {
            assertEquals(18, jsonText2.remove(keyPath).getInt(keyPath));
            fail();
        } catch (JsonException e) {
            checkError(e, ".*Cannot remove item at positin 3 as there are only 2 items present.*");
        }

        try {
            new JsonText(body2).remove("lotto/winning-numbers[100]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*Cannot remove item at position 101 as there are only 7 items present.*");
        }

        try {
            new JsonText(body2).remove("lotto/non-existing-element");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*Cannot remove JSON item .* as it does not exist.*");
        }

        try {
            new JsonText(body2).remove("lotto/winning-numbers[XYZ]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*Cannot remove JSON item .* as it does not exist.*");
        }

        new JsonText(body3).remove("[0]/id");

        new JsonText(body3).remove("[0]");

        new JsonText(body2).remove("lotto/winners/numbers");

        new JsonText(body2).remove("lotto/winning-numbers");

        try {
            new JsonText(body3).remove("[0]/non-existing-element");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*Cannot remove JSON item .* as it does not exist.*");
        }

        try {
            new JsonText(body2).remove("[XYZ]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        try {
            new JsonText(body3).remove("[XYZ]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        try {
            new JsonText(body2).remove("[-10]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        try {
            new JsonText(body3).remove("[-10]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        try {
            new JsonText(body2).remove("[10000]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        try {
            new JsonText(body3).remove("[10000]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        try {
            new JsonText(body2).remove("not_existing[10000]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        try {
            new JsonText(body3).remove("not_existing[10000]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        try {
            new JsonText(body2).remove("not_existing[0]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }

        try {
            new JsonText(body3).remove("not_existing[0]");
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }
    }

    @Test( expected = JsonException.class)
    public void invalidArrayIndex() {

        new JsonText(body2).get("lotto/winners[not_a_number]");
    }

    @Test( expected = JsonException.class)
    public void nullPath() {

        new JsonText(body2).get(null);
    }

    @Test( expected = JsonException.class)
    public void emptyPath() {

        new JsonText(body2).get("");
    }

    @Test
    public void badInputTextToConstructor() {

        try {
            new JsonText("invalid json text");
            fail();
        } catch (JsonException e) {
            checkError(e, "Provided JSON text must start with.*");
        }
    }

    @Test
    public void add_badPath() {

        try {
            System.out.println(new JsonText().add("name/ada", "koko"));
            fail();
        } catch (JsonException e) {
            checkError(e, ".*contains the not allowed delimiter character.*");
        }
    }

    @Test
    public void add_nullPath() {

        try {
            System.out.println(new JsonText().add(null, "koko"));
            fail();
        } catch (JsonException e) {
            checkError(e, "Null/empty path is not allowed");
        }
    }

    @Test
    public void addArrayWhenNotAllowed() {

        try {
            new JsonText().add("name", new int[]{ 1, 2, 3 });
            fail();
        } catch (JsonException e) {
            checkError(e, "Use the appropriate addArray\\(\\) method to add array to .*");
        }
    }

    @Test
    public void topLevelArray_getAllValueTypes() {

        JsonText jsonText = new JsonText(body3);

        assertEquals("9fc1f799-cb49-440e-ae68-a5dd1d1f8dd1", jsonText.getString("[0]/id"));
        assertEquals(false, jsonText.getBoolean("[2]/enabled"));
        assertEquals(100, jsonText.getInt("[2]/integer"));
        assertEquals(100.2, jsonText.getFloat("[2]/float"), 0.1);
        assertEquals(null, jsonText.get("[2]/null_object"));
    }

    @Test
    public void topLevelArray_replaceValue() {

        JsonText jsonText = new JsonText(body3);

        String keyPath = "[1]/type";

        assertEquals("rest", jsonText.getString(keyPath));
        assertEquals("this value was changed",
                     jsonText.replace(keyPath, "this value was changed").getString(keyPath));
    }

    @Test
    public void topLevelArray_removeValue() {

        JsonText jsonText = new JsonText(body3);

        String keyPath = "[1]/type";
        assertNotNull(jsonText.get(keyPath));

        try {
            jsonText.remove(keyPath).getString(keyPath);
            fail();
        } catch (JsonException e) {
            checkError(e, ".*is not a valid path.*");
        }
    }

    @Test
    public void getNumberOfElementsInObject() {

        assertEquals(1, new JsonText(body1).getNumberOfElements("integer"));

        assertEquals(3, new JsonText(body2).getNumberOfElements("lotto"));

        assertEquals(2, new JsonText(body2).getNumberOfElements("lotto/winners"));

        assertEquals(7, new JsonText(body2).getNumberOfElements("lotto/winning-numbers"));

        assertEquals(1, new JsonText(body2).getNumberOfElements("lotto/winning-numbers[5]"));
    }

    @Test
    public void getNullElementsInObject() {

        assertEquals(0, new JsonText(body1).getNumberOfElements("null_object"));
    }

    @Test
    public void getNumberOfElementsInObject_badPath() {

        try {
            new JsonText(body1).getNumberOfElements("lotto");
        } catch (JsonException e) {
            checkError(e, "'lotto' is not a valid path");
        }
    }

    @Test
    public void getNumberOfElementsInArray() {

        assertEquals(0, new JsonText("{}").getNumberOfElements(""));
        assertEquals(0, new JsonText("[]").getNumberOfElements(""));

        assertEquals(3, new JsonText(body3).getNumberOfElements(""));
        assertEquals(3, new JsonText(body3).getNumberOfElements(null));

        assertEquals(5, new JsonText(body3).getNumberOfElements("[0]"));
        assertEquals(1, new JsonText(body3).getNumberOfElements("[0]/id"));
    }

    @Test
    public void iterateOverArrayElement() {

        //@formatter:off
        String[] expectedElementNames = new String[]{

                                                      "id",
                                                      "summary",
                                                      "name",
                                                      "type",
                                                      "uri",

                                                      "id",
                                                      "summary",
                                                      "name",
                                                      "type",
                                                      "uri",

                                                      "enabled",
                                                      "null_object",
                                                      "integer",
                                                      "string",
                                                      "float" };
        //@formatter:on

        JsonText jsonText1 = new JsonText(body3);
        assertTrue(!jsonText1.isTopLevelObject());
        assertEquals(3, jsonText1.getNumberOfElements(""));

        List<String> actualElementNames = new LinkedList<String>();
        for (int i = 0; i < jsonText1.getNumberOfElements(""); i++) {
            JsonText jsonText2 = jsonText1.get("[" + i + "]");

            for (String name : jsonText2.getElementNames()) {
                actualElementNames.add(name);
            }
        }
        assertTrue(compareJSon(Arrays.asList(expectedElementNames), actualElementNames));
    }

    private boolean compareJSon(
                                 List<String> expected,
                                 List<String> actual ) {

        for (String name : expected) {
            for (int i = 0; i < actual.size(); i++) {
                if (actual.get(i).trim().equals(name.trim())) {
                    actual.remove(i);
                    break;
                } else if (i == actual.size() - 1) {
                    return false;
                }
            }
        }
        return actual.isEmpty();
    }

    @Test
    public void iterateOverObjectElements() {

        JsonText jsonText1 = new JsonText(body2);
        assertTrue(jsonText1.isTopLevelObject());
        assertEquals(1, jsonText1.getNumberOfElements(""));

        for (String name1 : jsonText1.getElementNames()) {
            assertEquals("lotto", name1);

            JsonText jsonText2 = jsonText1.get(name1);
            assertTrue(jsonText2.isTopLevelObject());

            assertEquals(3, jsonText2.getNumberOfElements(""));
            String[] names = jsonText2.getElementNames();
            assertEquals("winners", names[0]);
            assertEquals("lottoId", names[1]);
            assertEquals("winning-numbers", names[2]);
        }
    }

    private void fail() {

        throw new RuntimeException("We should not get here");

    }

    private void checkError(
                             JsonException e,
                             String expectedMsg ) {

        if (!e.getMessage().matches("(?s)" + expectedMsg)) {
            throw new RuntimeException("Did not get the expected error message '" + expectedMsg + "'", e);
        }
    }

    //    @Test
    public void getFromArray() {

        JsonText[] jsons = new JsonText(body4).getArray("lotto/winners[]/winnerId");
        assertEquals(2, jsons.length);
        assertEquals(23, jsons[0].getInt(""));
        assertEquals(54, jsons[1].getInt(null));

        jsons = new JsonText(body4).getArray("lotto/winners[]");
        assertEquals(2, jsons.length);
        assertTrue(jsons[0].toString().contains("winnerId\":23"));
        assertTrue(jsons[0].toString().contains("city\":\"London"));
        assertTrue(jsons[1].toString().contains("winnerId\":54"));
        assertTrue(jsons[1].toString().contains("city\":\"Plovdiv"));

        jsons = new JsonText(body4).getArray("lotto/winners[]/numbers");
        assertEquals(2, jsons.length);
        assertEquals("[2,45,34,23,3,5]", jsons[0].getString(null));
        assertEquals("[52,3,12,11,18,22]", jsons[1].toString());

        jsons = new JsonText(body4).getArray("lotto/winners[]/numbers[]");
        assertEquals(12, jsons.length);
        assertEquals(2, jsons[0].getInt(null));
        assertEquals(5, jsons[5].getInt(null));
        assertEquals(52, jsons[6].getInt(null));
        assertEquals(22, jsons[11].getInt(null));

        jsons = new JsonText(body4).getArray("lotto/winners[]/numbers[3]");
        assertEquals(2, jsons.length);
        assertEquals(23, jsons[0].getInt(null));
        assertEquals(11, jsons[1].getInt(null));

        jsons = new JsonText(body4).getArray("lotto/winners[]/address");
        assertEquals(2, jsons.length);
        assertEquals("[{\"street\":\"Main\",\"city\":\"Paris\"},{\"street\":\"Glory\",\"city\":\"London\"}]",
                     jsons[0].toString());
        assertEquals("[{\"street\":\"Vasil Levski\",\"city\":\"Sofia\"},{\"street\":\"Hristo Botev\",\"city\":\"Plovdiv\"}]",
                     jsons[1].toString());

        jsons = new JsonText(body4).getArray("lotto/winners[]/address[]");
        assertEquals(4, jsons.length);
        assertEquals("{\"street\":\"Main\",\"city\":\"Paris\"}", jsons[0].toString());
        assertEquals("{\"street\":\"Glory\",\"city\":\"London\"}", jsons[1].toString());
        assertEquals("{\"street\":\"Vasil Levski\",\"city\":\"Sofia\"}", jsons[2].toString());
        assertEquals("{\"street\":\"Hristo Botev\",\"city\":\"Plovdiv\"}", jsons[3].toString());

        jsons = new JsonText(body4).getArray("lotto/winners[]/address[]/street");
        assertEquals(4, jsons.length);
        assertEquals("Main", jsons[0].getString(null));
        assertEquals("Glory", jsons[1].getString(null));
        assertEquals("Vasil Levski", jsons[2].getString(null));
        assertEquals("Hristo Botev", jsons[3].getString(null));

        jsons = new JsonText(body4).getArray("lotto/winners[]/address[1]/street");
        assertEquals(2, jsons.length);
        assertEquals("Glory", jsons[0].getString(""));
        assertEquals("Hristo Botev", jsons[1].getString(null));

        jsons = new JsonText(body4).getArray("lotto/winners[1]/address[1]/street");
        assertEquals(1, jsons.length);
        assertEquals("Hristo Botev", jsons[0].getString(""));

        jsons = new JsonText(body4).getArray("lotto/winners[1]/address[]/street");
        assertEquals(2, jsons.length);
        assertEquals("Vasil Levski", jsons[0].getString(""));
        assertEquals("Hristo Botev", jsons[1].getString(null));
    }
}
