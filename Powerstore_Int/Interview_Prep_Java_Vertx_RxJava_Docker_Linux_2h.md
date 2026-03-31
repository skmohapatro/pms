# 2-Hour Interview Prep — Java on Linux + Vert.x + RxJava + Microservices + Docker

This is a focused, high-signal prep pack for the JD emphasis:

- Develop in **Java over Linux**
- **Vert.x** (reactive toolkit)
- **RxJava** (reactive streams, backpressure)
- **Microservices** (reliability, observability, data consistency)
- **Docker containers** (runtime, debugging, performance)

---

## 0) 2-Hour Timeboxed Plan (what to cover, in order)

### 0:00–0:10 — “Open with your story” (2-minute pitch + setup)
- What you build
- Scale (QPS / throughput)
- Your ownership (design, delivery, ops)
- Reliability habits (SLOs, backpressure, idempotency, DLQs)

### 0:10–0:35 — Vert.x essentials (what interviewers expect)
- Event loop model, non-blocking design
- Verticles, deployment model
- HTTP server + routing
- Worker verticles vs executeBlocking
- Vert.x Future/Promise + RxJava integration

### 0:35–0:55 — RxJava essentials
- Observable vs Flowable, Single/Maybe/Completable
- Backpressure: why and how
- Operators you must know + common pitfalls

### 0:55–1:30 — Microservices: reliability and data correctness
- Service boundaries and data ownership
- Consistency, sagas, idempotency
- Resilience patterns (timeouts, retries, circuit breakers)
- Observability (logs/metrics/traces)

### 1:30–1:55 — Docker + Linux + production debugging
- Container fundamentals
- JVM-in-container tuning
- Linux commands for diagnosing latency/CPU/memory

### 1:55–2:00 — Closing
- 30-second “Why this role”
- 2 questions to ask interviewer

---

## 0A) Vert.x + RxJava Essentials — Detailed Terms, Usage, and Real-World Examples

This section expands the two blocks in the plan:

- Vert.x: event loop, non-blocking design, verticles, deployment, HTTP routing, worker vs `executeBlocking`, Future/Promise, Rx integration
- RxJava: Observable/Flowable/Single/Maybe/Completable, backpressure, key operators, pitfalls

### 0A.1 Vert.x — Event loop model

#### Event loop (what it is)
- A small set of threads that handle a large number of concurrent connections by doing **non-blocking IO**.
- Each event loop thread should stay responsive; think “do a little work, then yield back”.

#### Why it matters
- If you block an event-loop thread (JDBC call, file read, long CPU loop), you stall many requests and create tail-latency spikes.

#### Example (mental model)
- Bad: `GET /product/{id}` handler calls a blocking DB query on event-loop thread.
- Good: handler calls an async client, or offloads blocking work to worker pool.

#### Real-world use case
- High-QPS APIs (catalog/product pages, telemetry ingestion) where you need high concurrency with predictable latency.

### 0A.2 Vert.x — Non-blocking design

#### Non-blocking (definition)
- A request handler should not wait synchronously for IO.
- It returns quickly and continues when the async callback/Future completes.

#### Usage pattern
- Validate input -> start async call -> compose results -> respond.

#### Real-world use case
- Fan-out requests: product page service calling catalog + pricing + inventory concurrently and composing results.

### 0A.3 Vert.x — Verticles and deployment model

#### Verticle (what it is)
- Deployable unit in Vert.x. A verticle typically owns:
  - an HTTP server
  - an event bus consumer
  - periodic timers

#### Standard vs worker verticle
- Standard verticle: runs on event loop threads.
- Worker verticle: runs on worker threads and can contain blocking operations.

#### Deployment model (what to say)
- You scale inside a JVM by increasing verticle instances.
- You scale across machines by running more container replicas.

#### Real-world use case
- Separate verticles for:
  - API ingress
  - background consumers (Kafka/RabbitMQ)
  - scheduled cleanup or compaction

