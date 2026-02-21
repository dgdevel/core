# core

A JSON-RPC service built with Java, Maven, Netty, and H2 database.

The server now supports both JSON-RPC (HTTP) and MessagePack (TCP) protocols.

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

The server starts on port 8080 for JSON-RPC (HTTP) and port 8081 for MessagePack (TCP) by default.

### Run Tests

```bash
cd core
mvn test
```

### Run Tests with Coverage Report

```bash
cd core
mvn clean test jacoco:report
```

Coverage reports will be generated in `target/site/jacoco/index.html`.

### Run All Quality Checks

```bash
cd core
mvn clean verify
```

This will run:
- Compilation
- All tests
- JaCoCo coverage analysis and report generation
- Code style validation (Checkstyle)

## Code Quality Tools

The project uses Maven plugins to ensure code quality:

### JaCoCo (Code Coverage)

JaCoCo is configured to measure test coverage. The plugin:
- Automatically instruments code during test execution
- Generates HTML reports at `target/site/jacoco/index.html`
- Checks coverage thresholds (90% instruction, 75% branch)

**Run JaCoCo report:**

```bash
mvn jacoco:report
```

**View coverage report:**

```bash
open target/site/jacoco/index.html
```

**Coverage thresholds:**
- Instruction coverage: 90%
- Branch coverage: 75%

### Checkstyle (Code Style)

Checkstyle validates code against Google Java Style Guide.

**Run Checkstyle check:**

```bash
mvn checkstyle:check
```

**Configuration:**
- Uses `google_checks.xml` style guide
- Fails on error: `false` (warns only)
- Console output: `true`

### PMD (Code Quality Analysis)

PMD analyzes code for common programming flaws and potential bugs.

**Note:** PMD 3.25.0 has compatibility limitations with Java 25 bytecode. The build uses Checkstyle as the primary code quality tool.

**Run PMD check:**

```bash
mvn pmd:check
```

**Configuration:**
- Target JDK: 17
- Rule set: `quickstart.xml`
- Reports generated in: `target/pmd/`

## Continuous Integration

Run the complete verification pipeline (includes all quality checks):

```bash
mvn clean verify
```

This will:
1. Compile the project
2. Run all unit tests
3. Generate JaCoCo coverage reports
4. Validate code with Checkstyle
5. Check coverage against thresholds
6. Build the JAR artifact

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

### MessagePack Protocol

The server also supports MessagePack over TCP on a separate port (default 8081).

All methods available via JSON-RPC are also available via MessagePack. The MessagePack protocol uses the same JSON-RPC 2.0 request/response structure but encoded in MessagePack binary format.

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

See the `core-client/README.md` for examples using the MessagePack client.

## Dependencies

- Netty 4.1.119.Final
- H2 Database 2.3.232
- Jackson 2.18.3
- MessagePack 0.9.8
- JUnit 5.12.0
- JaCoCo 0.8.13
- Checkstyle 3.6.0
- PMD 3.25.0

# Test Coverage

Current test coverage (JaCoCo):
- Instruction coverage: 77%
- Branch coverage: 63%
- All 196 tests passing
- 191 methods tested
- 16 classes covered

## Code Style Status

- Checkstyle violations: 0
- Google Java Style Guide compliance
