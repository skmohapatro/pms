# E-commerce Platform (Amazon-like) — System Design (Interview Doc)

## 1. Problem Statement
Design a large-scale e-commerce platform similar to Amazon that supports:

- Browsing a large product catalog
- Search and recommendations
- Cart and checkout
- Payments
- Order management and fulfillment
- Customer accounts and addresses
- Multi-seller marketplace (optional extension)

This document is written in an interview style: requirements, assumptions, APIs, high-level architecture, deep dives, tradeoffs, and operational considerations.

---

## 2. Scope and Assumptions

### 2.1 In-scope (MVP)
- Product catalog (categories, product detail pages)
- Search (keyword + filters)
- User accounts (auth, profile)
- Cart
- Checkout
- Payments integration (via payment provider)
- Orders (create, view, cancel)
- Inventory reservation / decrement
- Shipping address management
- Notifications (email/SMS) for order status

### 2.2 Out-of-scope (explicitly not designing in detail)
- In-house payment processing (PCI-heavy); we use a PSP (Stripe/Adyen/etc.)
- Last-mile delivery optimization
- Tax engine and internationalization complexities
- Full recommendation ML model training (we define interfaces and event streams)

### 2.3 Key assumptions (numbers to anchor design)
- 50M registered users, 5M DAU
- Peak 300k read QPS (homepage, product pages, search)
- Peak 20k write QPS (cart updates, checkout, order placement)
- Catalog size: 200M SKUs (marketplace scale)
- Availability goal: 99.95% for browse/search, 99.99% for checkout/order placement
- Latency goal (p95):
  - Browse/product page: < 300ms (server-side)
  - Search: < 500ms
  - Checkout: < 800ms (excluding external payment redirect)

---

## 3. Requirements

## 3.1 Functional Requirements

### 3.1.1 Customer-facing
- Browse categories and product pages
- Search products with filters/sorting
- View price, availability, shipping estimates
- Maintain cart (add/remove/update quantity)
- Checkout:
  - select shipping address
  - select shipping method
  - apply coupons / gift cards (optional extension)
  - pay using an external payment provider
- Order history and order details
- Cancel order (within policy window)
- Track order status (placed, confirmed, shipped, delivered, returned)

### 3.1.2 Platform operations
- Product onboarding (create/update product, images)
- Inventory updates (stock increment/decrement, reservation release)
- Pricing updates
- Order processing pipeline (payment capture, fulfillment, shipment updates)
- Customer notifications

### 3.1.3 Optional extensions
- Marketplace multi-seller support (seller accounts, offers per SKU)
- Reviews and ratings
- Recommendations and personalization
- Returns and refunds workflow

## 3.2 Non-Functional Requirements

### 3.2.1 Scalability
- Heavy read traffic on catalog and search
- Flash sale / event spikes
- Efficient caching and CDN for static content (images)

### 3.2.2 Availability and Reliability
- Checkout and order placement must be highly available and consistent
- Prevent overselling (inventory correctness)
- Idempotency for payment/order APIs
- Backpressure and queue-based processing for downstream systems

### 3.2.3 Consistency
- Strong consistency required for:
  - inventory reservation and decrement
  - payment/order state transitions
- Eventual consistency acceptable for:
  - search index updates
  - recommendations
  - analytics

### 3.2.4 Performance
- Low-latency reads for product pages
- Efficient pagination and filtering for search

### 3.2.5 Security and Privacy
- Authentication/authorization
- PII encryption and access controls
- Secure payment flows (tokenization via PSP)
- Audit logs for critical actions

### 3.2.6 Observability
- End-to-end tracing for checkout pipeline
- SLOs, dashboards, alerts
- Dead-letter queues (DLQs) for failed async tasks

---

## 4. High-Level Architecture

### 4.1 Major components
- **Clients**: Web, Mobile
- **CDN**: static assets, images, edge caching
- **API Gateway / BFF**:
  - auth, rate limiting, request routing
  - specialized BFFs for web/mobile (optional)
- **Core Microservices**:
  - Identity Service
  - Catalog Service
  - Search Service
  - Pricing Service
  - Inventory Service
  - Cart Service
  - Checkout Service
  - Payment Service (integration)
  - Order Service
  - Fulfillment/Shipping Service
  - Notification Service
- **Async Messaging**: Kafka/Pulsar (event streams) + optional RabbitMQ (task queues)
- **Data stores**:
  - Product metadata store (NoSQL or relational depending on modeling)
  - Search index (Elasticsearch/OpenSearch)
  - Carts store (Redis/DynamoDB)
  - Orders store (relational or scalable NewSQL)
  - Inventory store (strong consistency; relational/NewSQL)
  - Object storage for images (S3)
- **Caching**:
  - CDN cache
  - Redis/Memcached for hot product pages, pricing snapshots

