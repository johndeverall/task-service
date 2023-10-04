package nz.co.solnet.api_test;

import au.com.origin.snapshots.comparators.v1.PlainTextEqualsComparator;
import au.com.origin.snapshots.Snapshot;
import com.google.gson.*;
import nz.co.solnet.api.tasks.GsonLocalDateAdapter;

import java.time.LocalDate;

/**
 * This class supports implementing ignorable parameters for snapshot testing.
 * Long term, this is probably a better approach to snapshot testing in java compared with writing a custom serializer using mixin annotations.
 * This is because testers writing test classes are more comfortable dealing with something like xpath or jsonpath values than mixins.
 * Providing xpath and jsonpath configuration for ignore values within the testcases themselves should be the long term goal here.
 */
public class IgnoreCapableComparator extends PlainTextEqualsComparator {

    private String[] propertiesToIgnore;

    public IgnoreCapableComparator(String[] propertiesToIgnore) {
        this.propertiesToIgnore = propertiesToIgnore;
    }

    public IgnoreCapableComparator() {
    }

    @Override
    public boolean matches(Snapshot previous, Snapshot current) {
        if (propertiesToIgnore != null) {

            Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new GsonLocalDateAdapter()).create();

            String previousBody = ignoreProperty(gson.fromJson(previous.getBody(), JsonElement.class), propertiesToIgnore);
            String currentBody = ignoreProperty(gson.fromJson(current.getBody(), JsonElement.class), propertiesToIgnore);

            return previousBody.equals(currentBody);
        } else {
            return previous.getBody().equals(current.getBody());
        }
    }

    private String ignoreProperty(JsonElement element, String[] propertiesToIgnore) {
        if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            for (String propertyToIgnore : propertiesToIgnore) {
                for (String propertyName : jsonObject.keySet()) {
                    if (propertyName.equals(propertyToIgnore)) {
                        jsonObject.addProperty(propertyName, "IGNORED");
                    }
                }
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(jsonElement -> ignoreProperty(jsonElement, propertiesToIgnore));
        }
        return element.toString();
    }
}