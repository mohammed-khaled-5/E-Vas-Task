# TCP Rate Limiter (Java + Redis)

Raw TCP rate limiter built with Java sockets and Redis. Clients send `REQUEST` over TCP; the server responds with `ALLOW` or `DENY` based on a per-client rate limit.


###  Client-Side Results

The following screenshot shows the output from the concurrent test client.

- **10 requests** were **ALLOWED**
- **5 requests** were **DENIED**
- Denied requests occurred after exceeding the configured rate limit

<img src="/screenshots/client.PNG" width="700" />


###  Server-Side Logs

The screenshot below shows the server logs during the same test execution.

- Each incoming client connection is logged
- Allowed requests are logged normally
- Denied requests are logged with the reason:  
  **`RATE_LIMIT_EXCEEDED`**
- This confirms that rate limiting is enforced consistently on the server side

<img src="/screenshots/server.PNG" width="700" />



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

### Start Redis on docker 
```bash
docker run --name redis-rate-limiter -p 6379:6379 -d redis

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

Run the concurrent client (15 clients; with default limit 10/min, expect 5 DENY):
```bash
mvn -q -DskipTests exec:java -Dexec.mainClass=com.example.ratelimiter.TestClientConcurrent
```

## Tests
Integration-style tests validate Redis-backed rate limiting behavior.

```bash
mvn test
```

Notes:
- Tests assume Redis is available at `localhost:6379`.
- If Redis is not running, the tests will be skipped.

## Rate Limiting Algorithm
Sliding window per client using a Redis sorted set:
1. Remove timestamps older than `now - window`
2. Count remaining timestamps
3. If count >= limit, deny
4. Otherwise, add the current timestamp and allow

This is atomic via a Redis Lua script to ensure correctness under concurrency.

## Project Structure
- `src/main/java/com/example/ratelimiter/server` - TCP server and client handler
- `src/main/java/com/example/ratelimiter/service` - Rate limiter logic using Redis
- `src/test/java/com/example/ratelimiter/service` - Unit + integration tests

