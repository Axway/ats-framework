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
package com.axway.ats.action.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.axway.ats.action.exceptions.JsonException;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;

/**
 * A parser for JSON REST body.
 * 
 * <br> <br> <b>Note:</b> Key paths are represented as the following:
 * <ul>
 * <li>"key1" - pointing to a root level element</li>
 * <li>"key1/key2" - pointing to an element under a root element</li>
 * <li>"key1/key2[3]" - pointing to an array element. Note the array is zero based, which means in this
 * example we are pointing to the 4th element</li>
 * </ul>
 * 
 * <br> <br> <b>Note:</b> Many of the supported methods return the instance of this object
 * which allows chaining the code like this:
 * <blockquote>new JSONText().add("name", "John").add("age", "20").add("sex", "Male");</blockquote>
 */
@PublicAtsApi
public class JsonText {

    private static final Logger  log                    = LogManager.getLogger(JsonText.class);

    private static final Pattern NAME_AND_INDEX_PATTERN = Pattern.compile("(.*)\\[(\\d*)\\]");

    private static final String  PATH_DELIMETER         = "/";

    private JSONObject           jsonObject;
    private JSONArray            jsonArray;
    private Object               javaObject;

    private final static int     ERROR_MSG_MAX_BYTES    = 200;

    /**
     * Generic constructor
     */
    @PublicAtsApi
    public JsonText() {

        this.jsonObject = new JSONObject();
    }

    /**
     * Constructor which accepts the text content
     * 
     * @param jsonText the content
     * @throws JsonException exception in case of an parsing error
     */
    @PublicAtsApi
    public JsonText( String jsonText ) throws JsonException {

        if (jsonText.startsWith("{")) {
            try {
                this.jsonObject = (JSONObject) new JSONParser().parse(jsonText);
            } catch (ParseException e) {
                throw new JsonException("Error creating JSON Object. " + getFirstBytes(jsonText), e);
            }
        } else if (jsonText.startsWith("[")) {
            try {
                this.jsonArray = (JSONArray) new JSONParser().parse(jsonText);
            } catch (ParseException e) {
                throw new JsonException("Error creating JSON Array. " + getFirstBytes(jsonText), e);
            }
        } else {
            throw new JsonException("Provided JSON text must start with '{' or '['. "
                                    + getFirstBytes(jsonText));
        }
    }

    private JsonText( Object object ) {

        if (object instanceof JSONObject) {
            this.jsonObject = (JSONObject) object;
        } else if (object instanceof JSONArray) {
            this.jsonArray = (JSONArray) object;
        } else {
            this.javaObject = object;
        }
    }

    private String getFirstBytes( String jsonText ) {

        int endIndex = jsonText.length();
        if (endIndex > ERROR_MSG_MAX_BYTES) {
            endIndex = ERROR_MSG_MAX_BYTES;

            return "The first " + endIndex + " JSON body characters are '" + jsonText.substring(0, endIndex)
                   + "'";
        } else {
            return "The JSON body content is '" + jsonText.substring(0, endIndex) + "'";
        }
    }

    /**
     * Add JSON key-value pair
     * 
     * @param keyPath key path
     * @param keyValue key value
     * @return this instance
     * @throws JsonException
     */
    @PublicAtsApi
    @SuppressWarnings( "unchecked")
    public JsonText add( String keyPath, Object keyValue ) throws JsonException {

        if (StringUtils.isNullOrEmpty(keyPath)) {
            throw new JsonException("Null/empty path is not allowed");
        }

        if (keyPath.contains(PATH_DELIMETER)) {
            throw new JsonException("'" + keyPath + "' contains the not allowed delimiter character '"
                                    + PATH_DELIMETER + "'");
        }

        if (keyValue != null && keyValue.getClass().isArray()) {
            throw new JsonException("Use the appropriate method to add array to '" + keyPath + "'");
        }

        if (keyValue instanceof JsonText) {
            keyValue = ((JsonText) keyValue).jsonObject;
        }

        this.jsonObject.put(keyPath, keyValue);
        return this;
    }

