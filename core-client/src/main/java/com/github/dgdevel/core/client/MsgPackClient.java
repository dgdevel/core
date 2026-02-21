package com.github.dgdevel.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ValueType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MsgPackClient {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar core-client-1.0.0.jar [--msgpack-host <host>] [--msgpack-port <port>] <method-name> [param1] [param2] [...]");
            System.err.println("  --msgpack-host <host>: MessagePack server host (default: localhost)");
            System.err.println("  --msgpack-port <port>: MessagePack server port (default: 8081)");
            System.err.println("  method-name: Method to invoke");
            System.err.println("  param1, param2, ...: Method parameters (as strings)");
            System.err.println("");
            System.err.println("Examples:");
            System.err.println("  java -jar core-client-1.0.0.jar generic/ping");
            System.err.println("  java -jar core-client-1.0.0.jar generic/setConfigValue app theme dark");
            System.err.println("  java -jar core-client-1.0.0.jar --msgpack-host example.com --msgpack-port 9091 generic/ping");
            System.exit(1);
        }

        try {
            String host = "localhost";
            int port = 8081;
            String method = null;
            List<String> paramList = new ArrayList<>();
            int i = 0;

            while (i < args.length) {
                if ("--msgpack-host".equals(args[i])) {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: --msgpack-host flag requires a host argument");
                        System.exit(1);
                    }
                    host = args[i + 1];
                    i += 2;
                } else if ("--msgpack-port".equals(args[i])) {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: --msgpack-port flag requires a port argument");
                        System.exit(1);
                    }
                    port = Integer.parseInt(args[i + 1]);
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
                System.exit(1);
            }

            Object[] params = paramList.toArray(new String[0]);
            Object response = sendRequest(host, port, method, params);

            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Object sendRequest(String host, int port, String method, Object[] params) throws Exception {
        try (Socket socket = new Socket(host, port);
             InputStream in = socket.getInputStream();
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            byte[] requestData = packRequest(method, params);

            out.write(requestData);
            out.flush();

            socket.setSoTimeout(5000);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            return unpackResponse(baos.toByteArray());
        }
    }

    private static byte[] packRequest(String method, Object[] params) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(out);
        try {
            packer.packArrayHeader(4);
            packer.packString("2.0");
            packer.packString(method);
            packer.packArrayHeader(params.length);
            for (Object param : params) {
                packValue(packer, param);
            }
            packer.packInt(1);
            packer.flush();
            return out.toByteArray();
        } finally {
            packer.close();
        }
    }

    private static void packValue(MessagePacker packer, Object value) throws Exception {
        if (value == null) {
            packer.packNil();
        } else if (value instanceof Boolean) {
            packer.packBoolean((Boolean) value);
        } else if (value instanceof Integer) {
            packer.packInt((Integer) value);
        } else if (value instanceof Long) {
            packer.packLong((Long) value);
        } else if (value instanceof Float) {
            packer.packFloat((Float) value);
        } else if (value instanceof Double) {
            packer.packDouble((Double) value);
        } else if (value instanceof String) {
            packer.packString((String) value);
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            packer.packArrayHeader(array.length);
            for (Object item : array) {
                packValue(packer, item);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            packer.packArrayHeader(list.size());
            for (Object item : list) {
                packValue(packer, item);
            }
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            packer.packMapHeader(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                packer.packString(entry.getKey().toString());
                packValue(packer, entry.getValue());
            }
        } else {
            packer.packString(value.toString());
        }
    }

    private static Object unpackResponse(byte[] data) throws Exception {
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new ByteArrayInputStream(data));
        try {
            int arraySize = unpacker.unpackArrayHeader();
            String jsonrpc = unpacker.unpackString();
            Object result = unpackValue(unpacker);
            Object error = unpackValue(unpacker);
            int id = unpacker.unpackInt();

            if (error == null) {
                return result;
            } else {
                return Map.of("error", error, "id", id);
            }
        } finally {
            unpacker.close();
        }
    }

    private static Object unpackValue(MessageUnpacker unpacker) throws Exception {
        ValueType valueType = unpacker.getNextFormat().getValueType();
        if (valueType == ValueType.NIL) {
            unpacker.unpackNil();
            return null;
        } else if (valueType == ValueType.BOOLEAN) {
            return unpacker.unpackBoolean();
        } else if (valueType == ValueType.INTEGER) {
            return unpacker.unpackLong();
        } else if (valueType == ValueType.FLOAT) {
            return unpacker.unpackDouble();
        } else if (valueType == ValueType.STRING) {
            return unpacker.unpackString();
        } else if (valueType == ValueType.BINARY) {
            int binSize = unpacker.unpackBinaryHeader();
            byte[] binData = new byte[binSize];
            unpacker.readPayload(binData);
            return binData;
        } else if (valueType == ValueType.ARRAY) {
            int arraySize = unpacker.unpackArrayHeader();
            Object[] array = new Object[arraySize];
            for (int i = 0; i < arraySize; i++) {
                array[i] = unpackValue(unpacker);
            }
            return array;
        } else if (valueType == ValueType.MAP) {
            int mapSize = unpacker.unpackMapHeader();
            Map<String, Object> map = new java.util.HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                String key = unpacker.unpackString();
                Object value = unpackValue(unpacker);
                map.put(key, value);
            }
            return map;
        } else {
            throw new IllegalArgumentException("Unsupported MessagePack format: " + valueType);
        }
    }
}
