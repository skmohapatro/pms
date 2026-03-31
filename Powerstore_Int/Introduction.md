# Introduction

Hi, I’m Sudhir Mohapatro. I have 16+ years of experience in software development and have worked on multiple full-stack projects. In my current role at Dell, I work on Dell AIOps, which monitors large-scale storage deployments. I design and build backend services for high-volume telemetry ingestion (objects, metrics, events, alerts). We use RabbitMQ to decouple producers and consumers and manage backpressure, a processing layer to enrich and aggregate telemetry, and a polyglot storage approach: Cassandra for write-heavy time-series data and PostgreSQL for relational, tenant, and metadata needs. On the UI side, I work with Angular, React, Node.js, and TypeScript to build visually appealing, interactive dashboards for monitoring and predicting storage infrastructure behavior.

On top of that, I’m responsible for production readiness: SLO-driven monitoring, failure-mode thinking (DLQs, retries, idempotency), and secure multi-tenant isolation.


## Explanation of the architecture choices (details)

### RabbitMQ: decouple producers/consumers + enforce backpressure

- **Decoupling**: producers (storage arrays / product gateways) and consumers (processors) do not call each other directly. Producers publish messages to queues/exchanges, and consumers pull from queues. This means:
  - Producers can keep sending even if consumers are temporarily slow.
  - Consumers can be scaled independently (increase replicas to catch up).
  - A consumer deploy/restart does not require coordinating with producers.

- **Backpressure**: the system prevents a slow downstream (processor / DB) from collapsing the entire pipeline.
  - **Queue buffering** absorbs bursts (temporary spikes in telemetry volume).
  - **Consumer prefetch / concurrency limits** keep each consumer from taking more work than it can process safely.
  - **Acknowledgements (ack)** ensure messages are removed only after successful processing; if a consumer dies mid-work, the message can be re-delivered (at-least-once).

### Processing layer: enrich + aggregate telemetry

- **Enrichment**: add context needed for analytics and UI, for example:
  - Map raw IDs to asset hierarchy (cluster → node → volume).
  - Attach tenant/customer context.
  - Normalize units and timestamps.

- **Aggregation**: compute rollups to make querying faster and cheaper:
  - Convert raw per-second/per-minute metrics into 5m/1h/day aggregates.
  - Pre-compute KPIs (latency p95/p99, capacity used %, error rates).

- **Why it matters**: raw telemetry is high volume and often not directly query-friendly. Enrichment makes it meaningful; aggregation makes reads fast for dashboards and reports.

### Polyglot storage: Cassandra + PostgreSQL

- **Cassandra (time-series, write-heavy)**
  - Best for very high ingest rates and large volumes of append-style telemetry.
  - Data modeling is optimized for known query patterns (usually “asset + time range”).
  - Works well with retention via TTL and wide-column/time-bucket designs.

- **PostgreSQL (relational + tenant/config/metadata)**
  - Best for relational data (tenant config, user preferences, asset metadata, schemas) that needs joins/constraints/transactions.
  - Strong consistency (ACID) where correctness matters more than raw write throughput.

- **Why not one database for everything?**
  - Using Cassandra for relational queries is painful and inefficient.
  - Using Postgres for extremely high-frequency time-series ingest can become expensive and harder to scale.
  - Splitting lets each datastore do what it’s best at, and the processing/API layer hides the complexity from consumers.

---

# Likely Follow-up Questions (with clear answers)

## 1) Role clarity / scope

### Q: What exactly do you own in this system?
**Answer**: I own backend services in the ingestion and processing path—designing and implementing consumers/processors, improving reliability and backpressure behavior, and ensuring data correctness across retries. I also contribute to production readiness: SLOs, monitoring/alerting, incident response, and multi-tenant safeguards.

### Q: What’s the hardest problem you solved recently?
**Answer**: Stabilizing our PCF → Kubernetes migration for ingestion services. The hard part wasn’t just deployment—it was ensuring consumer stability under load, controlling retries/backpressure, and preventing telemetry gaps while maintaining tenant isolation and correctness.

## 2) Architecture walkthrough

