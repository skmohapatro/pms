# Dell AIOPS / CloudIQ — Design Document (Interview Ready)

## 1. Executive Summary

**CloudIQ** is Dell's AI-powered infrastructure monitoring and analytics platform. It ingests telemetry data (metrics, events, alerts, object changes) from Dell storage products (PowerStore, PowerScale, etc.), processes and enriches the data, stores it in a polyglot persistence layer, and exposes APIs for downstream consumers (dashboards, ML models, alerting systems).

**Key qualities**: high-throughput ingestion, multi-tenant isolation, schema-driven data evolution, near-real-time analytics.

---

## 2. Architecture Overview (Component Breakdown)

```
┌─────────────┐     ┌─────────────────────────┐     ┌───────────────────┐     ┌─────────────┐
│  Landing    │────▶│ CloudIQ Product Data    │────▶│    RabbitMQ       │────▶│  Generic    │
│  Zone       │     │ Processor               │     │ (Base telemetry)  │     │  Data       │
└─────────────┘     └─────────────────────────┘     └───────────────────┘     │  Processor  │
                              │                              │                └──────┬──────┘
                              ▼                              │                       │
                    ┌─────────────────┐                      │                       ▼
                    │  Schema Manager │                      │              ┌─────────────────┐
                    │  + Telemetry    │                      │              │  GDS (Cassandra)│
                    │  Metadata       │                      │              └─────────────────┘
                    └─────────────────┘                      │                       │
                                                             │                       ▼
                                                             │              ┌─────────────────┐
                                                             └─────────────▶│ CloudIQ Post    │
                                                                            │ Processing      │
                                                                            │ Consumer        │
                                                                            └────────┬────────┘
                                                                                     │
                    ┌────────────────────────────────────────────────────────────────┘
                    │
                    ▼
    ┌───────────────────────────────────────────────────────────────────────────────────┐
    │                              Storage & API Layer                                   │
    │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────────────┐   │
    │  │ Data API    │    │ Tenant      │    │ Data-API    │    │ Data API Access  │   │
    │  │ Base        │    │ Domain      │    │ (PostgreSQL)│    │ (Gateway)        │   │
    │  └─────────────┘    └─────────────┘    └─────────────┘    └──────────────────┘   │
    └───────────────────────────────────────────────────────────────────────────────────┘
                                              │
                                              ▼
                                    ┌───────────────────┐
                                    │ CloudIQ Data      │
                                    │ Consumers         │
                                    │ (Dashboards, ML,  │
                                    │  Alerts, Reports) │
                                    └───────────────────┘
```

### 2.1 Ingestion Layer

| Component | Responsibility |
|-----------|----------------|
| **Landing Zone** | Entry point for raw telemetry from Dell products. Acts as a buffer/staging area before processing. |
| **CloudIQ Product Data Processor** | Validates, normalizes, and routes incoming product data. Publishes schema definitions to Schema Manager. |
| **Schema Manager + Telemetry Metadata** | Central registry for telemetry schemas. Ensures producers/consumers agree on data contracts. Enables schema evolution without breaking downstream. |

**Data flow**: `Landing Zone → Product Data Processor → RabbitMQ`

### 2.2 Messaging Layer (RabbitMQ)

| What flows through RMQ | Description |
|------------------------|-------------|
| **Base asset telemetry** | Objects, metrics, events, alerts from storage arrays |
| **Any product data** | Flexible payload for new product integrations |

**Why RabbitMQ?**
- Supports multiple exchange types (direct, topic, fanout) for flexible routing
- Durable queues for reliability
- Consumer acknowledgments for at-least-once delivery
- Scales horizontally with clustering

### 2.3 Processing Layer

| Component | Responsibility |
|-----------|----------------|
| **Generic Data Processor** | Stateless transformer that enriches/aggregates raw telemetry. Writes to GDS (Cassandra). Emits Object Change Events. |
| **CloudIQ Post Processing Consumer** | Consumes post-processed KPIs, events, alerts. Applies business rules, anomaly detection, tenant-specific logic. Writes to Data API Base and Tenant Domain. |