### 0A.4 Vert.x — HTTP server + routing

#### HTTP server
- Vert.x provides an async HTTP server on top of Netty.

#### Router and handlers
- `Router` matches a request to a chain of handlers (auth -> validation -> business logic -> response).

#### Real-world use case
- API Gateway/BFF style service:
  - auth and rate limiting handler
  - request correlation ID injection
  - handler that fans out to other services (catalog/pricing/inventory)

### 0A.5 Vert.x — Worker verticles vs `executeBlocking`

#### Worker verticle (when to use)
- When a whole component is fundamentally blocking:
  - legacy JDBC
  - file system calls
  - certain SDKs without async APIs

#### `executeBlocking` (when to use)
- When you have **small, isolated** blocking sections within an otherwise async flow.

#### Key point: bounded concurrency
- Offloading blocking work must be bounded (pool size, queue limits), otherwise you just move the meltdown elsewhere.

#### Real-world use cases
- Worker verticle:
  - integrating with a legacy RDBMS driver in an older subsystem
- `executeBlocking`:
  - generating a PDF invoice
  - performing CPU-heavy encryption/compression for a small payload

### 0A.6 Vert.x — `Future<T>` and `Promise<T>`

#### What they mean
- `Promise<T>`: something you complete (`complete`/`fail`).
- `Future<T>`: read-only view of the eventual result.

#### Why interviewers care
- Composing Futures is how you keep request handlers non-blocking and readable.

#### Common operations (what to use when)
- `compose`: chain dependent async steps (A then B).
- `map`: transform a successful result.
- `recover`: fallback/translate failures.

#### Real-world use case
- Checkout flow orchestration:
  - validate cart -> reserve inventory -> create payment intent -> create order
  - with a `recover` path to release reservation on failure.

### 0A.7 Vert.x + RxJava integration (why you’d combine them)

#### Why combine
- Vert.x Futures are great for request/response async.
- RxJava is great for:
  - streams (events/messages)
  - batching
  - backpressure-aware pipelines

#### Real-world use case
- Consumer service reading from Kafka/RabbitMQ:
  - decode message -> enrich -> call downstream -> write DB
  - use RxJava to bound concurrency and apply backpressure under load.

---

### 0A.8 RxJava — Types (what to use and when)

#### `Single<T>`
- Use when exactly one result is expected.
- Use case: fetch product details by ID.

#### `Maybe<T>`
- Use when result can be absent.
- Use case: optional cache lookup (hit/miss).

#### `Completable`
- Use when you only care about success/failure.
- Use case: “publish event”, “invalidate cache”, “send notification”.

#### `Observable<T>`
- Stream without backpressure.
- Use case: UI events or low-volume streams where you control production rate.

#### `Flowable<T>`
- Stream with backpressure.
- Use case: high-throughput event ingestion pipelines.

### 0A.9 RxJava — Backpressure (why and how)

#### Backpressure (definition)
- Mechanism to ensure a fast producer doesn’t overwhelm a slow consumer.

#### What “good” sounds like in an interview
- “I prefer `Flowable` for ingestion pipelines and I bound concurrency to protect downstream dependencies.”

#### Real-world use case
- Telemetry/message processing:
  - If DB writes slow down, the pipeline should reduce in-flight work and avoid memory blow-ups.

#### Practical tactics
- Bound concurrency using `flatMap(..., maxConcurrency)`.
- Choose strategy:
  - buffer (with limits) when correctness requires keeping all events
  - drop/latest/sample when the business can tolerate approximation

### 0A.10 RxJava — Operators you must know (with usage)

#### `map`
- Transform item A into item B.
- Use case: decode JSON -> domain object.

#### `flatMap`
- Async fan-out / concurrency.
- Use case: for each message, call an HTTP service, then merge results.

#### `concatMap`
- Like `flatMap` but preserves order.
- Use case: applying ordered updates to the same entity/aggregate.

