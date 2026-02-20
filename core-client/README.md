# core-client

A command-line client for the core service, supporting both JSON-RPC (HTTP) and MessagePack (TCP) protocols.

## Requirements

- Java 25
- Maven 3.9+

## Building

```bash
cd core-client
mvn clean package
```

This creates an executable JAR at `target/core-client-1.0.0.jar`.

## Usage

### JSON-RPC Client

```bash
java -jar target/core-client-1.0.0.jar [--server <server-url>] <method-name> [param1] [param2] [...]
```

- `--server <url>`: Server URL (optional, defaults to `http://localhost:8080`)
- `method-name`: JSON-RPC method to invoke (or `schema` to list all methods)
- `param1, param2, ...`: Method parameters (as strings)

### MessagePack Client

```bash
java -jar target/core-client-1.0.0.jar --msgpack-host <host> [--msgpack-port <port>] <method-name> [param1] [param2] [...]
```

- `--msgpack-host <host>`: MessagePack server host (default: `localhost`)
- `--msgpack-port <port>`: MessagePack server port (default: `8081`)
- `method-name`: Method to invoke
- `param1, param2, ...`: Method parameters (as strings)

### Examples

List all available methods and their parameters:
```bash
java -jar target/core-client-1.0.0.jar schema
```

Call `generic/ping` (no parameters):
```bash
java -jar target/core-client-1.0.0.jar generic/ping
```

Call `generic/setConfigValue` with string parameters:
```bash
java -jar target/core-client-1.0.0.jar generic/setConfigValue app theme dark
```

Call `user/findById` with a number parameter:
```bash
java -jar target/core-client-1.0.0.jar user/findById 1
```

Call `auth/registerType` with boolean parameters:
```bash
java -jar target/core-client-1.0.0.jar auth/registerType password true true
```

Call with a custom server URL:
```bash
java -jar target/core-client-1.0.0.jar --server http://example.com:9090 generic/ping
```

### MessagePack Examples

Call `generic/ping` using MessagePack protocol:
```bash
java -jar target/core-client-1.0.0.jar --msgpack-host localhost generic/ping
```

Call `generic/setConfigValue` using MessagePack with custom port:
```bash
java -jar target/core-client-1.0.0.jar --msgpack-host localhost --msgpack-port 9091 generic/setConfigValue app theme dark
```

Connect to remote MessagePack server:
```bash
java -jar target/core-client-1.0.0.jar --msgpack-host example.com --msgpack-port 9091 generic/ping
```

## How It Works

### JSON-RPC Protocol

#### Schema Command (`schema`)

When you use the special `schema` command, the client:

1. Fetches the schema from the server's `/schema` endpoint
2. Displays all available methods with their descriptions and parameters in a human-readable format
3. Shows parameter names, types, required status, and descriptions

#### Method Invocation

For all other commands, the client:

1. Fetches the JSON schema from the server's `/schema` endpoint
2. Looks up the method in the schema to determine parameter types
3. String arguments are converted to the appropriate types:
   - `number` → `Long` or `Double`
   - `boolean` → `Boolean`
   - `string` → `String`
   - `string/number` → `Timestamp` (supports epoch milliseconds or ISO format)
   - `array` → JSON array parsed as `List`
   - `object` → JSON object parsed as `Map`
4. A JSON-RPC 2.0 request is constructed and sent to the server
5. The response is displayed in formatted JSON

### MessagePack Protocol

When using `--msgpack-host`, the client uses MessagePack protocol:

1. Constructs a MessagePack-encoded request with the format: `["2.0", method, [params], id]`
2. Opens a TCP connection to the MessagePack server
3. Sends the MessagePack-encoded request
4. Reads the MessagePack-encoded response
5. Decodes and displays the response in formatted JSON

**MessagePack Request Format:**
```
[
  "2.0",           // jsonrpc version
  "method/name",    // method name
  [param1, ...],   // parameters as array
  1                // request id
]
```

**MessagePack Response Format:**
```
[
  "2.0",           // jsonrpc version
  {...result},      // result (or nil on error)
  [code, msg] or nil,  // error array or nil if success
  1                // request id
]
```

All parameters are passed as strings and the server handles type conversion internally.

## Available Methods

See the server's `/schema` endpoint for a complete list of available methods and their parameters:

```bash
curl http://localhost:8080/schema
```

Or use the schema command:

```bash
java -jar target/core-client-1.0.0.jar schema
```

### Example Schema Output

```
JSON-RPC Service Schema
Version: 1.0.0
Methods:

generic/ping
  Description: Returns the current database timestamp
  Parameters: (none)

generic/setConfigValue
  Description: Sets a configuration value for a namespace and key
  Parameters:
    namespace:         string           (required) - The configuration namespace
    key:               string           (required) - The configuration key
    value:             string           (required) - The configuration value
```