**Processing patterns**:
- **Enrichment**: join with metadata (asset hierarchy, customer info)
- **Aggregation**: roll-up metrics (hourly, daily)
- **Alerting**: threshold checks, anomaly flags

### 2.4 Storage Layer (Polyglot Persistence)

| Store | Technology | Use Case |
|-------|------------|----------|
| **GDS** | Apache Cassandra | High-volume time-series metrics, events. Optimized for write-heavy, append-only workloads. Partitioned by asset + time. |
| **Data-API (PostgreSQL)** | PostgreSQL | Relational data: tenant config, asset metadata, user preferences. Supports complex queries, joins, transactions. |
| **Tenant Domain** | Logical isolation | Per-tenant data boundaries for multi-tenancy compliance (SOC2, GDPR). |

**Why this split?**
- Cassandra handles **scale** (millions of metrics/sec) with eventual consistency
- PostgreSQL handles **correctness** (ACID transactions, relational integrity)

### 2.5 API Layer

| Component | Responsibility |
|-----------|----------------|
| **Generic Data API** | Exposes Cassandra data (metrics, events) via REST/gRPC. Supports time-range queries, downsampling. |
| **Data API Access** | Unified gateway for CloudIQ Data Consumers. Handles auth, rate limiting, tenant context injection. |
| **Modeling Metadata** | Provides schema/model definitions to API consumers (self-describing APIs). |
| **Data API Base** | Exposes PostgreSQL data (config, metadata) via REST. |

### 2.6 Consumers

**CloudIQ Data Consumers** include:
- **Dashboards**: real-time and historical views
- **Alerting systems**: push notifications, integrations (ServiceNow, email)
- **ML pipelines**: anomaly detection, capacity forecasting
- **Reporting**: compliance, SLA reports

---

## 3. Key Design Decisions & Tradeoffs

### 3.1 RabbitMQ vs Kafka

| Aspect | RabbitMQ (chosen) | Kafka (alternative) |
|--------|-------------------|---------------------|
| **Delivery** | At-least-once with acks | At-least-once with offsets |
| **Ordering** | Per-queue FIFO | Per-partition ordering |
| **Replay** | Limited (requires DLQ) | Native replay from offset |
| **Ops complexity** | Moderate | Higher (ZooKeeper/KRaft) |

**Decision**: RabbitMQ chosen for simpler ops, flexible routing, and sufficient throughput for current scale. Kafka would be considered if replay/event sourcing becomes critical.

### 3.2 Cassandra vs TimescaleDB

| Aspect | Cassandra (chosen) | TimescaleDB |
|--------|--------------------| ------------|
| **Write throughput** | Excellent (LSM tree) | Good |
| **Query flexibility** | Limited (partition key required) | Full SQL |
| **Ops at scale** | Proven at PB scale | Growing adoption |

**Decision**: Cassandra chosen for write-heavy telemetry workload and proven scale. Query flexibility handled by pre-aggregating and caching hot paths.

### 3.3 Schema Evolution Strategy

- **Schema Manager** acts as a registry (similar to Confluent Schema Registry)
- **Backward compatible changes** (add optional fields) don't require consumer updates
- **Breaking changes** versioned; consumers migrate on their schedule
- **Telemetry Metadata** provides runtime schema lookups for dynamic deserialization

---

## 4. Non-Functional Requirements

| Requirement | Target | How achieved |
|-------------|--------|--------------|
| **Throughput** | 500K+ events/sec | Horizontal scaling of processors, Cassandra partitioning |
| **Latency (ingestion → queryable)** | < 30 sec p99 | Streaming processors, no batch windows |
| **Availability** | 99.9% | Multi-AZ deployment, RMQ mirroring, Cassandra RF=3 |
| **Multi-tenancy** | Strict isolation | Tenant Domain boundaries, row-level security in PG |
| **Data retention** | 90 days hot, 2 years cold | TTL in Cassandra, tiered storage |

