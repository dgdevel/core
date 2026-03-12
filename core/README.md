# core

A JSON-RPC service built with Java, Maven, Netty, and H2 database.

## Requirements

- Java 25
- Maven 3.9+

## Quick Start

### Build and Run Server

```bash
cd core
mvn clean compile
mvn exec:java -Dexec.mainClass="com.github.dgdevel.core.server.Server"
```

The server starts on port 8080 for JSON-RPC (HTTP) by default.

## Usage

See [METHODS.md](METHODS.md) for a complete list of available methods and their documentation.

### API Schema

The server exposes a JSON schema endpoint that lists all available methods and their parameters:

```bash
curl http://localhost:8080/schema
```

### Example Request

```bash
curl -X POST http://localhost:8080 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"generic/ping","params":{},"id":1}'
```
