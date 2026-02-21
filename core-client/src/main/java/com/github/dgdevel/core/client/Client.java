package com.github.dgdevel.core.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Client {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar core-client-1.0.0.jar [--server <server-url>] <method-name> [param1] [param2] [...]");
            System.err.println("  --server <url>: Server URL (default: http://localhost:8080)");
            System.err.println("  method-name: JSON-RPC method to invoke (or 'schema' to list all methods)");
            System.err.println("  param1, param2, ...: Method parameters (as strings)");
            System.err.println("");
            System.err.println("Examples:");
            System.err.println("  java -jar core-client-1.0.0.jar generic/ping");
            System.err.println("  java -jar core-client-1.0.0.jar generic/setConfigValue app theme dark");
            System.err.println("  java -jar core-client-1.0.0.jar --server http://example.com:9090 generic/ping");
            System.err.println("  java -jar core-client-1.0.0.jar schema");
            System.exit(1);
        }

        try {
            String serverUrl = "http://localhost:8080";
            String method = null;
            List<String> paramList = new ArrayList<>();
            int i = 0;

            while (i < args.length) {
                if ("--server".equals(args[i])) {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: --server flag requires a URL argument");
                        System.exit(1);
                    }
                    serverUrl = args[i + 1];
                    i += 2;
                } else {
                    if (method == null) {
                        method = args[i];
                    } else {
                        paramList.add(args[i]);
                    }
                    i++;
                }
            }

            if (method == null) {
                System.err.println("Error: Method name is required");
                System.err.println("Use 'schema' to list available methods");
                System.exit(1);
            }

            if ("schema".equalsIgnoreCase(method)) {
                displaySchema(serverUrl);
                return;
            }

            String[] paramStrings = paramList.toArray(new String[0]);

            String schemaJson = fetchSchema(serverUrl);
            Schema schema = objectMapper.readValue(schemaJson, Schema.class);
            MethodDescriptor methodDescriptor = findMethod(schema, method);

            if (methodDescriptor == null) {
                System.err.println("Error: Method '" + method + "' not found in schema");
                System.err.println("Use 'schema' command to list available methods");
                System.exit(1);
            }

            Object[] params = convertParams(paramStrings, methodDescriptor);

            JsonRpcRequest request = new JsonRpcRequest(method, params);
            String requestJson = objectMapper.writeValueAsString(request);
            String responseJson = sendRequest(serverUrl, requestJson);
            Object response = objectMapper.readValue(responseJson, Object.class);

            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void displaySchema(String serverUrl) throws Exception {
        String schemaJson = fetchSchema(serverUrl);
        Schema schema = objectMapper.readValue(schemaJson, Schema.class);

        System.out.println("JSON-RPC Service Schema");
        System.out.println("Version: " + schema.version);
        System.out.println("Methods:");
        System.out.println();

        for (MethodDescriptor method : schema.methods) {
            System.out.println(method.name);
            System.out.println("  Description: " + method.description);
            if (method.params != null && !method.params.isEmpty()) {
                System.out.println("  Parameters:");
                for (ParamDescriptor param : method.params) {
                    System.out.printf("    %-20s %-15s %s%n",
                            param.name + ":",
                            param.type + (param.required ? " (required)" : ""),
                            param.description != null ? "- " + param.description : "");
                }
            } else {
                System.out.println("  Parameters: (none)");
            }
            System.out.println();
        }
    }

    private static String fetchSchema(String serverUrl) throws Exception {
        URL url = URI.create(serverUrl + "/schema").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != 200) {
            throw new Exception("Failed to fetch schema: " + connection.getResponseCode());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static MethodDescriptor findMethod(Schema schema, String methodName) {
        for (MethodDescriptor method : schema.methods) {
            if (method.name.equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    private static Object[] convertParams(String[] paramStrings, MethodDescriptor methodDescriptor) {
        List<Object> converted = new ArrayList<>();
        List<ParamDescriptor> paramDescriptors = methodDescriptor.params;

        for (int i = 0; i < paramStrings.length; i++) {
            String value = paramStrings[i];

            if (i < paramDescriptors.size()) {
                ParamDescriptor descriptor = paramDescriptors.get(i);
                converted.add(convertParam(value, descriptor.type));
            } else {
                converted.add(value);
            }
        }

        return converted.toArray();
    }

    private static Object convertParam(String value, String type) {
        try {
            switch (type) {
                case "number":
                    if (value.contains(".")) {
                        return Double.parseDouble(value);
                    } else {
                        return Long.parseLong(value);
                    }
                case "boolean":
                    return Boolean.parseBoolean(value);
                case "string":
                    return value;
                case "string/number":
                    try {
                        return Instant.ofEpochMilli(Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        return Instant.parse(value);
                    }
                case "array":
                    return objectMapper.readValue(value, List.class);
                case "object":
                    return objectMapper.readValue(value, Map.class);
                default:
                    return value;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert '" + value + "' to type " + type + ": " + e.getMessage(), e);
        }
    }

    private static String sendRequest(String serverUrl, String requestJson) throws Exception {
        URL url = URI.create(serverUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestJson.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        if (connection.getResponseCode() != 200) {
            throw new Exception("Request failed: " + connection.getResponseCode());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JsonRpcRequest {
        @JsonProperty("jsonrpc")
        private final String jsonrpc = "2.0";

        @JsonProperty("method")
        private final String method;

        @JsonProperty("params")
        private final Object[] params;

        @JsonProperty("id")
        private final Integer id = 1;

        public JsonRpcRequest(String method, Object[] params) {
            this.method = method;
            this.params = params;
        }

        public String getJsonrpc() {
            return jsonrpc;
        }

        public String getMethod() {
            return method;
        }

        public Object[] getParams() {
            return params;
        }

        public Integer getId() {
            return id;
        }
    }

    public static class Schema {
        @JsonProperty("jsonrpc")
        public String jsonrpc;

        @JsonProperty("title")
        public String title;

        @JsonProperty("version")
        public String version;

        @JsonProperty("methods")
        public List<MethodDescriptor> methods;
    }

    public static class MethodDescriptor {
        @JsonProperty("name")
        public String name;

        @JsonProperty("description")
        public String description;

        @JsonProperty("params")
        public List<ParamDescriptor> params;
    }

    public static class ParamDescriptor {
        @JsonProperty("name")
        public String name;

        @JsonProperty("type")
        public String type;

        @JsonProperty("required")
        public Boolean required;

        @JsonProperty("description")
        public String description;
    }
}