---

## 5. Failure Modes & Mitigations

| Failure | Impact | Mitigation |
|---------|--------|------------|
| **RabbitMQ broker down** | Ingestion blocked | Clustered RMQ, quorum queues, producer retries with backoff |
| **Cassandra node failure** | Read/write degraded | RF=3, consistency level LOCAL_QUORUM, anti-entropy repair |
| **Processor crash** | Events pile up in queue | Stateless processors, auto-restart, consumer acks only after write |
| **Schema mismatch** | Deserialization errors | Schema validation at ingestion, DLQ for poison messages |
| **Tenant data leak** | Compliance violation | Tenant context in every request, audit logging, integration tests |

---

## 6. Observability Stack

- **Metrics**: Prometheus + Grafana (queue depth, processing lag, error rates)
- **Logs**: ELK or Splunk (structured JSON, correlation IDs)
- **Traces**: Jaeger/Zipkin (end-to-end latency, dependency mapping)
- **Alerts**: PagerDuty integration (SLO-based alerting)

**Key SLIs**:
- Ingestion lag (queue age)
- Processing error rate
- API latency p99
- Cassandra read/write latency

---

## 7. Interview Talking Points (How to Present This)

### Opening (30 sec)
> "I work on Dell CloudIQ, an AIOps platform that monitors Dell storage infrastructure. We ingest millions of telemetry events per second, process them in near-real-time, and expose APIs for dashboards, alerting, and ML-based anomaly detection."

### Architecture walkthrough (2 min)
> "The architecture has five layers: ingestion, messaging, processing, storage, and API.
> - **Ingestion**: Landing Zone receives raw telemetry; Product Data Processor validates and routes it.
> - **Messaging**: RabbitMQ decouples producers from consumers, handles backpressure. (Backpressure is like a traffic light for data - when consumers can't keep up, RabbitMQ signals producers to slow down, preventing system overload. It does this by monitoring queue lengths and using flow control mechanisms. Flow control works by RabbitMQ sending a "go-slow" signal to TCP connections when memory thresholds are exceeded, temporarily blocking network I/O until resources free up. Decoupling means producers don't need to know about consumers - they just send messages to RabbitMQ and move on, while consumers pull messages when ready. This allows independent scaling, failure isolation, and flexible consumer patterns.)
> - **Processing**: Generic Data Processor writes to Cassandra; Post Processing Consumer applies business rules and writes to PostgreSQL.
> - **Storage**: Cassandra for high-volume metrics (write-optimized), PostgreSQL for relational data (query-optimized).
> - **API**: Unified gateway with auth, rate limiting, tenant isolation."

### Tradeoff question (1 min)
> "We chose RabbitMQ over Kafka because our routing requirements are complex (topic-based fanout per product type), and we didn't need Kafka's replay semantics. If we later need event sourcing for audit, we'd reconsider."

### Scale question (1 min)
> "We handle 500K+ events/sec by horizontally scaling stateless processors and partitioning Cassandra by asset ID + time bucket. Hot partitions are avoided by salting partition keys for high-cardinality assets."

---

## 8. Deep-Dive Topics (Expect Follow-ups)

### 8.1 How does schema evolution work?

```
Producer (v2 schema)                    Consumer (v1 schema)
        │                                       │
        ▼                                       │
┌───────────────┐                               │
│ Schema Manager│◀──────────────────────────────┘
│ (registry)    │       (fetch schema on startup)
└───────┬───────┘
        │
        ▼
  Compatibility check:
  - v2 adds optional field? ✓ backward compatible
  - v2 removes required field? ✗ breaking change
```

**Key points**:
- Producers register schemas before publishing
- Consumers fetch schemas at startup + cache
- Breaking changes require versioned topics/queues

### 8.2 How do you handle backpressure?

1. **At RabbitMQ**: queue length limits, publisher confirms, flow control
2. **At processors**: bounded thread pools, reject excess with circuit breaker
3. **At API**: rate limiting per tenant, 429 responses, retry-after headers

