🚀 Distributed API Rate Limiter – Spring Boot + Redis + Lua

A production-grade API Gateway style rate limiter built using Spring Boot, Redis, and Lua scripts, implementing three industry-proven rate limiting algorithms:

Sliding Window Log

Token Bucket

Sliding Window Counter

Includes user plans, endpoint-specific rules, multi-layer protection, Prometheus metrics, structured logging, and rate-limit response headers — similar to Stripe, GitHub, Cloudflare, Kong, and API Gateways used in top companies.

📚 Table of Contents

Overview

Key Features

Tech Stack

Project Structure

How It Works (Deep Dive)

Rate Limiting Algorithms

Prometheus Metrics

Rate Limit Response Headers

User Plans

Run Locally

API Endpoints

Future Enhancements

📌 Overview

This project simulates a mini API Gateway entirely within Spring Boot by intercepting every incoming HTTP request, applying advanced rate-limiting rules, and enforcing distributed limits using Redis.

It protects APIs across four independent layers:

Endpoint-specific limits

Per-IP limits

Per-user limits

Global system limits

All decisions are executed atomically using Lua scripts, making this scalable and production-ready.

⭐ Key Features

✓ Three Rate Limiting Algorithms

✓ Redis-backed distributed limiter

✓ Atomic operations via Lua (no race conditions)

✓ Per-User, Per-IP, Per-Endpoint, and Global limits

✓ Subscription plans (FREE, PRO, ENTERPRISE)

✓ Prometheus counters for monitoring

✓ JSON structured logs with MDC

✓ Real-time rate-limit response headers

✓ Extendable API Gateway design

🧰 Tech Stack
Component	Technology
Backend Framework	Spring Boot 3
In-Memory Store	Redis (Docker)
Scripting Engine	Lua Scripts
Metrics	Micrometer + Prometheus
Logging Format	Logstash JSON
Gateway Mechanism	Spring Interceptor
📁 Project Structure
src/main/java/com/org/gateway/api_rate_limiter
│
├── controller/              → Test endpoints
├── gateway/                 → RateLimitInterceptor + WebConfig
├── service/                 → SlidingWindow, TokenBucket, SlidingCounter
├── config/                  → Rules, User Plans, Endpoint rules
│
src/main/resources/redis
├── sliding_window.lua
├── sliding_counter.lua
├── token_bucket.lua

🔍 How It Works (Deep Dive)

This project behaves like a minimal API Gateway, enforcing rate limits before any controller/business logic executes.

🛂 1. All Requests Pass Through Interceptor

Flow:

Client → RateLimitInterceptor → Controller → Response


The interceptor decides:

✔ Allow request
❌ Reject with 429 Too Many Requests

This mirrors real gateways like Kong, Zuul, and Cloudflare Workers.

🧵 2. Request Metadata Extracted

From each request, we extract:

IP Address

HTTP Method (GET, POST…)

Request Path

API Key (if present, else "anonymous")

Correlated request ID for logs

This metadata drives all rate limit checks.

🛡 3. Four Independent Layers of Rate Limiting

Each layer protects a different part of the system.

🔵 A) Endpoint-Specific Rate Limits

Some APIs are more expensive.

Example rules:

GET /orders → 20 req/min

POST /payments → 3 req/min

Configured in:

EndpointRateLimitRules


Only applies to matching endpoints.

🟣 B) Per-IP Rate Limit (Sliding Window Log)

Purpose: Prevent DDoS or brute-force from abusive clients.

Redis stores a sorted set of timestamps:

ip:<client_ip>


Allows X requests per Y seconds per IP.

🟢 C) Per-User Rate Limit (Token Bucket)

Purpose: Limit burst traffic while still allowing steady flow.

User subscription plans:

FREE → small bucket

PRO → larger bucket

ENTERPRISE → biggest bucket

Controls:

Capacity (burst)

Refill rate (constant tokens per second)

🟠 D) Global Rate Limit (Sliding Counter)

Purpose: Protect entire system from overload.

Example:

Allow 500 req/min globally


The Sliding Counter algorithm blends:

Previous window

Current window

Weighted average

Used by:

Nginx

Envoy

Istio

🔁 4. Atomic Lua Execution in Redis

Each algorithm uses a Lua script to ensure:

✔ Atomic operations
✔ No race conditions
✔ O(1) latency
✔ High throughput

Lua runs inside Redis, making decisions instantaneous.

📊 5. Decision Order

The request must pass all layers in order:

1. Endpoint limit
2. IP limit
3. User bucket limit
4. Global limit


If ANY fails → return 429
If ALL pass → allow request

This layered protection is identical to real API Gateways.

📩 6. Response Includes Rate-Limit Headers

Every response includes:

X-RateLimit-Limit: <max>
X-RateLimit-Remaining: <remaining>
X-RateLimit-Reset: <epoch_seconds>


Used by:

GitHub API

Twitter API

Stripe

📈 7. Prometheus Metrics for Monitoring

Counters exposed:

rate_limit_allowed_total

rate_limit_block_ip_total

rate_limit_block_user_total

rate_limit_block_endpoint_total

rate_limit_block_global_total

Perfect for:

Grafana dashboards

Alerts

SRE visibility

📜 8. Structured JSON Logs (ELK-ready)

Using Logstash encoder, logs include:

method

path

ip

apiKey

request_id

These logs are searchable, structured, cloud-ready.

🧮 Rate Limiting Algorithms
Sliding Window Log

Stores timestamps in Redis sorted set

Removes expired entries

Allows if count < limit
✔ Highly accurate
✔ Good for per-IP rules

Token Bucket

Perfect for burst control

Refills tokens every second

Subtracts 1 token per request
✔ Used by Google Cloud, Stripe

Sliding Window Counter

Approximate but efficient

Combines previous + current window
✔ Ideal for global limits

📡 Prometheus Metrics

Access metrics:

http://localhost:8081/actuator/prometheus


Example:

rate_limit_allowed_total 105
rate_limit_block_ip_total 12
rate_limit_block_user_total 3
rate_limit_block_global_total 1

📝 Rate Limit Response Headers

Example:

HTTP/1.1 200 OK
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1763457739

👤 User Plans
Plan	IP Limit	User Capacity	Refill Rate	Global Limit
FREE	5/10s	10	1/sec	100/min
PRO	20/10s	50	5/sec	500/min
ENTERPRISE	100/10s	200	20/sec	2000/min
▶️ Run Locally
Start Redis
docker run -d --name api-rate-redis -p 6379:6379 redis:7

Run Spring Boot
mvn spring-boot:run

📌 API Endpoints
Endpoint	Method	Description
/test	GET	Test route with all rate limits applied
/rate-limiter/sliding-log	GET	Sliding window log test
/rate-limiter/token-bucket	GET	Token bucket test
/rate-limiter/sliding-counter	GET	Sliding counter test
/rate-limiter/health	GET	Health check
🚧 Future Enhancements

Distributed tracing (Jaeger / Zipkin)

JWT-based user identity

Dynamic rate limits from database

Dashboard to visualize usage

Circuit breaker (Resilience4j)