### Q: Can you walk me through the end-to-end flow?
**Answer**: Telemetry lands in the ingestion entry point, gets validated/normalized, and is sent through RabbitMQ to decouple producers and consumers. Processing services enrich and aggregate telemetry, generate KPIs/events/alerts, and store data in a polyglot model: Cassandra for high-volume time-series writes and Postgres for relational tenant/config/metadata. APIs then expose this data to dashboards, alerting, and analytics consumers with tenant context and access controls.

### Q: Why RabbitMQ? Why not Kafka?
**Answer**: RabbitMQ fits our current needs for flexible routing and decoupling with acknowledgements and operational simplicity. Kafka is great when long retention and replay are primary requirements. If we needed native replay/event sourcing across many consumers, Kafka becomes more attractive; for our current routing/backpressure/ops balance, RabbitMQ is the pragmatic choice.

### Q: What data goes to Cassandra vs Postgres?
**Answer**: Cassandra stores high-volume telemetry/time-series data where write throughput and scale are key, and queries are typically time-range + key-based. Postgres stores relational data—tenant config, metadata, relationships, and anything needing ACID transactions, constraints, or complex joins.

## 3) Scale & performance

### Q: How do you scale ingestion when volume spikes?
**Answer**: We let RabbitMQ absorb short bursts, then scale consumers horizontally and keep processing stateless. We cap concurrency with bounded thread pools/prefetch limits so we don’t overload downstream stores. We monitor lag/queue depth and scale based on SLO signals, not just CPU.

### Q: How do you avoid hot partitions in Cassandra?
**Answer**: We pick partition keys that spread writes—typically tenant + asset + time bucket. For hot assets, we add extra bucketing/salting to distribute load, and we avoid query patterns that require scanning many partitions.

## 4) Reliability / correctness

### Q: What delivery guarantees do you provide?
**Answer**: Primarily at-least-once delivery from the queue/consumer model. That means downstream writes must be idempotent or deduplicated. We also use DLQs for poison messages and track retry/redelivery counts to avoid infinite loops.

### Q: How do you handle retries safely?
**Answer**: Retries are bounded and jittered, and we retry only on transient failures. Permanent failures go to DLQ with alerting. We ensure processing is idempotent via deterministic keys/upserts/dedup windows so retries don’t double-apply effects.

## 5) Multi-tenancy

### Q: How do you ensure tenant isolation?
**Answer**: Tenant context is enforced at the API gateway and propagated through services. In storage, Cassandra data models include tenant identifiers in partitioning and Postgres can enforce row-level constraints/policies. We add audit logs with tenant context and test for cross-tenant access patterns.

## 6) PCF → Kubernetes migration

### Q: What changed during PCF → Kubernetes that caused issues?
**Answer**: The biggest differences were around service lifecycle and resource behavior—probe configuration, startup time, CPU throttling due to requests/limits, and how restarts affect consumer state and message acknowledgements.

### Q: What did you tune in Kubernetes?
**Answer**: Readiness/liveness probes (initial delay, thresholds), resource requests/limits, autoscaling signals, and consumer settings like prefetch/concurrency so pods remain stable and don’t amplify load during backlog drain.

## 7) Observability / debugging

### Q: What metrics do you monitor?
**Answer**: Queue depth and age (lag), consumer acks/re-deliveries, processing throughput, error rates, Cassandra/Postgres latency, API p95/p99 latency, and pod health (restarts, throttling).

### Q: How do you debug a production issue quickly?
**Answer**: I start with where the queue is building and which dependency is slow. Then I correlate logs/metrics/traces using correlation IDs. If needed, I use thread dumps/JFR for JVM services. Stabilize first (limit concurrency, scale, reroute to DLQ), then do root cause.

## 8) Leadership signals

### Q: How do you make design tradeoffs?
**Answer**: I anchor on requirements (latency, throughput, availability, correctness), then evaluate options with operational cost and failure modes. I document the decision and define success metrics and rollback triggers.

### Q: What are you looking for in your next role?
**Answer**: More end-to-end ownership across architecture and delivery—leading design decisions, improving reliability/performance, and mentoring—while building high-scale backend systems.
