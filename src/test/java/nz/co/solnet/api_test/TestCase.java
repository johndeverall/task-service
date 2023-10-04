package nz.co.solnet.api_test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class TestCase {
    private List<Operation> operations;

    private String baseUrl;

    public TestCase(String baseUrl) {
        this.baseUrl = baseUrl;
        operations = new ArrayList<>();
    }

    public void addOperation(Operation.HttpVerb httpVerb, String url, String json, String[] parameters) {
        operations.add(new Operation(httpVerb, baseUrl + url, json, parameters));
    }

    public void addOperation(Operation.HttpVerb httpVerb, String url, String json) {
        operations.add(new Operation(httpVerb, baseUrl + url, json, null));
    }

    public void addOperation(Operation.HttpVerb httpVerb, String url, String[] parameters) {
        operations.add(new Operation(httpVerb, baseUrl + url, "", parameters));
    }

    public void addOperation(Operation.HttpVerb httpVerb, String url) {
        operations.add(new Operation(httpVerb, baseUrl + url, "", null));
    }

    public Results execute() throws IOException {
        // Execute the operations and return the results as an array
        Result[] results = new Result[operations.size()];
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);

            if (i > 0) { // we must have previous result
                Result previousResult = results[i - 1];
                operation.setParameterValues(previousResult);
            }
            // Execute the operation and store the result
            results[i] = executeOperation(operation);
        }
        return new Results(results);
    }

    private Result executeOperation(Operation operation) throws IOException {
        // Execute the operation and return the result
        // You can implement the logic here to perform the HTTP request and return the response

        URL url = new URL(operation.getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(operation.getHttpVerb().name());
        connection.setRequestProperty("Content-Type", "application/json");

        if (operation.getHttpVerb() == Operation.HttpVerb.GET) {
            connection.setDoOutput(false);
        } else {
            connection.setDoOutput(true);
            String jsonPayload = operation.getJson();
            if (jsonPayload != null && !jsonPayload.isBlank()) {
                connection.getOutputStream().write(jsonPayload.getBytes());
            }
        }

        if (connection.getResponseCode() == 404) {
            return new Result(connection.getResponseCode());
        } else if (connection.getResponseCode() == 500) {
            return new Result(connection.getResponseCode());
        } else {
            String responsePayload = readResponsePayload(connection);

            // find parameter values
            Map<String, String> parameterValues = new HashMap<>();
            if (operation.getParameters() != null) {
                for (String parameter : operation.getParameters()) {
                    JsonObject jsonObject = new Gson().fromJson(responsePayload, JsonObject.class);
                    String value = findParameterValue(jsonObject, parameter);
                    if (value != null) {
                        parameterValues.put(parameter, value);
                    }
                }
            }
            return new Result(connection.getResponseCode(), responsePayload, parameterValues);
        }
    }

    /*private String readResponsePayload(HttpURLConnection connection) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder responsePayload = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            responsePayload.append(line);
        }
        bufferedReader.close();
        return responsePayload.toString();
    }*/

    private String readResponsePayload(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        InputStreamReader inputStreamReader;
        if (responseCode >= 200 && responseCode < 300) {
            inputStreamReader = new InputStreamReader(connection.getInputStream());
        } else {
            inputStreamReader = new InputStreamReader(connection.getErrorStream());
        }

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder responsePayload = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            responsePayload.append(line);
        }
        bufferedReader.close();
        return responsePayload.toString();
    }


    /*
        Used for finding a parameter value in a JSON response payload so that parameters can be chained between requests.
     */
    private static String findParameterValue(JsonObject jsonObject, String parameterName) {
        for (String key : jsonObject.keySet()) {
            JsonElement element = jsonObject.get(key);

            if (key.equals(parameterName)) {
                return element.getAsString();
            }

            if (element.isJsonObject()) {
                String value = findParameterValue(element.getAsJsonObject(), parameterName);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    public static class Operation {

        public enum HttpVerb {
            GET, POST, PUT, DELETE
        }
        private HttpVerb httpVerb;
        private String json;

        private String url;

        private String[] parameters;

        public Operation(HttpVerb httpVerb, String url, String json, String[] parameters) {
            this.httpVerb = httpVerb;
            this.json = json;
            this.url = url;
            this.parameters = parameters;
        }

        public HttpVerb getHttpVerb() {
            return httpVerb;
        }

        public String getUrl() {
            return url;
        }

        public String getJson() {
            return json;
        }

        public String[] getParameters() {
            return parameters;
        }

        public void setParameterValues(Result result) {
            for (String parameter : result.getParameters()) {
                String value = result.getParameterValue(parameter);
                if (value != null) {
                    url = url.replace("{" + parameter + "}", value);
                }
            }
        }

        public String toString() {
            return "Operation: " + httpVerb + " " + url + " " + json + " " + parameters;
        }
    }

    public class Result {
        private int statusCode;
        private String json;
        private Map<String, String> parameterValues;

        public Result(int statusCode, String json, Map<String, String> parameterValues) {
            this.statusCode = statusCode;
            this.json = json;
            this.parameterValues = parameterValues;
        }

        public Result (int statusCode) {
            this.statusCode = statusCode;
            this.json = "";
            this.parameterValues = new HashMap<String, String>();
        }

        public String getJson() {
            return json;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getParameterValue(String parameterName) {
            if (parameterValues != null) {
                return parameterValues.get(parameterName);
            } else {
                return null;
            }
        }

        public String[] getParameters() {
            if (parameterValues != null) {
                return parameterValues.keySet().toArray(new String[0]);
            } else {
                return null;
            }
        }
    }

    public class Results implements Iterator<Result> {

        private final LinkedList<Result> results;

        public Results(Result[] results) {
            this.results = new LinkedList<>(Arrays.asList(results));
        }

        public Result next() {
            return results.poll();
        }

        public boolean hasNext() {
            return !results.isEmpty();
        }

        public Result getLast() {
            return results.getLast();
        }

    }
}
