package nz.co.solnet.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ConstraintViolation {
    private String message;
    private String propertyPath;
    private Object invalidValue;

    public ConstraintViolation(String message, StringBuilder originalJson, String invalidValue) {
        this.message = message;
        this.invalidValue = invalidValue;

        JsonObject json = new Gson().fromJson(originalJson.toString(), JsonObject.class);
        Map<String, String> elements = findPropertiesByValue(json, invalidValue);
        if (elements.size() > 0) {
            this.propertyPath = elements.keySet().iterator().next();
        }
    }

    public ConstraintViolation(String message, String propertyPath, String invalidValue) {
        this.message = message;
        this.propertyPath = propertyPath;
        this.invalidValue = invalidValue;
    }

    /**
     * Find the property name(s) based on the given value in a JSON document.
     *
     * @param json  The JSON document represented as a JsonObject.
     * @param value The value to search for.
     * @return A map of property names and their corresponding values if found, or an empty map if not found.
     */
    public static Map<String, String> findPropertiesByValue(JsonObject json, String value) {
        Map<String, String> properties = new HashMap<>();

        if (json == null || value == null) {
            return properties;
        }

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement element = entry.getValue();

            if (element.isJsonObject()) {
                Map<String, String> childProperties = findPropertiesByValue(element.getAsJsonObject(), value);
                properties.putAll(childProperties);
            } else if (element.isJsonPrimitive()) {
                String elementValue = element.getAsString();
                if (elementValue.equals(value)) {
                    properties.put(key, elementValue);
                }
            }
        }

        return properties;
    }

    public String getMessage() {
        return message;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }
}