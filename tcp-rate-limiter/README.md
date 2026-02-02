# TCP Rate Limiter (Java + Redis)

Raw TCP rate limiter built with Java sockets and Redis. Clients send `REQUEST` over TCP; the server responds with `ALLOW` or `DENY` based on a per-client rate limit.

## Features
- Raw TCP server using `ServerSocket`
- Concurrent client handling with a thread pool
- Redis-backed counters using `INCR` + `EXPIRE`
- Denied-request logging
- Configurable rate limits and Redis connection

## Requirements
- Java 17+ (or compatible)
- Maven 3.8+
- Redis running locally (default `localhost:6379`)

## Build
```bash
mvn clean package
```

This produces a runnable shaded JAR in `target/` with `TcpRateLimiterServer` as the entry point.

## Run the Server
```bash
java -jar target/tcp-rate-limiter-1.0-SNAPSHOT.jar
```

The server listens on port `9090` by default.

## Configuration
You can set configuration via environment variables or JVM system properties.

Environment variables:
- `RATE_LIMIT_MAX_REQUESTS` (default: `10`)
- `RATE_LIMIT_WINDOW_SECONDS` (default: `60`)
- `REDIS_HOST` (default: `localhost`)
- `REDIS_PORT` (default: `6379`)

System properties:
- `rate.maxRequests`
- `rate.windowSeconds`
- `redis.host`
- `redis.port`

Example (PowerShell):
```powershell
$env:RATE_LIMIT_MAX_REQUESTS="10"
$env:RATE_LIMIT_WINDOW_SECONDS="60"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
java -jar target/tcp-rate-limiter-1.0-SNAPSHOT.jar
```

Example (JVM properties):
```bash
java -Drate.maxRequests=10 -Drate.windowSeconds=60 -Dredis.host=localhost -Dredis.port=6379 -jar target/tcp-rate-limiter-1.0-SNAPSHOT.jar
```

## Test the Server Manually
You can use `nc` or `telnet` to send requests:

```bash
# netcat example
printf "REQUEST\n" | nc 127.0.0.1 9090
```

Expected response: `ALLOW` or `DENY`.

There is also a simple concurrent client in:
- `src/main/java/com/example/ratelimiter/TestClientConcurrent.java`

## Tests
Integration-style tests validate Redis-backed rate limiting behavior.

```bash
mvn test
```

Notes:
- Tests assume Redis is available at `localhost:6379`.
- If Redis is not running, the tests will be skipped.

## Rate Limiting Algorithm
Fixed window counter per client:
1. `INCR rate:<clientId>`
2. If the counter is `1`, set `EXPIRE rate:<clientId> <windowSeconds>`
3. Allow when `count <= maxRequests`, otherwise deny

This is simple and fast, with correctness under concurrency due to Redis atomic operations.

## Project Structure
- `src/main/java/com/example/ratelimiter/server` - TCP server and client handler
- `src/main/java/com/example/ratelimiter/service` - Rate limiter logic using Redis
- `src/test/java/com/example/ratelimiter/service` - Integration tests

