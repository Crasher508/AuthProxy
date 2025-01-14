package dev.crasher508.authproxy.utils;

import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.JSONValue;

import static org.cloudburstmc.protocol.common.util.Preconditions.checkArgument;

public class Json {

    public static JSONObject parseJSONObject(String string) {
        try {
            Object json = JSONValue.parseWithException(string);
            checkArgument(json instanceof JSONObject, "Expected JSON object");
            return (JSONObject) json;
        } catch (Exception exception) {
            return new JSONObject();
        }
    }

    public static JSONArray parseJSONArrayFromObject(JSONObject jsonObject, String key) {
        return (JSONArray) jsonObject.get(key);
    }

    public static JSONObject parseJSONObjectFromObject(JSONObject jsonObject, String key) {
        return (JSONObject) jsonObject.get(key);
    }

    public static JSONObject parseJSONObjectFromArray(JSONArray jsonObject, int key) {
        return (JSONObject) jsonObject.get(key);
    }

    public static String parseStringFromJSONObject(JSONObject jsonObject, String key) {
        return (String) jsonObject.get(key);
    }

    public static Integer parseIntegerFromJSONObject(JSONObject jsonObject, String key) {
        return (int) jsonObject.get(key);
    }

    public static Long parseLongFromJSONObject(JSONObject jsonObject, String key) {
        return (long) jsonObject.get(key);
    }

    public static boolean parseBooleanFromJSONObject(JSONObject jsonObject, String key) {
        return (boolean) jsonObject.get(key);
    }


    public static String parseStringFromJSONArray(JSONArray jsonObject, int key) {
        return (String) jsonObject.get(key);
    }
}