### 4.2 Architecture diagram (text)
- Client -> CDN -> API Gateway
- API Gateway -> (Catalog/Search/Cart/Checkout/Orders/...)
- Services publish events -> Event Bus (Kafka)
- Consumers -> update search index, send notifications, analytics, recommendations

---

## 5. Service Decomposition (Responsibilities)

### 5.1 Identity Service
- User registration, login
- Token issuance (OAuth2/OIDC)
- Session management

### 5.2 Catalog Service
- Product entities: title, description, attributes, category, images
- Read-heavy APIs for product pages

### 5.3 Search Service
- Search query parsing, filters, ranking
- Uses OpenSearch/Elasticsearch
- Index built from catalog + offer + inventory signals

### 5.4 Pricing Service
- Price calculation: base price, discounts, taxes placeholders
- Supports time-based promotions

### 5.5 Inventory Service
- Source of truth for stock levels
- Reservation APIs for checkout
- Prevent oversell; supports per-warehouse (optional)

### 5.6 Cart Service
- User cart state (items, quantities)
- High write rate, low-latency
- TTL for abandoned carts

### 5.7 Checkout Service
- Orchestrates:
  - cart validation
  - pricing snapshot
  - inventory reservation
  - payment intent creation
  - order creation

### 5.8 Payment Service (integration)
- Creates payment intents with PSP
- Handles webhooks (payment succeeded/failed)
- Ensures idempotency

### 5.9 Order Service
- Order state machine
- Stores order items, amounts, shipping address snapshot
- Exposes order history APIs

### 5.10 Fulfillment/Shipping Service
- Assign fulfillment center (optional)
- Shipment creation, tracking updates

### 5.11 Notification Service
- Email/SMS/push notifications
- Template management

---

## 6. Data Model (High level)

### 6.1 Product / Offer modeling
- **Product**: canonical SKU information (title, brand, attributes)
- **Offer** (optional marketplace): seller-specific price, condition, shipping promises

### 6.2 Cart
- cart_id (user_id or anonymous session)
- items: (sku_id, qty)
- created_at, updated_at, ttl

### 6.3 Inventory
- sku_id
- location_id (optional)
- available_qty
- reserved_qty
- version (for optimistic concurrency)

### 6.4 Order
- order_id
- user_id
- order_items (sku_id, qty, unit_price, item_total)
- totals (subtotal, shipping, tax, grand_total)
- shipping_address snapshot
- status (PLACED, PAYMENT_PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELED)
- idempotency_key

---

## 7. Storage Choices (and why)

### 7.1 Catalog Store
Options:
- **NoSQL (DynamoDB/Cassandra)** for scale and flexible attributes
- **PostgreSQL** if schema is stable and strong relational needs exist

Typical hybrid:
- Product core in NoSQL
- Operational/admin metadata in relational

### 7.2 Search Index
- OpenSearch/Elasticsearch
- Asynchronously updated from catalog events

### 7.3 Cart Store
- Redis Cluster (fast reads/writes, TTL)
- Alternative: DynamoDB for durability + global distribution

### 7.4 Orders and Payments State
- Relational DB / NewSQL (Aurora, Spanner, CockroachDB)
- Reason: transactions for order state transitions and strong constraints

### 7.5 Inventory
- NewSQL or relational with careful locking/optimistic concurrency
- Strong consistency required to prevent oversell

---

## 8. Key APIs (Example)

### 8.1 Catalog
- `GET /v1/products/{skuId}`
- `GET /v1/categories/{categoryId}/products?page=...`

### 8.2 Search
- `GET /v1/search?q=...&filters=...&sort=...&page=...`

### 8.3 Cart
- `GET /v1/cart`
- `POST /v1/cart/items` `{ skuId, qty }`
- `PATCH /v1/cart/items/{skuId}` `{ qty }`

### 8.4 Checkout
- `POST /v1/checkout/preview` (pricing + availability)
- `POST /v1/checkout/submit` (creates order + payment intent)

### 8.5 Payments
- `POST /v1/payments/intents` (idempotent)
- `POST /v1/payments/webhook` (from PSP)

### 8.6 Orders
- `GET /v1/orders` (paginated)
- `GET /v1/orders/{orderId}`
- `POST /v1/orders/{orderId}/cancel`

---

## 9. Key Flows (Deep Dive)

## 9.1 Product page load (read path)
1. Client requests product page
2. CDN serves cached HTML/images when possible
3. API Gateway routes to Product Page BFF
4. BFF calls:
   - Catalog Service (product details)
   - Pricing Service (price snapshot)
   - Inventory Service (availability indicator)
5. Response assembled and cached (short TTL)

Caching notes:
- Cache product details longer (minutes)
- Cache price/inventory shorter (seconds)

## 9.2 Search
1. Query -> Search Service
2. Search hits OpenSearch index
3. Results enriched with pricing/availability (either pre-indexed signals or a fast batch fetch)