    /**
     * Add integer array of JSON key-value pairs
     * 
     * @param keyPath key path
     * @param keyValues the key values
     * @return this instance
     * @throws JsonException
     */
    @PublicAtsApi
    @SuppressWarnings( "unchecked")
    public JsonText addArray( String keyPath, int[] keyValues ) throws JsonException {

        JSONArray jsonArray = new JSONArray();
        for (int keyValue : keyValues) {
            jsonArray.add(keyValue);
        }

        this.jsonObject.put(keyPath, jsonArray);
        return this;
    }

    /**
     * Add float array of JSON key-value pairs
     * 
     * @param keyPath key path
     * @param keyValues the key values
     * @return this instance
     * @throws JsonException
     */
    @PublicAtsApi
    @SuppressWarnings( "unchecked")
    public JsonText addArray( String keyPath, float[] keyValues ) throws JsonException {

        JSONArray jsonArray = new JSONArray();
        for (float keyValue : keyValues) {
            jsonArray.add(keyValue);
        }

        this.jsonObject.put(keyPath, jsonArray);
        return this;
    }

    /**
     * Add Object(often String) array of JSON key-value pairs
     * 
     * @param keyPath key path
     * @param keyValues the key values
     * @return this instance
     * @throws JsonException
     */
    @PublicAtsApi
    @SuppressWarnings( "unchecked")
    public JsonText addArray( String keyPath, Object[] keyValues ) throws JsonException {

        List<Object> actualKeyValues = new ArrayList<Object>();
        for (int i = 0; i < keyValues.length; i++) {
            if (keyValues[i] instanceof JsonText) {
                actualKeyValues.add( ((JsonText) keyValues[i]).jsonObject);
            } else {
                actualKeyValues.add((Object) keyValues[i]);
            }
        }

        this.jsonObject.put(keyPath, actualKeyValues);
        return this;
    }