### 8.3 How do you ensure tenant isolation?

- **Network**: tenant context injected at API gateway, propagated via headers
- **Storage**: PostgreSQL row-level security (RLS) policies, Cassandra partition includes tenant ID
- **Audit**: all data access logged with tenant context for compliance

---

# Behavioral Questions & Answers (STAR Format)

## Q1: Tell me about a time you owned a critical production incident.

**Situation**: During our **PCF → Kubernetes** migration for the CloudIQ ingestion pipeline (RabbitMQ consumers + processors), we saw intermittent **telemetry gaps** right after cutover. RabbitMQ queue depth kept growing and some dashboards showed missing metrics for a subset of tenants.

**Task**: As the on-call/driver for the migration weekend, I had to restore stable ingestion quickly, ensure **no tenant data leakage**, and bring us back under our ingestion-lag SLO (minutes-level) without a risky rollback.

**Action**:
1. **Triaged symptoms**: checked RabbitMQ management UI (queue depth/consumer acks), API lag dashboards, and Kubernetes signals (pod restarts, readiness, CPU/memory throttling).
2. **Isolated the failure mode**: consumers were restarting frequently due to an overly aggressive **liveness probe** combined with **slow startup/warmup** in Kubernetes. On restart, the consumer would reconnect, re-fetch metadata, and fall behind; in some cases it also caused bursts of re-deliveries because work wasn’t fully acked yet.
3. **Stabilized the cluster**:
    - Tuned probes (increased initial delay, used readiness to gate traffic until warm)
    - Right-sized CPU/memory requests/limits to avoid throttling
    - Reduced concurrency and added bounded prefetch to prevent memory spikes during catch-up
4. **Protected correctness**: verified idempotency/dedup behavior for at-least-once delivery (ensured writes were safe on retries) and confirmed tenant context propagation was intact.
5. **Drained backlog safely**: scaled consumer replicas horizontally after the system stabilized and monitored ingestion lag until queues returned to normal.
6. **Prevented recurrence**: added alerts on restart rate + readiness failures, and documented a cutover runbook (probe settings, resource baselines, and rollback triggers).

**Result**: Ingestion stabilized within ~30 minutes of intervention; we drained the backlog over the next hour with **no data loss** and no cross-tenant impact. The probe/resource baseline changes eliminated the restart loop and made subsequent services migrations to Kubernetes smoother.

---

## Q2: Describe a tough technical tradeoff you made.

**Situation**: We needed to add a "recent alerts" feature with sub-second query latency, but Cassandra queries were slow without the partition key.

**Task**: Design a solution that met latency SLA without re-architecting the entire storage layer.

**Action**:
1. Evaluated options: (a) secondary index in Cassandra, (b) materialized view, (c) separate Redis cache, (d) PostgreSQL for recent data.
2. Chose Redis cache with 24-hour TTL because:
   - Sub-millisecond reads
   - Write-through from processor (minimal code change)
   - Acceptable staleness (alerts are append-only)
3. Rejected Cassandra secondary index due to scatter-gather anti-pattern at scale.

**Result**: Achieved p99 < 50ms for recent alerts. Cache hit rate 98%. Added 1 new component but avoided costly Cassandra schema migration.

---

## Q3: How did you mentor a teammate or improve team practices?

**Situation**: A junior engineer was struggling with debugging production issues — often escalating to seniors without investigation.

**Task**: Help them become self-sufficient and reduce escalation noise.

**Action**:
1. Pair-debugged 3 incidents with them, narrating my thought process ("first I check queue depth, then consumer logs, then downstream health").
2. Created a runbook template: symptoms → data to gather → escalation criteria.
3. Introduced "blameless postmortems" where we documented lessons learned without finger-pointing.
4. Encouraged them to present one incident analysis per sprint to the team.

**Result**: Their escalation rate dropped 60% in 2 months. They became the go-to person for RabbitMQ issues. The runbook became a team standard adopted by 3 other squads.

---