Tradeoff:
- Pre-index price/inventory yields faster search but index churn
- Fetch on read reduces churn but increases latency

## 9.3 Add to cart
1. `POST /cart/items`
2. Cart Service validates SKU existence (light check)
3. Writes to Redis/DynamoDB
4. Emits event `CartUpdated` for analytics (optional)

## 9.4 Checkout and place order (critical write path)
Goals:
- No oversell
- No duplicate orders
- Payment and order state consistent

### Step-by-step
1. Client calls `POST /checkout/preview`
   - validate cart
   - compute totals
   - verify inventory is likely available
2. Client calls `POST /checkout/submit` with `idempotency_key`
3. Checkout Service:
   - loads cart
   - creates pricing snapshot
   - reserves inventory (Inventory Service)
   - creates payment intent via PSP (Payment Service)
   - creates order in `PAYMENT_PENDING`
   - returns `order_id` + payment redirect/token
4. PSP callback/webhook `payment_succeeded`:
   - Payment Service verifies signature
   - publishes `PaymentSucceeded(order_id)`
5. Order Service consumes event:
   - transitions order to `CONFIRMED`
   - emits `OrderConfirmed`
6. Fulfillment Service consumes `OrderConfirmed`:
   - allocates shipment
   - updates status `SHIPPED` later

### Handling failures
- Payment fails: release inventory reservation
- Webhook delayed: order stays `PAYMENT_PENDING` with timeout
- Retry safety: idempotency key on `checkout/submit` and `payments/intents`

---

## 10. Consistency, Idempotency, and Concurrency

### 10.1 Preventing oversell
Common strategy:
- Inventory Service provides `Reserve(sku, qty, reservation_id)`
- Inventory DB update using either:
  - atomic conditional update (NoSQL with condition), or
  - transactional update (NewSQL), or
  - optimistic concurrency with version checks

### 10.2 Idempotency
- Client supplies `Idempotency-Key` for checkout submission
- Store idempotency record keyed by `(user_id, idempotency_key)` mapping to `order_id`
- Payment intents created idempotently too

### 10.3 Saga pattern
- Checkout is a distributed transaction
- Use saga with compensating actions:
  - reserve inventory -> if payment fails, release reservation
  - create order -> if inventory reservation fails, do not create order

---

## 11. Messaging and Eventing

### 11.1 Event bus
- Kafka/Pulsar for durable streams

Key topics:
- `catalog.product_updated`
- `inventory.changed`
- `order.placed`
- `order.confirmed`
- `payment.succeeded`
- `payment.failed`

### 11.2 Consumers
- Search indexer service
- Notification service
- Analytics pipeline
- Recommendation feature store updates

### 11.3 DLQs and retries
- Retriable failures: exponential backoff
- Non-retriable: DLQ + alert
- Exactly-once not required; design consumers to be idempotent

---

## 12. Caching Strategy
- CDN for images and static content
- Redis cache for:
  - hot product details
  - category listings
  - computed price snapshots (short TTL)

Invalidation:
- publish `ProductUpdated` events and selectively purge hot keys
- use TTL as fallback

---

## 13. Security
- OAuth2/OIDC for auth
- RBAC for admin APIs
- Encrypt PII at rest
- Tokenize payment details (do not store card data)
- Signed webhook validation for PSP callbacks

---

## 14. Observability and Operations

### 14.1 SLOs
- Browse/search availability and latency SLO
- Checkout success rate SLO
- Payment webhook processing lag SLO

### 14.2 Monitoring
- Golden signals: latency, traffic, errors, saturation
- Queue lag monitoring (Kafka consumer lag)

### 14.3 Tracing
- Distributed tracing across Checkout -> Inventory -> Payment -> Order

---

## 15. Capacity and Scaling Notes
- Horizontal scale stateless services behind LBs
- Partition/shard by:
  - user_id for cart/orders
  - sku_id for inventory
- Use read replicas for read-heavy relational workloads

---

## 16. Tradeoffs and Alternatives

- **Microservices vs modular monolith**:
  - For interview: microservices show separation of concerns
  - In reality: could start as modular monolith, split at scale

- **Inventory correctness vs latency**:
  - Strong consistency at checkout
  - Approx availability on product pages acceptable

- **Search enrichment strategy**:
  - Pre-index vs fetch-on-read

---

## 17. Quick Interview Summary (What I’d say aloud)
- Requirements split into read-heavy (catalog/search) and correctness-heavy (checkout/order/inventory).
- Use CDN + caches for reads; event-driven pipelines to update search/recs.
- Checkout uses a saga with idempotency and strongly consistent inventory reservations.
- Orders are the source of truth; payments integrated via PSP with webhooks.
- Observability, DLQs, retries, and security are first-class.