    /**
     * Remove a JSON key 
     * 
     * @param keyPath key path
     * @return this instance
     * @throws JsonException
     */
    @PublicAtsApi
    public JsonText remove( String keyPath ) throws JsonException {

        JsonText parentJsonText = getParentOf(keyPath);

        List<String> paths = new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER)));
        String lastTokenPath = paths.get(paths.size() - 1);

        Matcher m = NAME_AND_INDEX_PATTERN.matcher(lastTokenPath);
        if (m.find()) {
            // last token is pointing to array
            if (m.groupCount() < 2) {
                throw new JsonException("'" + lastTokenPath
                                        + "' does not specify an array in the valid way 'key_name[index_number]'");
            }

            String name = m.group(1); // name in the path token
            int index = getIndex(lastTokenPath, m); // index in the path token, for example "name[3]"

            if (parentJsonText.jsonObject != null) {
                List<?> array = (List<?>) parentJsonText.jsonObject.get(name);
                if (index >= array.size()) {
                    throw new JsonException("Cannot remove item at position " + (index + 1)
                                            + " as there are only " + array.size() + " items present");
                } else {
                    array.remove(index);
                }
            } else // if( parentJsonText.jsonObject != null ) 
            {
                parentJsonText.jsonArray.remove(index);
            }
        } else {
            // last token is pointing to object
            if (parentJsonText.jsonObject != null) {
                if (!parentJsonText.jsonObject.containsKey(lastTokenPath)) {
                    throw new JsonException("Cannot remove JSON item '" + keyPath
                                            + "' as it does not exist");
                } else {
                    parentJsonText.jsonObject.remove(lastTokenPath);
                }
            } else if (parentJsonText.jsonArray != null) {
                parentJsonText.jsonArray.remove(lastTokenPath);
            } else {
                throw new RuntimeException("Not implemented");
            }
        }
        return this;
    }

    /**
     * Replace a JSON key. It will throw an error if the keys does not exist prior to replacing.
     * 
     * @param keyPath key path
     * @param newKeyValue the new key value
     * @return this instance
     */
    @PublicAtsApi
    @SuppressWarnings( "unchecked")
    public JsonText replace( String keyPath, Object newKeyValue ) throws JsonException {

        JsonText parentJsonText = getParentOf(keyPath);

        List<String> paths = new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER)));
        String lastTokenPath = paths.get(paths.size() - 1);

        Matcher m = NAME_AND_INDEX_PATTERN.matcher(lastTokenPath);
        if (m.find()) {
            // last token is pointing to array
            if (m.groupCount() < 2) {
                throw new JsonException("'" + lastTokenPath
                                        + "' does not specify an array in the valid way 'key_name[index_number]'");
            }

            String name = m.group(1); // name in the path token
            int index = getIndex(lastTokenPath, m); // index in the path token, for example "name[3]"

            if (parentJsonText.jsonObject != null) {
                List<Object> array = (List<Object>) parentJsonText.jsonObject.get(name);
                if (index >= array.size()) {
                    throw new JsonException("Cannot replace JSON item at positin " + (index + 1)
                                            + " as there are only " + array.size() + " items present");
                } else {
                    array.set(index, newKeyValue);
                }
            } else // if( parentJsonText.jsonObject != null ) 
            {
                parentJsonText.jsonArray.set(index, newKeyValue);
            }
        } else {
            // last token is pointing to object
            if (parentJsonText.jsonObject != null) {
                if (!parentJsonText.jsonObject.containsKey(lastTokenPath)) {
                    throw new JsonException("Cannot replace JSON item '" + keyPath
                                            + "' as it does not exist");
                } else {
                    parentJsonText.jsonObject.put(lastTokenPath, newKeyValue);
                }
            } else {
                throw new RuntimeException("Not implemented");
            }
        }
        return this;
    }

    /**
     * @param keyPath the key path
     * @return JSON Text which is part of the initial JSON text
     */
    @PublicAtsApi
    public JsonText get( String keyPath ) {

        if (StringUtils.isNullOrEmpty(keyPath)) {
            throw new JsonException("Invalid json path '" + keyPath + "'");
        }

        return getInternalJson(new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER))));
    }

    /**
     * @param keyPath the key path
     * @return a String JSON value
     */
    @PublicAtsApi
    public String getString( String keyPath ) {

        Object object;
        if (StringUtils.isNullOrEmpty(keyPath)) {
            // return the root element
            object = this.toString();
        } else {
            object = getInternalJson(new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER)))).javaObject;
        }

        if (object == null) {
            throw new JsonException("'" + keyPath + "' is not a valid path");
        }

        if (! (object instanceof String)) {
            throw new JsonException("'" + keyPath + "' does not point to a String value:\n"
                                    + object.toString());
        }

        return (String) object;
    }

    /**
     * @param keyPath the key path
     * @return an Integer JSON value
     */
    @PublicAtsApi
    public int getInt( String keyPath ) {

        Object object;
        if (StringUtils.isNullOrEmpty(keyPath)) {
            // return the root element
            object = this.javaObject;
        } else {
            object = getInternalJson(new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER)))).javaObject;
        }

        if (object == null) {
            throw new JsonException("'" + keyPath + "' is not a valid path");
        }

        try {
            return Integer.parseInt(object.toString());
        } catch (NumberFormatException nfe) {
            throw new JsonException("'" + keyPath + "' does not point to a Integer value:\n"
                                    + object.toString());
        }
    }
    
    /**
     * @param keyPath the key path
     * @return a Long JSON value
     */
    @PublicAtsApi
    public long getLong( String keyPath ) {

        Object object;
        if (StringUtils.isNullOrEmpty(keyPath)) {
            // return the root element
            object = this.javaObject;
        } else {
            object = getInternalJson(new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER)))).javaObject;
        }

        if (object == null) {
            throw new JsonException("'" + keyPath + "' is not a valid path");
        }

        try {
            return Long.parseLong(object.toString());
        } catch (NumberFormatException nfe) {
            throw new JsonException("'" + keyPath + "' does not point to a Long value:\n"
                                    + object.toString());
        }
    }

    /**
     * @param keyPath the key path
     * @return a boolean JSON value
     */
    @PublicAtsApi
    public boolean getBoolean( String keyPath ) {

        Object object;
        if (StringUtils.isNullOrEmpty(keyPath)) {
            // return the root element
            object = this.javaObject;
        } else {
            object = getInternalJson(new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER)))).javaObject;
        }

        if (object == null) {
            throw new JsonException("'" + keyPath + "' is not a valid path");
        }

        return Boolean.parseBoolean(object.toString());
    }

    /**
     * @param keyPath the key path
     * @return a float JSON value
     */
    @PublicAtsApi
    public float getFloat( String keyPath ) {

        Object object;
        if (StringUtils.isNullOrEmpty(keyPath)) {
            // return the root element
            object = this.javaObject;
        } else {
            object = getInternalJson(new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER)))).javaObject;
        }

        if (object == null) {
            throw new JsonException("'" + keyPath + "' is not a valid path");
        }

        try {
            return Float.parseFloat(object.toString());
        } catch (NumberFormatException nfe) {
            throw new JsonException("'" + keyPath + "' does not point to a Float value:\n"
                                    + object.toString());
        }
    }

    /**
     * Returns an array of JSON values.
     * It makes sense to use it in cases like "company/employees[]/name" where the name of all employees will be returned.
     * 
     * @param keyPath the key path
     * @return the array of found values
     */
    @PublicAtsApi
    public JsonText[] getArray( String keyPath ) {

        List<JsonText> jsonResults = new ArrayList<>();

        parseInternalJson(jsonResults, new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER))));

        return jsonResults.toArray(new JsonText[jsonResults.size()]);
    }

    /**
     * @return true when the top level element is a JSON object and false when it is a JSON array 
     */
    @PublicAtsApi
    public boolean isTopLevelObject() {

        return this.jsonObject != null;
    }

    /**
     * @return the names of all top level elements
     */
    @PublicAtsApi
    @SuppressWarnings( "unchecked")
    public String[] getElementNames() {

        Set<String> keys = jsonObject.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    /**
     * Get the number of elements pointed with the key path.
     * If pointing to a JSON object - it will return the number of the sub-elements
     * If pointing to a JSON or simple java array - it will return the number of array sub-elements
     * 
     * @param keyPath the path 
     * @return the number of elements pointed with the key path
     */
    @PublicAtsApi
    public int getNumberOfElements( String keyPath ) {

        JsonText jsonText;
        if (StringUtils.isNullOrEmpty(keyPath)) {
            jsonText = this;
        } else {
            jsonText = get(keyPath);
        }

        if (jsonText == null) {
            log.warn("JSON object " + keyPath + " is empty!");
            return 0;
        } else if (jsonText.jsonObject != null) {
            return jsonText.jsonObject.size();
        } else if (jsonText.jsonArray != null) {
            return jsonText.jsonArray.size();
        } else if (jsonText.javaObject != null) {
            return 1;
        } else {
            // we assume this is a String, Integer or something similar
            log.warn("We expected to point to a JSON object or array. We will return 1 as number of its elements");
            return 1;
        }
    }

    private JsonText getParentOf( String keyPath ) {

        if (StringUtils.isNullOrEmpty(keyPath)) {
            throw new JsonException("Invalid json path '" + keyPath + "'");
        }

        List<String> paths = new ArrayList<>(Arrays.asList(keyPath.split(PATH_DELIMETER)));
        if (paths.size() == 1) {
            // there is only one token, check it is present in our JSON text as a top level element

            String lastTokenPath = paths.get(0);
            boolean pathIsPresent = false;
            if (jsonObject != null && jsonObject.containsKey(lastTokenPath)) {
                pathIsPresent = true;
            } else if (jsonArray != null) {

                Matcher m = NAME_AND_INDEX_PATTERN.matcher(lastTokenPath);
                if (m.find()) {
                    // last token is pointing to array
                    if (m.groupCount() < 2) {
                        throw new JsonException("'" + lastTokenPath
                                                + "' does not specify an array in the valid way 'key_name[index_number]'");
                    }

                    String name = m.group(1); // name in the path token
                    int index = getIndex(lastTokenPath, m); // index in the path token, for example "name[3]"
                    if (StringUtils.isNullOrEmpty(name) && index < jsonArray.size()) {
                        pathIsPresent = true;
                    }
                }
            }

            if (pathIsPresent) {
                return this;
            } else {
                throw new JsonException("'" + keyPath + "' is not a valid path");
            }
        } else

        {
            paths.remove(paths.size() - 1);
            return getInternalJson(new ArrayList<>(paths));
        }

    }

    /**
     * Return the internal entity available at the pointed position
     * 
     * @param pathTokens
     * @return
     */
    private JsonText getInternalJson( List<String> pathTokens ) {

        for (String path : pathTokens) {

            Matcher m = NAME_AND_INDEX_PATTERN.matcher(path);

            if (m.find()) {
                // path is pointing to array
                if (m.groupCount() < 2) {
                    throw new JsonException("'" + path
                                            + "' does not specify an array in the valid way 'key_name[index_number]'");
                }

                String name = m.group(1); // name in the path token
                int index = getIndex(path, m); // index in the path token, for example "name[3]"

                if (index == -1) {
                    // we have an array but no index is specified -> "[]"
                } else if (!StringUtils.isNullOrEmpty(name)) {
                    // pointing to JSON object
                    pathTokens.remove(0);

                    if (index >= 0) {
                        pathTokens.add(0, "[" + String.valueOf(index) + "]");
                    }
                    return new JsonText(jsonObject.get(name)).getInternalJson(pathTokens);
                } else {
                    // directly pointing to JSON array, for example "[3]"
                    pathTokens.remove(0);

                    if (pathTokens.size() > 0) {
                        return new JsonText(jsonArray.get(index)).getInternalJson(pathTokens);
                    } else {
                        if (index >= jsonArray.size()) {
                            throw new JsonException("Cannot remove item at positin " + (index + 1)
                                                    + " as there are only " + jsonArray.size()
                                                    + " items present");
                        } else {
                            return new JsonText(jsonArray.get(index));
                        }
                    }
                }
            } else {
                // path is pointing to object
                if (!jsonObject.containsKey(path)) {
                    throw new JsonException("'" + path + "' is not a valid path");
                }

                if (jsonObject.get(path) == null) {
                    // the value is null
                    return null;
                }

                JsonText jsonText = new JsonText(jsonObject.get(path));

                pathTokens.remove(0);
                if (pathTokens.size() > 0) {
                    jsonText = jsonText.getInternalJson(pathTokens);
                }
                return jsonText;
            }
        }

        return this;
    }

    /**
     * Inspects the JSON content and returns all matched entities into the 
     * provided input list
     * 
     * @param jsonResults the list with matched results
     * @param pathTokens the path to match
     */
    private void parseInternalJson( List<JsonText> jsonResults, List<String> pathTokens ) {

        List<String> _paths = new ArrayList<>(pathTokens);
        for (String path : pathTokens) {

            Matcher m = NAME_AND_INDEX_PATTERN.matcher(path);

            if (m.find()) {
                // path is pointing to array
                if (m.groupCount() < 2) {
                    throw new JsonException("'" + _paths
                                            + "' does not specify an array in the valid way 'key_name[index_number]'");
                }

                String name = m.group(1); // name in the path token
                int index = getIndex(path, m); // index in the path token, for example "name[3]"

                _paths.remove(0);
                if (index == -1) {
                    // we have an array but no index is specified -> "[]"
                    Object internalObject = jsonObject.get(name);
                    if (internalObject instanceof JSONArray) {
                        JSONArray internalJsonArray = (JSONArray) internalObject;
                        for (int i = 0; i < internalJsonArray.size(); i++) {
                            JsonText ooo = new JsonText(internalJsonArray.get(i));
                            if (_paths.size() == 0) {
                                // this is the path end, the last path token ends with "[]"
                                jsonResults.add(ooo);
                            } else {
                                // go deeper
                                ooo.parseInternalJson(jsonResults, _paths);
                            }
                        }

                        // we have cycled deeply into an array, do not go to the next path token
                        return;
                    } else {
                        throw new RuntimeException("Not implemented");
                    }
                } else if (!StringUtils.isNullOrEmpty(name)) {
                    // pointing to JSON object
                    if (index >= 0) {
                        _paths.add(0, "[" + String.valueOf(index) + "]");
                    }
                    new JsonText(jsonObject.get(name)).parseInternalJson(jsonResults, _paths);
                    return;
                } else {
                    // directly pointing to JSON array, for example "[3]"
                    if (_paths.size() == 0) {
                        // this is the path end
                        if (index >= jsonArray.size()) {
                            throw new JsonException("Cannot remove item at positin " + (index + 1)
                                                    + " as there are only " + jsonArray.size()
                                                    + " items present");
                        } else {
                            jsonResults.add(new JsonText(jsonArray.get(index)));
                            return;
                        }
                    } else {
                        // go deeper
                        new JsonText(jsonArray.get(index)).parseInternalJson(jsonResults, _paths);
                        return;
                    }
                }
            } else {
                // path is pointing to object
                if (!jsonObject.containsKey(path)) {
                    throw new JsonException("'" + _paths + "' is not a valid path");
                }

                if (jsonObject.get(path) == null) {
                    // the value is null
                    jsonResults.add(null);
                    return;
                }

                JsonText jsonText = new JsonText(jsonObject.get(path));

                _paths.remove(0);
                if (_paths.size() > 0) {
                    jsonText.parseInternalJson(jsonResults, _paths);
                } else {
                    jsonResults.add(jsonText);
                }
                return;
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    @PublicAtsApi
    public String toString() {

        if (jsonObject != null) {
            return jsonObject.toString();
        } else if (jsonArray != null) {
            return jsonArray.toString();
        } else if (javaObject != null) {
            return javaObject.toString();
        } else {
            // this is legal, we are pointing to a NULL value somewhere inside the JSON text
            return null;
        }
    }

    /**
     * @return the JSON as nicely formatted text
     */
    @PublicAtsApi
    public String toFormattedString() {

        final String INDENT_CHAR = "\t";
        String indent = "";

        boolean weAreInArray = false;
        char lastChar = 0;
        StringBuilder sb = new StringBuilder();

        String plain;
        if (jsonObject != null) {
            plain = jsonObject.toString();
        } else if (jsonArray != null) {
            plain = jsonArray.toString();
        } else {
            plain = javaObject.toString();
        }

        for (int i = 0; i < plain.length(); i++) {
            char ch = plain.charAt(i);

            if (ch == '[') {
                weAreInArray = true;
            } else if (ch == ']') {
                weAreInArray = false;
            }

            if (ch == ',') {
                sb.append(ch);
                if (!weAreInArray || lastChar < '0' || lastChar > '9') {
                    sb.append("\n");
                    sb.append(indent);
                }
            } else if (ch == '{' || ch == '[') {
                sb.append(ch);
                sb.append("\n");
                indent = indent + INDENT_CHAR;
                sb.append(indent);
            } else if (ch == '}' || ch == ']') {
                sb.append("\n");
                indent = indent.replaceFirst(INDENT_CHAR, "");
                sb.append(indent);
                sb.append(ch);
            } else {
                sb.append(ch);
            }

            lastChar = ch;
        }

        return sb.toString();
    }

    private int getIndex( String path, Matcher m ) {

        if (StringUtils.isNullOrEmpty(m.group(2))) {
            // we have an array but no index is specified -> "[]"
            return -1;
        }

        int index = -1;
        try {
            index = Integer.parseInt(m.group(2));
        } catch (NumberFormatException nfe) {
            throw new JsonException("Invalid index number in '" + path + "'");
        }

        if (index < 0) {
            throw new JsonException("Negative index number in '" + path + "'");
        }

        return index;
    }
}