## Q4: Tell me about a disagreement on a design decision.

**Situation**: I proposed using Cassandra for a new feature (asset inventory), but a senior architect wanted PostgreSQL for query flexibility.

**Task**: Reach alignment without delaying the project.

**Action**:
1. Wrote a 1-page RFC comparing both options on: write volume, query patterns, consistency needs, ops burden.
2. Organized a 30-min meeting with data points, not opinions.
3. Acknowledged their concern (complex queries are painful in Cassandra) and proposed a hybrid: Cassandra for raw events, PostgreSQL for materialized aggregates.
4. Agreed on success metrics: if PG replication lag > 5 sec, we'd revisit.

**Result**: Hybrid approach shipped on time. Both stakeholders felt heard. The RFC became a template for future design discussions.

---

## Q5: Describe a time you delivered under pressure.

**Situation**: A major customer (Fortune 100) needed a custom reporting feature in 3 weeks; normal timeline was 6 weeks.

**Task**: Deliver a working solution without compromising quality or burning out the team.

**Action**:
1. Negotiated scope: "must-have" (3 reports) vs "nice-to-have" (5 reports). Customer agreed to 3.
2. Parallelized work: I handled backend API while a teammate built the UI.
3. Reused existing query patterns instead of building new ones (80% code reuse).
4. Daily 15-min syncs to unblock fast.
5. Cut integration testing scope but added contract tests for critical paths.

**Result**: Delivered in 2.5 weeks. Customer signed a multi-year renewal. No production bugs in the first 60 days.

---

## Q6: How do you handle ambiguous requirements?

**Situation**: Product asked for "anomaly detection on storage metrics" with no specifics on accuracy, latency, or scope.

**Task**: Translate vague ask into a shippable MVP.

**Action**:
1. Asked clarifying questions: "What does success look like? Which metrics matter most? What's the tolerance for false positives?"
2. Proposed a phased approach: Phase 1 = static thresholds (ship in 2 weeks), Phase 2 = ML-based (8 weeks).
3. Built a prototype with 3 metrics, shared with PM for feedback.
4. Iterated based on customer interviews PM arranged.

**Result**: Phase 1 shipped and caught 2 real customer issues in the first month. Phase 2 scope was refined based on Phase 1 learnings, reducing wasted effort.

---

## Quick Reference: Behavioral Themes to Highlight

| Theme | Your angle |
|-------|------------|
| **Ownership** | On-call incident resolution, end-to-end feature delivery |
| **Technical depth** | Cassandra tuning, RabbitMQ backpressure, schema evolution |
| **Tradeoffs** | Redis vs Cassandra, RMQ vs Kafka, hybrid storage |
| **Mentoring** | Runbooks, pair debugging, blameless postmortems |
| **Communication** | RFCs, design docs, stakeholder alignment |
| **Delivery** | Scope negotiation, parallelization, MVP thinking |

---

## Cheat Sheet: Numbers to Remember

| Metric | Value |
|--------|-------|
| Events/sec throughput | 500K+ |
| Ingestion-to-queryable latency | < 30 sec p99 |
| Availability target | 99.9% |
| Cassandra replication factor | 3 |
| Data retention (hot) | 90 days |
| Data retention (cold) | 2 years |
| Recent alerts cache TTL | 24 hours |
| Recent alerts p99 latency | < 50 ms |

---

## 9. Migrating Applications from PCF to Kubernetes: Key Considerations

Migrating from Pivotal Cloud Foundry (PCF) to Kubernetes introduces both opportunities and challenges. Below are critical areas to focus on to ensure a smooth transition, especially for high-throughput, stateful services like those in the CloudIQ ingestion pipeline.

### 9.1 Application Configuration and Environment