#### `zip` / `combineLatest`
- Combine multiple sources.
- Use case: combine pricing + inventory + catalog into a single response.

#### `retryWhen` / error handling
- Retrying transient failures with backoff.
- Use case: retry transient 5xx from downstream service (only if operation is idempotent).

### 0A.11 RxJava — Common pitfalls (say these proactively)
- Unbounded `flatMap` concurrency (can overload DB/HTTP downstream).
- `onBackpressureBuffer` without limits (memory risk).
- Using retries on non-idempotent operations (duplicate writes/orders).
- Not controlling schedulers (accidentally moving CPU-heavy work onto critical threads).

---

### 0A.12 A realistic “end-to-end” use case story (you can narrate)

**Scenario**: Order placement / ingestion pipeline under load.

- Vert.x HTTP handler receives request on event loop.
- Uses Futures to:
  - validate input
  - call inventory reservation
  - call payment intent
  - create order
- Publishes an event to Kafka.
- A consumer uses RxJava `Flowable` to process events:
  - bounds concurrency
  - applies backpressure
  - retries transient failures
  - routes poison messages to DLQ

This ties together Vert.x non-blocking IO + RxJava backpressure + microservice reliability.

## 1) 2-Minute Pitch (use this structure)

### 1.1 Template
- “I build **backend services** for high-volume ingestion / APIs.”
- “I’ve designed systems around **backpressure**, **async processing**, and **polyglot persistence**.”
- “I own **production readiness**: SLOs, monitoring, failure modes, retries/DLQs, and safe deployments.”
- “I’m comfortable with **reactive paradigms** and non-blocking IO, and I’ve shipped services in **containers on Linux**.”

### 1.2 Strong keywords to include naturally
- Event loop, non-blocking, bounded concurrency
- Backpressure
- Idempotency
- Timeout budgets
- Retries with jitter
- Circuit breaker
- Consumer lag
- Correlation IDs + distributed tracing
- Read/write separation

---

## 2) Vert.x Cheat Sheet (must-know)

## 2.1 Core mental model
- Vert.x uses an **event-loop** model (like Node.js) built on Netty.
- Rule: **never block the event loop** (no blocking DB calls, file IO, long CPU loops).
- Use:
  - async clients
  - worker verticles
  - `executeBlocking` for bounded blocking work

## 2.2 Building blocks
- **Verticle**: unit of deployment.
  - Standard verticle: runs on event loop.
  - Worker verticle: runs on worker pool for blocking operations.
- **Vertx instance**: event loop threads + worker pool.
- **EventBus**: messaging inside the application (request/reply, pub/sub).
- **Web**: `Router`, handlers, body parsing, auth.

