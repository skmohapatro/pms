# AI Chat System - Complete Flow Documentation

This document explains the end-to-end flow of the AI Chat feature — from the Angular frontend, through the Spring Boot backend, to the Flask chat-backend, and finally to Dell's GenAI API.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Map](#component-map)
3. [Request Flow - Step by Step](#request-flow---step-by-step)
4. [Layer 1: Frontend (Angular)](#layer-1-frontend-angular)
5. [Layer 2: Backend (Spring Boot)](#layer-2-backend-spring-boot)
6. [Layer 3: Dynamic Context Builder](#layer-3-dynamic-context-builder)
7. [Layer 4: Chat Backend (Flask)](#layer-4-chat-backend-flask)
8. [Layer 5: Dell GenAI API](#layer-5-dell-genai-api)
9. [Payload Transformations](#payload-transformations)
10. [Auto-Discovery of New Tables](#auto-discovery-of-new-tables)
11. [Available Models](#available-models)
12. [Error Handling Chain](#error-handling-chain)

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                      User's Browser                               │
│                                                                    │
│   Angular Frontend (port 4200)                                    │
│   chat.component.ts ──► api.service.ts ──► POST /api/chat        │
└──────────────────────────────┬───────────────────────────────────┘
                               │ HTTP POST :8080/api/chat
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│              Spring Boot Backend (port 8080)                      │
│                                                                    │
│  ChatController ──► ChatService ──► ChatContextService            │
│                          │               │                        │
│                          │         EntityManager (JPA)            │
│                          │         Auto-discovers ALL DB tables   │
│                          │         Fetches up to 100 rows each   │
│                          │                                        │
│                          │ HTTP POST :5000/api/chat               │
└──────────────────────────┼───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│              Flask Chat Backend (port 5000)                       │
│                                                                    │
│  /api/chat ──► get_auth_token() ──► call_dell_genai()            │
│                    │                       │                      │
│              aia_auth.sso()         HTTPS POST to Dell           │
│              (Dell SSO token)       GenAI Gateway                │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│           Dell GenAI API Gateway (external)                       │
│   https://aia.gateway.dell.com/genai/dev/v1/chat/completions     │
│   Models: gemma-3-27b-it  |  pixtral-12b-2409                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Component Map

| Layer | File | Responsibility |
|-------|------|----------------|
| **Frontend UI** | `frontend/src/app/components/chat/chat.component.ts` | User interaction, message display, model selection |
| **Frontend UI** | `frontend/src/app/components/chat/chat.component.html` | Chat template, input, suggestion chips |
| **Frontend API** | `frontend/src/app/services/api.service.ts` | HTTP calls to Spring Boot backend |
| **Backend Controller** | `backend/.../controller/ChatController.java` | REST endpoint `/api/chat` |
| **Backend DTOs** | `backend/.../dto/ChatRequestDTO.java` | Request model (message, model, history) |
| **Backend DTOs** | `backend/.../dto/ChatResponseDTO.java` | Response model (message, model, success) |
| **Backend Service** | `backend/.../service/ChatService.java` | Orchestrates context + AI call |
| **Context Builder** | `backend/.../service/ChatContextService.java` | Dynamically builds AI context from all DB tables |
| **Chat Backend** | `chat-backend/app.py` | Flask app, Dell SSO auth, GenAI API call |
| **Config** | `backend/src/main/resources/application.properties` | Chat backend URL config |

---

## Request Flow - Step by Step

```
Step 1: User types "What is my total investment?" → presses Enter
         │
Step 2: chat.component.ts::sendMessage() [line 106]
        - Adds user message to messages[]
        - Filters history (excludes error/loading messages)
        - Calls apiService.sendChatMessage(request)
         │
Step 3: api.service.ts::sendChatMessage() [line 195]
        - HTTP POST to http://localhost:8080/api/chat
        - Body: { message, model, history[] }
         │
Step 4: ChatController.java::chat() [line 22]
        - Validates message is not empty [line 23]
        - Delegates to chatService.chat(request) [line 27]
         │
Step 5: ChatService.java::chat() [line 24]
        - Calls contextService.buildDynamicContext() [line 26]  ◄── KEY
        - Prepends context as [system] message [lines 30–33]
        - Appends conversation history [lines 35–42]
        - Appends current user message [lines 44–47]
        - HTTP POST to http://localhost:5000/api/chat [lines 60–64]
         │
Step 6: ChatContextService.java::buildDynamicContext() [line 17]
        - Queries JPA EntityManager for ALL registered entities [line 22]
        - For each entity: schema + up to 100 rows of data [lines 24–41]
        - Returns complete context string
         │
Step 7: chat-backend/app.py::chat() [line 126]
        - Receives messages[] array (system + history + user)
        - Calls get_auth_token() → aia_auth.sso() [line 51]
        - Calls call_dell_genai() [line 63]
         │
Step 8: call_dell_genai() [line 63]
        - HTTPS POST to Dell GenAI API [line 89]
        - Headers: Authorization: Bearer <SSO-token>
         │
Step 9: Dell GenAI responds with AI-generated answer
         │
Step 10: Response bubbles back → user sees answer in chat UI
```

---

## Layer 1: Frontend (Angular)

### `chat.component.ts`

**File:** `frontend/src/app/components/chat/chat.component.ts`

#### Initialization (lines 65–69)
```typescript
ngOnInit(): void {
    this.loadModels();       // GET /api/chat/models → populates model dropdown
    this.loadContext();      // GET /api/chat/context → shows portfolio stats bar
    this.addWelcomeMessage();
}
```

#### Default Model (line 48)
```typescript
selectedModel = 'gemma-3-27b-it';  // Only Dell-compatible models used
```

#### Sending a Message (lines 106–158)
```typescript
sendMessage(): void {
    // Build history - EXCLUDES error messages to avoid corrupting context
    const history: ChatMessage[] = this.messages
        .filter(m => !m.isLoading && !m.isError)  // line 128 - critical filter
        .slice(-10)
        .map(m => ({ role: m.role, content: m.content }));

    const request: ChatRequest = {
        message,
        model: this.selectedModel,
        history: history.slice(0, -1)
    };
    this.apiService.sendChatMessage(request).subscribe({ ... });
}
```

#### Error Handling (lines 148–156)
```typescript
error: (error) => {
    this.messages.push({
        role: 'assistant',
        content: `Sorry, I encountered an error: ...`,
        timestamp: new Date(),
        isError: true   // flagged so it's excluded from future history
    });
}
```

### `api.service.ts` — Chat Methods (lines 194–205)

```typescript
private baseUrl = 'http://localhost:8080/api';  // line 99

sendChatMessage(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, request);
}

getChatModels(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/chat/models`);
}

getChatContext(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/chat/context`);
}
```

### `chat.component.html` Key Elements

| Lines | Element | Purpose |
|-------|---------|---------|
| 9–16 | `<mat-select [(value)]="selectedModel">` | AI model dropdown |
| 24–37 | `.portfolio-summary` | Live stats bar (companies, purchases, total ₹) |
| 39–55 | `.messages-container #chatContainer` | Scrollable chat history |
| 49 | `<mat-spinner>` | Loading indicator while waiting for AI |
| 50 | `[innerHTML]="formatMessage()"` | Renders markdown bold/italic/code |
| 57–67 | `.suggestion-chips` | Quick-start query buttons |
| 74–83 | `<textarea cdkTextareaAutosize>` | Auto-resizing input |

---

## Layer 2: Backend (Spring Boot)

### `ChatController.java` (lines 13–45)

```java
@RestController
@RequestMapping("/api/chat")   // base path
@CrossOrigin(origins = "*")    // allows Angular frontend
public class ChatController {

    @PostMapping               // POST /api/chat
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ChatResponseDTO.error("Message cannot be empty"));
        }
        ChatResponseDTO response = chatService.chat(request);
        return response.isSuccess()
            ? ResponseEntity.ok(response)
            : ResponseEntity.internalServerError().body(response);
    }

    @GetMapping("/models")     // GET /api/chat/models
    @GetMapping("/context")    // GET /api/chat/context
}
```

### `ChatService.java` — Message Assembly (lines 24–83)

```java
public ChatResponseDTO chat(ChatRequestDTO request) {
    // 1. Build full portfolio context
    String context = contextService.buildDynamicContext();  // line 26

    // 2. Assemble messages array
    List<Map<String, String>> messages = new ArrayList<>();

    // [system] - full portfolio data as context
    messages.add(Map.of("role", "system", "content", context));  // lines 30–33

    // [history] - previous conversation turns
    for (ChatRequestDTO.ChatMessage h : request.getHistory()) {  // lines 35–42
        messages.add(Map.of("role", h.getRole(), "content", h.getContent()));
    }

    // [user] - current question
    messages.add(Map.of("role", "user", "content", request.getMessage()));  // lines 44–47

    // 3. Build payload
    payload.put("model", request.getModel() != null ? request.getModel() : "gemma-3-27b-it");  // line 51
    payload.put("temperature", 0.7);   // line 52
    payload.put("max_tokens", 2000);   // line 53

    // 4. POST to Flask chat-backend
    restTemplate.postForEntity(chatBackendUrl + "/api/chat", entity, Map.class);  // lines 60–64
}
```

### Chat Backend URL Config

**File:** `backend/src/main/resources/application.properties`
```properties
chat.backend.url=${CHAT_BACKEND_URL:http://localhost:5000}
```
Injected via `@Value` at `ChatService.java` line 19.

---

## Layer 3: Dynamic Context Builder

This is the **most critical component** — it automatically discovers all database tables and includes their full data in the AI context.

### `ChatContextService.java`

**File:** `backend/src/main/java/com/investment/portfolio/service/ChatContextService.java`

#### Auto-Discovery via JPA Metamodel (lines 17–48)

```java
public String buildDynamicContext() {
    // Gets ALL JPA entities registered in the application
    Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();  // line 22

    for (EntityType<?> entityType : entities) {
        String entityName = entityType.getName();     // e.g. "PurchaseDateWise"
        Class<?> javaType = entityType.getJavaType(); // the Java class

        context.append("=== ").append(formatEntityName(entityName)).append(" ===\n");
        context.append("Schema: ").append(getEntitySchema(javaType)).append("\n");

        List<?> data = fetchEntityData(entityName);   // line 33
        context.append(formatDataAsTable(data, javaType));  // line 36
    }
}
```

#### Schema Extraction via Java Reflection (lines 54–67)

```java
private String getEntitySchema(Class<?> entityClass) {
    Field[] fields = entityClass.getDeclaredFields();
    // Reads field names and types via reflection
    // Output: "id (Long), date (String), company (String), quantity (Integer)..."
}
```

#### Data Fetching via JPQL (lines 70–77)

```java
private List<?> fetchEntityData(String entityName) {
    String queryStr = "SELECT e FROM " + entityName + " e";  // dynamic JPQL
    return entityManager.createQuery(queryStr).getResultList();
}
```

#### Row Formatting (lines 79–119)

```java
private String formatDataAsTable(List<?> data, Class<?> entityClass) {
    int maxRows = Math.min(data.size(), 100);  // line 93 - capped at 100 rows
    // Each row: "id:1, date:2024-01-15, company:RELIANCE, quantity:50, price:1500.0"
}
```

#### Example Generated Context

```
You are an AI assistant for an Investment Portfolio Management System.
Answer questions about the user's investment portfolio based on the following data.

=== Purchase Date Wise ===
Schema: id (Long), date (String), company (String), quantity (Integer), price (Double), investment (Double)
Data (148 records):
  id:1, date:2024-01-15, company:RELIANCE, quantity:50, price:1500.0, investment:75000.0
  id:2, date:2024-02-10, company:HDFC, quantity:100, price:1800.0, investment:180000.0
  ... (up to 100 rows)

=== Company Wise Aggregated Data ===
Schema: id (Long), instrument (String), qty (Integer), avgCost (Double), invested (Double)
Data (49 records):
  id:1, instrument:RELIANCE, qty:150, avgCost:1633.33, invested:245000.0
  ...

=== Investment Group ===
Schema: id (Long), groupName (String), instruments (Set)
Data (3 records):
  id:1, groupName:Tech Stocks, instruments:[5 items]
  ...

Provide helpful, accurate answers based on this portfolio data.
When discussing investments, include specific numbers and calculations when relevant.
Format currency values appropriately and be precise with quantities.
```

---

## Layer 4: Chat Backend (Flask)

**File:** `chat-backend/app.py`

### Startup — Auth Package Detection (lines 10–15)

```python
try:
    from aia_auth import auth
    AIA_AUTH_AVAILABLE = True   # Dell SSO will work
except ImportError:
    AIA_AUTH_AVAILABLE = False  # Falls back to mock token (won't work with real API)
    print("Warning: aia_auth module not available. Using mock authentication.")
```

> **Important:** If `AIA_AUTH_AVAILABLE = False`, all API calls will fail with 404. The `aia-auth-client` package must be installed.

### Authentication (lines 44–60)

```python
def get_auth_token():
    if not AIA_AUTH_AVAILABLE:
        return "mock-token-for-testing"  # ← causes 404 from Dell API

    if USE_SSO:                  # default: true (uses Windows SSO)
        token = auth.sso()       # line 51
        return token.token
    else:
        token = auth.client_credentials(CLIENT_ID, CLIENT_SECRET)  # line 56
        return token.token
```

### Dell GenAI API Call (lines 63–103)

```python
def call_dell_genai(messages, model, stream=False, temperature=0.7, max_tokens=2000):
    url = f"{DELL_GENAI_BASE_URL}/chat/completions"
    # = "https://aia.gateway.dell.com/genai/dev/v1/chat/completions"

    headers = {
        "Authorization": f"Bearer {token}",       # Dell SSO token
        "x-correlation-id": get_correlation_id(), # UUID for tracing
        "Content-Type": "application/json",
    }

    response = requests.post(url, headers=headers, json=payload,
                             verify=certifi.where(), timeout=60)
    response.raise_for_status()
    return response
```

### Chat Endpoint (lines 126–157)

```python
@app.route('/api/chat', methods=['POST'])
def chat():
    messages = data.get('messages', [])  # full array: [system, history..., user]
    model = data.get('model', DEFAULT_MODEL)

    response = call_dell_genai(messages=messages, model=model)
    result = response.json()

    return jsonify({
        "message": result['choices'][0]['message']['content'],
        "model": model,
        "finish_reason": result['choices'][0].get('finish_reason')
    })
```

### Available Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/health` | GET | Health check, shows `aia_auth_available` status |
| `/api/models` | GET | Returns list of available models |
| `/api/chat` | POST | Main chat endpoint |
| `/api/chat/stream` | POST | Streaming chat (SSE) |

---

## Layer 5: Dell GenAI API

**Endpoint:** `https://aia.gateway.dell.com/genai/dev/v1/chat/completions`

### Request (OpenAI-compatible format)

```json
{
  "model": "gemma-3-27b-it",
  "messages": [
    { "role": "system", "content": "You are an AI assistant... [full portfolio data]" },
    { "role": "user",   "content": "What is my total investment?" }
  ],
  "temperature": 0.7,
  "max_tokens": 2000,
  "stream": false
}
```

### Response

```json
{
  "choices": [{
    "message": {
      "role": "assistant",
      "content": "Your total investment across all holdings is ₹32,25,134.72..."
    },
    "finish_reason": "stop"
  }]
}
```

---

## Payload Transformations

### Frontend → Spring Boot

```json
POST http://localhost:8080/api/chat
{
  "message": "Which company has the highest investment?",
  "model": "gemma-3-27b-it",
  "history": [
    { "role": "user",      "content": "What is my total investment?" },
    { "role": "assistant", "content": "Your total investment is ₹32,25,134.72..." }
  ]
}
```

### Spring Boot → Flask (after context injection)

```json
POST http://localhost:5000/api/chat
{
  "messages": [
    {
      "role": "system",
      "content": "You are an AI assistant...\n=== Purchase Date Wise ===\nSchema: ...\nData (148 records):\n  id:1, date:2024-01-15, company:RELIANCE...\n..."
    },
    { "role": "user",      "content": "What is my total investment?" },
    { "role": "assistant", "content": "Your total investment is ₹32,25,134.72..." },
    { "role": "user",      "content": "Which company has the highest investment?" }
  ],
  "model": "gemma-3-27b-it",
  "temperature": 0.7,
  "max_tokens": 2000
}
```

### Flask → Dell GenAI API

Same as above, with auth headers:
```
Authorization: Bearer <Dell-SSO-Token>
x-correlation-id: <UUID>
Content-Type: application/json
```

---

## Auto-Discovery of New Tables

The system **automatically includes any new JPA entity** without code changes. Here's how:

### How It Works

1. You create a new JPA entity class (e.g., `DividendRecord.java`) with `@Entity`
2. Spring Boot registers it in the JPA metamodel on startup
3. `ChatContextService.java` line 22 calls `entityManager.getMetamodel().getEntities()`
4. The new entity is automatically included in the next chat context build
5. The AI can immediately answer questions about the new table's data

### Example: Adding a New Table

```java
// New entity added to backend
@Entity
@Table(name = "dividend_records")
public class DividendRecord {
    @Id private Long id;
    private String company;
    private Double amount;
    private String date;
}
```

After restart, the AI context will automatically include:
```
=== Dividend Record ===
Schema: id (Long), company (String), amount (Double), date (String)
Data (N records):
  id:1, company:RELIANCE, amount:5000.0, date:2024-03-31
  ...
```

No changes needed in `ChatContextService`, `ChatService`, or `ChatController`.

---

## Available Models

Only models available on Dell's internal GenAI gateway are listed:

| Model | Provider | Notes |
|-------|----------|-------|
| `gemma-3-27b-it` | Google DeepMind | Default, best for portfolio analysis |
| `pixtral-12b-2409` | Mistral AI | Also works, faster responses |

**Models that do NOT work on Dell's API** (return 404):
- `gpt-4o-mini` — OpenAI (not on Dell gateway)
- `gpt-4o` — OpenAI (not on Dell gateway)
- `claude-3-5-sonnet-20241022` — Anthropic (not on Dell gateway)

Model list is defined in `chat-backend/app.py` lines 31–34 and synced to the frontend via `GET /api/chat/models`.

---

## Error Handling Chain

```
Dell API Error (404/400/500)
        │
        ▼
chat-backend/app.py::chat() [line 155]
  except Exception as e:
    return jsonify({"error": str(e)}), 500
        │
        ▼
ChatService.java [line 80]
  catch (Exception e):
    return ChatResponseDTO.error("Error communicating with AI service: " + e.getMessage())
        │
        ▼
ChatController.java [line 31]
  return ResponseEntity.internalServerError().body(response)
        │
        ▼
api.service.ts → HTTP 500 error observable
        │
        ▼
chat.component.ts::error handler [line 148]
  messages.push({ content: "Sorry, I encountered an error...", isError: true })
  // isError=true ensures this message is EXCLUDED from future chat history
```

### Common Errors and Causes

| Error | Cause | Fix |
|-------|-------|-----|
| `404 Client Error for url: .../chat/completions` | Model not available on Dell API | Use `gemma-3-27b-it` or `pixtral-12b-2409` |
| `400 Client Error` | Context too large or bad payload | Reduce history length; check for error messages in history |
| `Error communicating with AI service` | Flask chat-backend not running | Start `python app.py` in `chat-backend/` |
| `Http failure response: 0 Unknown Error` | Spring Boot not running or CORS error | Restart Spring Boot; check `WebConfig.java` |
| `aia_auth module not available` | `aia-auth-client` not installed | `pip install aia-auth-client==0.0.8` |

---

## Running All Services

```bash
# Terminal 1: Chat Backend (must start first)
cd chat-backend
python app.py
# Verify: http://localhost:5000/api/health → aia_auth_available: true

# Terminal 2: Spring Boot Backend
cd backend
mvn spring-boot:run
# Verify: http://localhost:8080/api/chat/models

# Terminal 3: Angular Frontend
cd frontend
npm start
# Open: http://localhost:4200/chat
```

Or use the provided batch file:
```bash
start-servers.bat   # starts all 3 services in separate windows
```
