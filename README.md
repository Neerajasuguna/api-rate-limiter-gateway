
# 🚀 Distributed API Rate Limiter – Spring Boot + Redis + Lua

A **production-grade API Gateway style rate limiter** built with **Spring Boot**, **Redis**, and **Lua scripts**, implementing **three industry-standard algorithms**:

* **Sliding Window Log**
* **Token Bucket**
* **Sliding Window Counter**

Includes **user subscription plans**, **endpoint-specific rules**, **IP-based limits**, **global throttling**, **Prometheus metrics**, **structured logging (ELK-ready)**, and **rate-limit response headers** — similar to **Stripe, GitHub, Cloudflare, Kong**, and other modern API Gateways.

---

## 📚 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [How It Works (Deep Dive)](#how-it-works-deep-dive)
- [Rate Limiting Algorithms](#rate-limiting-algorithms)
- [Prometheus Metrics](#prometheus-metrics)
- [Rate Limit Response Headers](#rate-limit-response-headers)
- [User Plans](#user-plans)
- [Run Locally](#run-locally)
- [API Endpoints](#api-endpoints)
- [Future Enhancements](#future-enhancements)

---

## 📌 Overview

This project implements a **mini API Gateway inside Spring Boot**, intercepting all incoming requests and applying **layered distributed rate-limiting** powered by Redis.

The system enforces **four independent protections**:

1. **Endpoint-specific rate limits**
2. **Per-IP limits** (anti-bruteforce / anti-DDoS)
3. **Per-user limits** (Token Bucket)
4. **Global system-wide throttling**

All limits run **atomically** using Lua scripts inside Redis.

---

## ⭐ Key Features

* ✔ **3 distributed rate limiting algorithms**
* ✔ **Redis-backed atomic operations (Lua)**
* ✔ **Per-user, per-IP, per-endpoint, global limits**
* ✔ **User subscription plans (FREE, PRO, ENTERPRISE)**
* ✔ **Prometheus counters for monitoring**
* ✔ **Structured JSON logs with MDC**
* ✔ **Rate-limit response headers (GitHub-style)**
* ✔ **Interceptor-based API Gateway design**
* ✔ **Extendable configuration architecture**

---

## 🧰 Tech Stack

| Component         | Technology              |
| ----------------- | ----------------------- |
| Backend Framework | Spring Boot 3           |
| Cache / Store     | Redis (Docker)          |
| Scripting Engine  | Lua                     |
| Metrics           | Micrometer + Prometheus |
| Logging           | Logstash JSON (MDC)     |
| Gateway Mechanism | Spring MVC Interceptor  |

---

## 📁 Project Structure

```
src/main/java/com/org/gateway/api_rate_limiter
│
├── controller/                # Test endpoints
├── gateway/                   # RateLimitInterceptor + WebConfig
├── service/                   # SlidingWindow, TokenBucket, SlidingCounter
├── config/                    # User plans, endpoint rules, rate rules
│
src/main/resources/redis
├── sliding_window.lua
├── sliding_counter.lua
├── token_bucket.lua
```

---

## 🔍 How It Works (Deep Dive)

### **1️⃣ Every Request Goes Through Interceptor (API Gateway Layer)**

Flow:

```
Client → Interceptor → Controller → Response
```

The interceptor:

* Extracts metadata (IP, API key, method, path)
* Applies **four layers** of rate limiting
* Decides **allow or block (429)** before any controller runs

---

### **2️⃣ Request Metadata Extracted**

From each request we collect:

* IP address
* HTTP method (GET/POST)
* Request path
* API key → defaults to `"anonymous"`
* Unique request ID (MDC)

---

### **3️⃣ Four Independent Rate-Limit Checks**

#### **A) Endpoint-Specific Limits**

Example:

* `/orders` → 20 req / min
* `/payments` → 3 req / min

Some routes cost more → so they get tighter controls.

---

#### **B) Per-IP (Sliding Window Log)**

Prevents:

* Abuse
* Bots
* DoS spikes

Redis key:

```
ip:<ip_address>
```

Logs timestamps → counts within last X seconds.

---

#### **C) Per-User (Token Bucket)**

User plans:

| Plan       | Capacity | Refill Rate |
| ---------- | -------- | ----------- |
| FREE       | 10       | 1/sec       |
| PRO        | 50       | 5/sec       |
| ENTERPRISE | 200      | 20/sec      |

Great for:

* Burst traffic
* Fair usage enforcement
* API monetization

---

#### **D) Global Limit (Sliding Counter)**

Protects the **entire system** from overload.

Example:

```
1000 requests / minute globally
```

Uses dual-window calculation:

```
weighted(previous) + current < limit
```

---

### **4️⃣ Redis Lua Scripts for Atomic Decisions**

Why Lua?

* Prevents race conditions
* O(1) evaluation
* Safe for distributed systems
* Zero latency overhead

Each Lua script:

* Reads
* Updates
* Expires
* Returns allow/block

All inside a single atomic Redis transaction.

---

### **5️⃣ Response Includes Rate-Limit Headers**

Example:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1763457739
```

Similar to:

* GitHub
* Stripe
* Reddit
* Twitter API

---

### **6️⃣ Prometheus Metrics for Monitoring**

Metrics exported under:

```
/actuator/prometheus
```

Examples:

* `rate_limit_allowed_total`
* `rate_limit_block_ip_total`
* `rate_limit_block_user_total`
* `rate_limit_block_endpoint_total`
* `rate_limit_block_global_total`

Perfect for **Grafana dashboards**.

---

## 🧮 Rate Limiting Algorithms

Explanation of each algorithm:

### **Sliding Window Log**

* Stores timestamp of every request
* Removes expired timestamps
* Checks count within window
* Good for per-IP precision

---

### **Token Bucket**

* Bucket has capacity
* Refills at constant rate
* Request consumes 1 token
* Allows bursts

Used by **Stripe, Google Cloud**.

---

### **Sliding Counter**

* Two windows overlap
* Weighted calculation
* Used by **Nginx / Envoy / Cloudflare**

Great for global limits.

---

## 📡 Prometheus Metrics

Access metrics:

```
http://localhost:8081/actuator/prometheus
```

Example output:

```
rate_limit_allowed_total 128
rate_limit_block_ip_total 12
rate_limit_block_user_total 3
rate_limit_block_global_total 1
```

---

## 📝 Rate Limit Response Headers

Example:

```
HTTP/1.1 200 OK
X-RateLimit-Limit: 50
X-RateLimit-Remaining: 49
X-RateLimit-Reset: 1763457739
```

---

## 👤 User Plans

| Plan       | IP Limit | Bucket Capacity | Refill Rate | Global Limit |
| ---------- | -------- | --------------- | ----------- | ------------ |
| FREE       | 5/10s    | 10              | 1/s         | 100/min      |
| PRO        | 20/10s   | 50              | 5/s         | 500/min      |
| ENTERPRISE | 100/10s  | 200             | 20/s        | 2000/min     |

---

## ▶️ Run Locally

### **Start Redis**

```sh
docker run -d --name api-rate-redis -p 6379:6379 redis:7
```

### **Start Spring Boot**

```sh
mvn spring-boot:run
```

---

## 📌 API Endpoints

| Endpoint                        | Method | Description                        |
| ------------------------------- | ------ | ---------------------------------- |
| `/test`                         | GET    | Test endpoint with all rate limits |
| `/rate-limiter/sliding-log`     | GET    | Sliding Window Log demo            |
| `/rate-limiter/token-bucket`    | GET    | Token Bucket demo                  |
| `/rate-limiter/sliding-counter` | GET    | Sliding Counter demo               |
| `/rate-limiter/health`          | GET    | Health check                       |

---

## 🚧 Future Enhancements

* Dynamic limits from database
* Distributed tracing (Jaeger/Zipkin)
* JWT-based authentication
* Admin dashboard for monitoring usage
* API key provisioning portal

---