| Area | PCF Approach | Kubernetes Approach | Migration Tips |
|------|--------------|---------------------|----------------|
| **Environment Variables** | Set via `cf set-env` or manifest.yml | Set via `ConfigMaps` and `Secrets` | Use K8s `ConfigMaps` for non-sensitive data and `Secrets` for credentials. Consider using a Helm chart to template these. |
| **Service Discovery** | Internal routes via DNS (e.g., `app-name.apps.internal`) | `ClusterIP` services with DNS (`svc-name.namespace.svc.cluster.local`) | Update service URLs to use K8s DNS names. Use `headless` services for stateful apps. |
| **Application Bindings** | `cf bind-service` for RDS, S3, etc. | Use K8s `ServiceBinding` or external secret management | Export bound credentials to K8s `Secrets` and mount them as environment variables or files. |

### 9.2 Containerization and Dockerfile

- **Base Images**: Use minimal, secure base images (e.g., `distroless`, `alpine`). Ensure the base image matches your runtime (e.g., OpenJDK JRE for Java apps).
- **Multi-stage Builds**: Reduce final image size by using a builder stage and then copying only the necessary artifacts.
- **Health Checks**: Define `HEALTHCHECK` in the Dockerfile and corresponding K8s `liveness` and `readiness` probes.
- **Non-root User**: Run the container as a non-root user for security.

### 9.3 Probes and Health Checks

| Probe Type | Purpose | Recommended Settings for High-Throughput Services |
|-------------|---------|----------------------------------------------------|
| **Liveness Probe** | Determines if the app should be restarted. | Set a generous `initialDelaySeconds` (e.g., 60s) to avoid premature restarts during startup. Use `httpGet` on a dedicated `/health/live` endpoint. |
| **Readiness Probe** | Determines if the app is ready to receive traffic. | Use an endpoint that checks critical dependencies (e.g., DB connectivity, RabbitMQ connection). Set `failureThreshold` to allow temporary hiccups. |
| **Startup Probe** | Gives the app extra time during startup. | Use if your app takes longer to start (e.g., due to schema migrations). Set `periodSeconds` and `failureThreshold` accordingly. |

### 9.4 Resource Management

- **CPU/Memory Requests and Limits**: In PCF, memory is set via `cf push -m`. In K8s, you must set both `requests` (guaranteed) and `limits` (cap). For high-throughput services:
  - Set `requests` close to typical usage to ensure scheduling.
  - Set `limits` higher to allow bursts but avoid throttling.
- **JVM in Containers**: If using Java, set `-XX:MaxRAMPercentage` to avoid the JVM using more memory than the container limit.
- **Horizontal Pod Autoscaler (HPA)**: Configure HPA based on custom metrics (e.g., RabbitMQ queue depth) in addition to CPU.

### 9.5 Stateful Services and Data

| Concern | PCF | Kubernetes | Mitigation |
|---------|-----|------------|------------|
| **StatefulSets** | Not applicable (PCF is stateless by default) | Use `StatefulSets` for stable network IDs and ordered deployment. | Use `StatefulSets` for services that require stable identities (e.g., consumers that need to recover offsets). |
| **Persistent Volume Claims (PVCs)** | Not used in PCF | Use PVCs for persistent storage. | Ensure storage class supports the required IOPS. Use `volumeClaimTemplates` in `StatefulSets`. |
| **Database Connections** | Bound via services | Use K8s `Service` to expose DB endpoints. | Use connection pooling and configure timeouts. |

### 9.6 Networking and Service Exposure

- **Ingress**: Use an `Ingress` controller (e.g., NGINX, Traefik) to expose services externally. Configure TLS termination at the ingress.
- **Internal Communication**: Use K8s `ClusterIP` services for internal traffic. Avoid using public IPs for internal services.
- **Network Policies**: Apply `NetworkPolicy` resources to restrict traffic between pods, enhancing security.

### 9.7 Deployment Strategies

| Strategy | Description | Use Case |
|----------|-------------|----------|
| **RollingUpdate** | Gradually replaces old pods with new ones. | Default for most services. |
| **Recreate** | Stops all old pods before starting new ones. | Useful for services that cannot run multiple versions simultaneously. |
| **Blue/Green** | Deploys a new version alongside the old, then switches traffic. | Low-risk deployments; requires double resources. |
| **Canary** | Routes a small subset of traffic to the new version. | Validate new versions with real traffic. |