## 2.3 Concurrency and thread model (what to say)
- Event loop threads are few (typically ~#cores).
- Each request handler must return quickly and chain async callbacks/futures.
- Blocking code should run on:
  - worker pool (bounded)
  - dedicated executor for CPU-heavy tasks

## 2.4 Futures/Promises
- Vert.x has `Future<T>` and `Promise<T>`.
- Pattern:
  - `Future` represents a computation result.
  - Compose futures rather than nesting callbacks.

Key operations:
- `compose` (flatMap)
- `map` (transform)
- `recover` (fallback on failure)

## 2.5 Worker verticle vs executeBlocking
- **Use worker verticle** when a whole component is blocking (e.g., legacy JDBC).
- **Use executeBlocking** for isolated blocking sections.

Talking point:
- Always ensure **bounded concurrency** when offloading blocking work.

## 2.6 Common “gotchas”
- Logging or JSON serialization can become hot-path CPU. Avoid per-request heavy formatting.
- Accidentally calling blocking DB driver from event loop.
- Large payload parsing on event loop.
- Not setting timeouts -> resource leaks.

---

## 3) RxJava Cheat Sheet (must-know)

## 3.1 Types (how to choose)
- `Single<T>`: exactly one item
- `Maybe<T>`: 0 or 1 item
- `Completable`: only success/failure
- `Observable<T>`: push-based stream (no backpressure)
- `Flowable<T>`: stream **with backpressure**

Interview-safe statement:
- “For streaming/high-throughput pipelines, I prefer **Flowable** to control backpressure.”

## 3.2 Backpressure (core explanation)
Backpressure is what prevents a fast producer from overwhelming a slow consumer.

Approaches:
- Use `Flowable` + request-based consumption.
- Control concurrency with `flatMap(..., maxConcurrency)`.
- Buffer/drop/sample depending on business correctness.

Operators to mention:
- `onBackpressureBuffer` (risk: memory)
- `onBackpressureDrop` / `onBackpressureLatest`

## 3.3 Operators to be fluent in
- Transform: `map`, `flatMap`, `concatMap`
- Filtering: `filter`, `distinct`, `take`, `debounce`
- Combining: `zip`, `merge`, `concat`
- Error handling: `onErrorReturn`, `retryWhen`
- Resource mgmt: `using`, `doFinally`

Rule of thumb:
- Use `concatMap` when ordering matters.
- Use `flatMap` when parallelism matters.

## 3.4 Schedulers (simple explanation)
- `Schedulers.io()` for blocking IO (unbounded; be careful)
- `Schedulers.computation()` for CPU-bound tasks
- In Vert.x, be deliberate: don’t accidentally move work off the event loop without controlling concurrency.

## 3.5 Common RxJava pitfalls (say these proactively)
- Unbounded `flatMap` concurrency (explodes downstream)
- `onBackpressureBuffer` without limits
- Forgetting to dispose subscriptions
- Using `Observable` for high-throughput when backpressure is needed

---

## 4) How Vert.x + RxJava fit together (talk track)

### 4.1 What interviewers want to hear
- Vert.x provides event loop + async primitives; RxJava provides stream composition and backpressure.
- Build request pipelines with bounded concurrency and explicit timeouts.

### 4.2 Example pipeline narrative (no code)
- HTTP request handler validates input
- Calls async clients (DB/cache) using futures
- Maps results to domain model
- Uses RxJava for streaming tasks (batch processing, event streams)
- Emits events to bus; downstream consumers update read models/indexes

---

## 5) Microservices System Design — Topics to be ready for

## 5.1 Service boundaries (what “good” looks like)
- Boundaries around:
  - domain ownership
  - data ownership
  - scaling characteristics

Interview one-liner:
- “Each service owns its data and publishes events; other services build read models if needed.”

## 5.2 Synchronous vs asynchronous communication
- Synchronous: simple, but couples latency/availability
- Async: decouples, enables retries/DLQ, but adds eventual consistency

Typical hybrid:
- Sync for user request/response
- Async for side-effects (notifications, indexing, analytics)

## 5.3 Reliability patterns (must mention)
- Timeouts everywhere (client + server)
- Retries with jitter (only for safe/idempotent operations)
- Circuit breakers
- Bulkheads (separate pools)
- Rate limiting / backpressure

## 5.4 Idempotency (key interview differentiator)
Where:
- Create order / create payment intent
- Message consumers

How:
- Client passes idempotency key
- Store `(key → result)` with TTL
- Ensure state transitions are safe under retries

## 5.5 Distributed transactions: Saga
- Orchestration (central coordinator) vs choreography (events)
- Compensating actions

Talk track:
- “I model the flow as a state machine, persist state transitions, and ensure each step is idempotent.”

## 5.6 Data and DB patterns
- Per-service DB
- Read models for query performance (CQRS light)
- Outbox pattern for reliable event publishing

Outbox in one line:
- “Write business state + event record in the same DB transaction, then a relay publishes to Kafka.”

## 5.7 Observability (have a crisp answer)
- Logs: structured JSON + correlation ID
- Metrics: latency, error rate, saturation, queue lag
- Traces: distributed tracing across services
- SLOs: define targets + error budgets

---

## 6) Docker + Linux — What to review

## 6.1 Docker fundamentals (talk track)
- Immutable image, ephemeral containers
- Stateless service + externalized state
- Health checks and graceful shutdown

## 6.2 Dockerfile best practices (quick list)
- Multi-stage build
- Small base image
- Run as non-root
- Pin versions
- Use `.dockerignore`

## 6.3 JVM in containers (common interview topic)
- Ensure JVM respects container limits.
- Set explicit memory behavior:
  - Use `-XX:MaxRAMPercentage` (or explicit `-Xms/-Xmx`)
- Watch for OOMKilled vs Java OOM.

## 6.4 Container debugging commands
- `docker ps`
- `docker logs -f <container>`
- `docker exec -it <container> sh`
- `docker stats`

---

## 7) Linux Quick Commands (for Java service debugging)

## 7.1 CPU/memory
- `top` / `htop`
- `free -m`
- `vmstat 1`
- `ps aux --sort=-%cpu | head`

## 7.2 Disk and IO
- `df -h`
- `du -sh * | sort -h`
- `iostat -xz 1` (if available)

## 7.3 Network
- `ss -lntp` (list listening ports)
- `ss -antp | head`
- `curl -v http://localhost:PORT/health`

## 7.4 Process/thread inspection
- `ps -ef | grep java`
- `jcmd <pid> VM.flags`
- `jcmd <pid> Thread.print` (fast thread dump)

If you only remember one: thread dump + CPU usage is often enough to diagnose event-loop blocking.

---

## 8) Interview Q&A (common questions + strong answers)

## 8.1 “Explain Vert.x architecture in 60 seconds.”
- Vert.x is a reactive toolkit built on Netty.
- Uses event loops for handling IO efficiently.
- Encourages non-blocking handlers; blocking work is moved to worker pools.
- Verticles are deployment units; scale horizontally by deploying more instances.

## 8.2 “What happens if you block the event loop?”
- Latency spikes for all requests on that loop.
- Timeouts, cascading failures.
- Fix: use async clients, worker pools, and bounded concurrency.

## 8.3 “Observable vs Flowable?”
- `Flowable` supports backpressure; `Observable` does not.
- For high-throughput streams, `Flowable` avoids overwhelming consumers.

## 8.4 “How do you handle backpressure end-to-end?”
- Bound concurrency at each stage.
- Use bounded queues.
- Apply timeouts + shedding (drop/latest) where acceptable.
- Use buffering only with limits.

## 8.5 “How do you ensure no duplicate processing?”
- Idempotency keys for write APIs.
- Idempotent consumers.
- Store processed-message IDs or maintain state machine transitions.

## 8.6 “How do you publish events reliably?”
- Outbox pattern or transactional publish.
- Retries + DLQ.
- Consumers must be idempotent.

## 8.7 “How do you operate microservices in production?”
- SLOs, dashboards, alerts.
- Tracing + correlation IDs.
- Capacity planning (p95/p99 latency).
- Safe rollout (canary), fast rollback.

---

## 9) Rapid Review Checklist (last 10 minutes)
- Vert.x:
  - event loop vs worker
  - futures compose/recover
  - never block event loop
- RxJava:
  - Flowable vs Observable
  - backpressure + maxConcurrency
- Microservices:
  - idempotency, saga, outbox
  - timeouts, retries, CB
  - observability
- Docker/Linux:
  - JVM memory flags
  - docker logs/exec/stats
  - basic Linux diagnostics

---

## 10) Questions to ask the interviewer (pick 2)
- “How do you enforce non-blocking behavior and backpressure across services?”
- “What are your top SLOs for the platform and how do you measure them?”
- “How do you handle schema/versioning and safe rollouts in a microservices environment?”
- “What’s the biggest reliability incident you’ve had recently, and what changed after?”