### 9.8 Observability and Logging

- **Logging**: In PCF, logs are aggregated via `cf logs`. In K8s, use a logging stack (e.g., Fluentd/Fluent Bit + Elasticsearch + Kibana). Ensure logs are structured JSON.
- **Metrics**: Use Prometheus for scraping metrics exposed by applications (e.g., via `/actuator/prometheus` for Spring Boot apps).
- **Tracing**: Use OpenTelemetry instrumentation to emit traces to a backend (e.g., Jaeger, Zipkin).

### 9.9 Security

- **RBAC**: Define Role-Based Access Control (RBAC) policies to control who can interact with the cluster.
- **Pod Security Policies**: Use `PodSecurityContext` to run containers as non-root and drop unnecessary capabilities.
- **Secrets Management**: Avoid storing secrets in environment variables. Use a dedicated secret management system (e.g., HashiCorp Vault, K8s secrets with encryption).

### 9.10 CI/CD Pipeline Adjustments

- **Build and Push Images**: Update CI pipeline to build Docker images and push to a registry (e.g., Docker Hub, GCR, ECR).
- **Deploy to K8s**: Replace `cf push` with `helm upgrade` or `kubectl apply -f`.
- **Rollbacks**: Implement rollback strategies (e.g., `helm rollback`) to quickly revert bad deployments.

### 9.11 Common Pitfalls and How to Avoid Them

| Pitfall | Impact | Mitigation |
|---------|--------|------------|
| **Aggressive Liveness Probes** | Frequent restarts, leading to instability. | Set a longer `initialDelaySeconds` and use a lightweight endpoint. |
| **Ignoring Resource Limits** | Pods can be OOM-killed or cause node instability. | Set appropriate `requests` and `limits`. |
| **Hardcoded Service URLs** | Breaks when service names change. | Use K8s DNS names and environment variables for service discovery. |
| **Lack of Readiness Probes** | Traffic sent to unready pods, causing errors. | Implement readiness probes that check critical dependencies. |
| **Not Using Network Policies** | Increased attack surface. | Define `NetworkPolicy` to restrict traffic to necessary pods. |

### 9.12 Post-Migration Validation

- **Performance Testing**: Compare latency and throughput against PCF baselines.
- **Chaos Testing**: Use tools like Chaos Mesh to test resilience.
- **Monitoring**: Set up alerts for pod restarts, OOM kills, and high latency.
- **Rollback Drills**: Practice rollback procedures to ensure readiness for incidents.

---

## 10. Quick Reference: PCF to Kubernetes Migration Checklist

- [ ] **Containerize applications with optimized Dockerfiles.**
  - Use minimal base images (e.g., `distroless`, `alpine`) to reduce attack surface and image size.
  - Implement multi-stage builds to separate build-time dependencies from runtime artifacts.
  - Define a `HEALTHCHECK` instruction in the Dockerfile to enable basic health checks.
  - Run the application as a non-root user to enhance security.
  - Ensure the Dockerfile is version-controlled and follows best practices (e.g., `.dockerignore`).

- [ ] **Define health checks (`liveness`, `readiness`, `startup`).**
  - **Liveness Probe**: Use a lightweight endpoint (e.g., `/health/live`) that returns quickly. Set a generous `initialDelaySeconds` (e.g., 60s) to avoid premature restarts during startup.
  - **Readiness Probe**: Use an endpoint that checks critical dependencies (e.g., database, RabbitMQ). Configure `failureThreshold` to allow temporary hiccups.
  - **Startup Probe**: For applications with longer startup times (e.g., due to schema migrations), use a startup probe to give extra time before liveness and readiness probes kick in.

- [ ] **Set resource requests and limits.**
  - **CPU/Memory Requests**: Set to typical usage to ensure the scheduler can place the pod appropriately.
  - **CPU/Memory Limits**: Set higher than requests to allow bursts but prevent resource exhaustion.
  - For Java applications, use `-XX:MaxRAMPercentage` to align JVM heap size with container memory limits.
  - Monitor resource usage and adjust requests/limits based on observed patterns.

- [ ] **Configure `ConfigMaps` and `Secrets`.**
  - Use `ConfigMaps` for non-sensitive configuration data (e.g., feature flags, URLs).
  - Use `Secrets` for sensitive data (e.g., passwords, API keys). Consider encrypting secrets at rest.
  - Mount `ConfigMaps` and `Secrets` as environment variables or files, depending on application needs.
  - Use Helm charts or Kustomize to manage environment-specific configurations.

- [ ] **Update service discovery to use K8s DNS.**
  - Replace hardcoded service URLs with K8s DNS names (e.g., `svc-name.namespace.svc.cluster.local`).
  - Use `headless` services for stateful applications that need direct pod-to-pod communication.
  - Ensure applications are configured to resolve service names via K8s DNS.

- [ ] **Set up `Ingress` for external access.**
  - Deploy an `Ingress` controller (e.g., NGINX, Traefik) to manage external traffic routing.
  - Configure TLS termination at the Ingress level to offload SSL from applications.
  - Use annotations or `Ingress` resources to define routing rules (e.g., path-based, host-based).
  - Apply rate limiting and authentication at the Ingress if needed.

- [ ] **Implement logging and monitoring (Prometheus, ELK).**
  - **Logging**: Deploy a logging stack (e.g., Fluentd/Fluent Bit + Elasticsearch + Kibana). Ensure logs are structured (JSON) for easy parsing.
  - **Metrics**: Expose metrics in Prometheus format (e.g., via `/actuator/prometheus` for Spring Boot). Configure Prometheus to scrape metrics endpoints.
  - **Tracing**: Instrument applications with OpenTelemetry to emit traces to a backend (e.g., Jaeger, Zipkin).

- [ ] **Apply RBAC and network policies.**
  - **RBAC**: Define roles and role bindings to control access to K8s resources (e.g., who can deploy or view pods).
  - **Network Policies**: Use `NetworkPolicy` resources to restrict traffic between pods based on labels. Default-deny policies are recommended for enhanced security.
  - Regularly audit RBAC and network policies to ensure they align with the principle of least privilege.

- [ ] **Choose a deployment strategy (RollingUpdate, Blue/Green, Canary).**
  - **RollingUpdate**: Gradually replaces old pods with new ones. Suitable for most services with minimal downtime.
  - **Recreate**: Stops all old pods before starting new ones. Use for services that cannot run multiple versions simultaneously.
  - **Blue/Green**: Deploys a new version alongside the old, then switches traffic. Low-risk but requires double resources.
  - **Canary**: Routes a small subset of traffic to the new version. Useful for validating new versions with real traffic.

- [ ] **Update CI/CD pipeline for Docker and K8s.**
  - **Build and Push Images**: Modify CI pipeline to build Docker images and push to a registry (e.g., Docker Hub, GCR, ECR).
  - **Deploy to K8s**: Replace `cf push` commands with `helm upgrade` or `kubectl apply -f`.
  - **Rollbacks**: Implement rollback strategies (e.g., `helm rollback`) and test them in non-production environments.
  - Use GitOps practices (e.g., ArgoCD, Flux) to automate deployments and maintain state.

- [ ] **Conduct performance and chaos testing.**
  - **Performance Testing**: Load test the application in K8s to compare latency and throughput against PCF baselines.
  - **Chaos Testing**: Use tools like Chaos Mesh or Litmus to inject failures (e.g., pod deletions, network latency) and test resilience.
  - Validate autoscaling behavior under load and failure scenarios.

- [ ] **Document rollback procedures.**
  - Create runbooks for rollback scenarios, including step-by-step instructions and required commands.
  - Test rollback procedures regularly to ensure team familiarity and effectiveness.
  - Document success criteria for rollback (e.g., error rate thresholds) to automate decisions where possible.
