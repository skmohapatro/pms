# Financial News Dashboard - Design Document

## 1. Overview

A **real-time financial news dashboard** that serves as the landing page of the Portfolio Management System. It fetches the latest financial news in the background, uses **RAG (Retrieval-Augmented Generation)** to highlight news relevant to the user's **portfolio holdings** and **watchlist instruments**, and presents AI-generated headlines and insights.

### Coverage Scope
- **Portfolio Holdings**: Stocks you currently own - news affects your invested capital
- **Watchlist Instruments**: Stocks you're tracking - news helps timing buy/sell decisions

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ANGULAR FRONTEND                         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              DashboardComponent                       │  │
│  │  ┌──────────┐ ┌──────────────┐ ┌───────────────────┐ │  │
│  │  │ Portfolio │ │  Market      │ │  AI-Generated     │ │  │
│  │  │ Summary  │ │  Headlines   │ │  Portfolio Insight │ │  │
│  │  │ Cards    │ │  Feed        │ │  Section          │ │  │
│  │  └──────────┘ └──────────────┘ └───────────────────┘ │  │
│  │  ┌──────────────────────┐ ┌──────────────────────┐   │  │
│  │  │ Holdings-Related     │ │  Watchlist News      │   │  │
│  │  │ News (RAG-filtered)  │ │  (Tracking Alerts)   │   │  │
│  │  └──────────────────────┘ └──────────────────────┘   │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │ Top Gainers/Losers (Portfolio + Watchlist)   │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP
┌──────────────────────▼──────────────────────────────────────┐
│               SPRING BOOT BACKEND (8080)                    │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ DashboardController  /api/dashboard                    │ │
│  │  GET /summary        → Portfolio summary + live prices │ │
│  │  GET /news           → Fetched & RAG-ranked news       │ │
│  │  GET /insights       → AI-generated portfolio insights │ │
│  └───────────────┬────────────────────┬───────────────────┘ │
│                  │                    │                      │
│  ┌───────────────▼──┐  ┌─────────────▼────────────────┐    │
│  │ DashboardService │  │ NewsService                   │    │
│  │ (orchestrator)   │  │ (fetches from free news APIs) │    │
│  └───────────────┬──┘  └─────────────┬────────────────┘    │
│                  │                    │                      │
└──────────────────┼────────────────────┼─────────────────────┘
                   │                    │
┌──────────────────▼────────────────────▼─────────────────────┐
│              FLASK CHAT-BACKEND (5000)                       │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ POST /api/dashboard/insights                           │ │
│  │   - Receives: portfolio holdings + watchlist + news    │ │
│  │   - RAG: Combines context → Dell GenAI LLM             │ │
│  │   - Returns: ranked news, insights, headlines          │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. News Data Sources (Free, No API Key Required)

### Global Financial News

| Source | URL | Format | Content |
|--------|-----|--------|---------|
| **Google News RSS** | `news.google.com/rss/search?q=...` | XML/RSS | General + stock-specific news |
| **Bloomberg RSS** | `bloomberg.com/feeds/...` | XML/RSS | Global markets, economy, commodities |
| **Reuters RSS** | `reuters.com/arc/outboundfeeds/...` | XML/RSS | Breaking financial news, global markets |
| **CNBC RSS** | `cnbc.com/id/.../device/rss/rss.html` | XML/RSS | US markets, investing, economy |
| **Financial Times RSS** | `ft.com/?format=rss` | XML/RSS | Global business, markets analysis |
| **Yahoo Finance RSS** | `finance.yahoo.com/rss/` | XML/RSS | Stock quotes, market news, analysis |
| **MarketWatch RSS** | `marketwatch.com/rss/...` | XML/RSS | Real-time market data, stock analysis |
| **Investing.com RSS** | `investing.com/rss/...` | XML/RSS | Forex, commodities, crypto, stocks |
| **Seeking Alpha RSS** | `seekingalpha.com/feed.xml` | XML/RSS | Stock analysis, earnings, dividends |

### Indian Market News

| Source | URL | Format | Content |
|--------|-----|--------|---------|
| **Economic Times RSS** | `economictimes.com/rssfeedstopstories.cms` | XML/RSS | Indian market news, economy |
| **Moneycontrol RSS** | `moneycontrol.com/rss/...` | XML/RSS | Stock market, mutual funds, IPOs |
| **Business Standard RSS** | `business-standard.com/rss/...` | XML/RSS | Indian business, markets, policy |
| **Livemint RSS** | `livemint.com/rss/...` | XML/RSS | Finance, markets, startups |
| **NDTV Profit RSS** | `ndtvprofit.com/rss/...` | XML/RSS | Business news, stock updates |
| **NSE India** | `nseindia.com/api/...` | JSON | Official NSE announcements, corporate actions |
| **BSE India** | `bseindia.com/...` | JSON | Official BSE news, AGM/EGM notices |

### Regulatory & Central Bank Sources

| Source | URL | Format | Content |
|--------|-----|--------|---------|
| **RBI Press Releases** | `rbi.org.in/scripts/BS_PressReleaseDisplay.aspx` | HTML | Monetary policy, interest rates |
| **SEBI Updates** | `sebi.gov.in/sebiweb/home/HomeAction.do` | HTML | Regulatory updates, circulars |
| **Fed Reserve** | `federalreserve.gov/feeds/...` | XML/RSS | US monetary policy, FOMC decisions |

For each stock in the **portfolio** and **watchlist**, we query Google News RSS with the stock symbol to get holding-specific and watchlist-specific news.

---

## 4. RAG (Retrieval-Augmented Generation) Pipeline

### Flow:
```
1. RETRIEVE
   ├── Fetch general market news (RSS feeds)
   ├── Fetch stock-specific news for each portfolio holding (Google News RSS)
   ├── Fetch stock-specific news for each watchlist instrument (Google News RSS)
   ├── Gather portfolio context (holdings, P&L, recent trades)
   └── Gather watchlist context (tracked symbols, target prices)

2. AUGMENT
   ├── Rank news by relevance to portfolio AND watchlist (keyword matching + recency)
   ├── Tag news as "Portfolio" or "Watchlist" category
   ├── Build a context prompt:
   │   "You are a financial analyst. Given these portfolio holdings:
   │    [PORTFOLIO_DATA]
   │    And these watchlist stocks being tracked:
   │    [WATCHLIST_DATA]
   │    And these recent news articles:
   │    [NEWS_ARTICLES]
   │    Generate: 1) A market summary  2) Portfolio-specific insights
   │    3) Watchlist alerts (buy/sell opportunities)  4) Risk alerts
   │    5) Key headlines ranked by relevance"
   └── Limit context to fit LLM token window (~4000 tokens)

3. GENERATE
   └── Send to Dell GenAI (via existing chat-backend)
       → Returns structured insights JSON
```

### RAG Context Structure:
```json
{
  "portfolio": {
    "holdings": ["SBICARD", "HDFCBANK", "RELIANCE", ...],
    "totalInvested": 500000,
    "totalCompanies": 15
  },
  "watchlist": {
    "instruments": ["TCS", "INFY", "TATAMOTORS", ...],
    "totalTracking": 8,
    "watchlistNames": ["Tech Stocks", "Auto Sector"]
  },
  "news": [
    {
      "title": "RBI holds rates steady...",
      "source": "Economic Times",
      "date": "2026-02-25",
      "snippet": "...",
      "relevantHoldings": ["HDFCBANK", "SBICARD"],
      "relevantWatchlist": [],
      "category": "portfolio"
    },
    {
      "title": "TCS wins major AI contract...",
      "source": "Moneycontrol",
      "date": "2026-02-25",
      "snippet": "...",
      "relevantHoldings": [],
      "relevantWatchlist": ["TCS"],
      "category": "watchlist"
    }
  ]
}
```

---

## 5. Backend Components

### 5.1 NewsService.java (New)
**Location**: `backend/src/main/java/com/investment/portfolio/service/NewsService.java`

- `fetchMarketNews()` → Fetches from RSS feeds (ET, Moneycontrol)
- `fetchStockNews(String symbol)` → Google News RSS for a specific stock
- `fetchPortfolioNews(List<String> holdings)` → News for all portfolio holdings
- `fetchWatchlistNews(List<String> watchlistSymbols)` → News for all watchlist instruments
- `parseRssFeed(String url)` → XML parser for RSS
- Returns `List<NewsArticle>` DTO with category (portfolio/watchlist)

### 5.2 DashboardService.java (New)
**Location**: `backend/src/main/java/com/investment/portfolio/service/DashboardService.java`

- `getDashboardSummary()` → Portfolio stats + watchlist stats + live price summary
- `getNewsWithRelevance()` → Fetches news + tags with portfolio holdings AND watchlist
- `getWatchlistAlerts()` → Price movements and news for watchlist instruments
- `getAiInsights()` → Calls chat-backend RAG endpoint for AI-generated insights
- Caches results for 15 minutes to avoid excessive API calls

### 5.3 DashboardController.java (New)
**Location**: `backend/src/main/java/com/investment/portfolio/controller/DashboardController.java`

```
GET /api/dashboard/summary    → Portfolio + watchlist overview cards data
GET /api/dashboard/news       → News articles ranked by relevance (portfolio + watchlist)
GET /api/dashboard/insights   → AI-generated insights (RAG) for holdings + watchlist
GET /api/dashboard/watchlist  → Watchlist-specific alerts and price movements
```

### 5.4 DTOs (New)
- `NewsArticleDTO` — title, source, url, publishedDate, snippet, relevantHoldings[], relevantWatchlist[], category
- `DashboardSummaryDTO` — totalInvested, totalCompanies, totalPnl, watchlistCount, topGainers[], topLosers[]
- `DashboardInsightsDTO` — marketSummary, portfolioInsights[], watchlistAlerts[], riskAlerts[], headlines[]
- `WatchlistAlertDTO` — symbol, watchlistName, priceChange, newsCount, alertType (opportunity/risk)

### 5.5 Chat-Backend Endpoint (New)
**Location**: `chat-backend/app.py`

```python
@app.route('/api/dashboard/insights', methods=['POST'])
# Accepts: { portfolio: {...}, watchlist: {...}, news: [...] }
# Builds RAG prompt → Calls Dell GenAI
# Returns: { marketSummary, portfolioInsights, watchlistAlerts, riskAlerts, headlines }
```

---

## 6. Frontend Components

### 6.1 DashboardComponent (New)
**Location**: `frontend/src/app/components/dashboard/`

**Layout (Top to Bottom):**

```
┌─────────────────────────────────────────────────────────────┐
│  PORTFOLIO & WATCHLIST SNAPSHOT (5 summary cards)           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐
│  │ Total    │ │ Total    │ │ Day's    │ │ Portfolio│ │Watchlist│
│  │ Invested │ │Companies │ │ P&L      │ │ Value    │ │Tracking │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────┘
├─────────────────────────────────────────────────────────────┤
│  AI INSIGHTS (RAG-generated, auto-refreshes)                │
│  ┌─────────────────────────────────────────────────┐        │
│  │ 📊 Market Summary                               │        │
│  │ "Markets opened flat with banking stocks..."     │        │
│  ├─────────────────────────────────────────────────┤        │
│  │ 💡 Portfolio Insights                            │        │
│  │ • "HDFCBANK up 2.3% on strong Q3 results"       │        │
│  │ • "SBICARD near 52-week low, consider averaging" │        │
│  ├─────────────────────────────────────────────────┤        │
│  │ 👀 Watchlist Alerts                              │        │
│  │ • "TCS showing breakout pattern - consider entry"│        │
│  │ • "INFY down 3% after earnings miss"            │        │
│  ├─────────────────────────────────────────────────┤        │
│  │ ⚠️ Risk Alerts                                   │        │
│  │ • "3 stocks near circuit limits"                 │        │
│  └─────────────────────────────────────────────────┘        │
├──────────────────────────┬──────────────────────────────────┤
│  TOP MOVERS              │  NEWS FEED (Tabbed)              │
│  (Portfolio + Watchlist) │  ┌────────────────────────────┐  │
│  ┌────────────────────┐  │  │ [All] [Portfolio] [Watchlist]│ │
│  │ 🟢 Top Gainers     │  │  ├────────────────────────────┤  │
│  │  HDFCBANK  +2.3% 📦│  │  │ Breaking news cards        │  │
│  │  TCS       +1.8% 👀│  │  │ with source, time,         │  │
│  │                     │  │  │ relevance badge:           │  │
│  │ 🔴 Top Losers      │  │  │ 📦 = Portfolio holding     │  │
│  │  SBICARD   -1.2% 📦│  │  │ 👀 = Watchlist stock       │  │
│  │  INFY      -0.5% 👀│  │  │                            │  │
│  └────────────────────┘  │  └────────────────────────────┘  │
│  📦 = Portfolio          │                                   │
│  👀 = Watchlist          │                                   │
└──────────────────────────┴──────────────────────────────────┘
```

### 6.2 UI Features
- **Auto-refresh**: News fetched every 15 minutes in background
- **Relevance badges**: News tagged with "📦 Portfolio" or "👀 Watchlist" badge
- **Tabbed news feed**: Filter news by All / Portfolio / Watchlist
- **Skeleton loaders**: While AI insights are being generated
- **Color-coded cards**: Green for gains, red for losses
- **Click-through**: News cards link to original source
- **Refresh button**: Manual refresh for latest data
- **Watchlist alerts section**: Dedicated section for tracking opportunities

### 6.3 Angular Material Components Used
- `mat-card` — Summary cards, news cards, insight panels
- `mat-chip` — Relevance tags, stock symbols, category badges
- `mat-tab-group` — News feed tabs (All/Portfolio/Watchlist)
- `mat-progress-bar` — Loading states
- `mat-icon` — Icons for sections
- `mat-divider` — Section separators
- `mat-badge` — Alert counts

---

## 7. Routing Change

```typescript
// app-routing.module.ts
{ path: '', redirectTo: '/dashboard', pathMatch: 'full' },  // Changed from '/upload'
{ path: 'dashboard', component: DashboardComponent },        // New route
```

Navigation bar will get a new "Dashboard" link as the first item.

---

## 8. Data Flow Timeline

```
Page Load (t=0)
  ├── GET /api/dashboard/summary    (fast, DB only, ~100ms)
  │     └── Fetches portfolio stats + watchlist count
  ├── GET /api/dashboard/news       (RSS fetch, ~2-3s)
  │     └── Parallel: market RSS + portfolio RSS + watchlist RSS
  ├── GET /api/dashboard/watchlist  (live prices, ~1s)
  │     └── Fetches watchlist instruments with live prices
  └── GET /api/dashboard/insights   (RAG + LLM, ~5-8s)
        └── Backend: builds context (portfolio + watchlist) → chat-backend → returns

Auto-refresh (every 15 min)
  └── Repeat news + insights + watchlist fetch
```

---

## 9. Caching Strategy

| Data | Cache Duration | Storage |
|------|---------------|---------|
| Portfolio summary | 5 min | Backend in-memory (ConcurrentHashMap) |
| Watchlist data | 5 min | Backend in-memory (ConcurrentHashMap) |
| News articles (portfolio) | 15 min | Backend in-memory |
| News articles (watchlist) | 15 min | Backend in-memory |
| AI Insights | 15 min | Backend in-memory |
| Live prices (portfolio + watchlist) | 2 min | Frontend service |

---

## 10. File Changes Summary

### New Files:
| File | Type | Purpose |
|------|------|---------|
| `NewsService.java` | Backend Service | RSS feed fetching & parsing |
| `DashboardService.java` | Backend Service | Orchestrates dashboard data |
| `DashboardController.java` | Backend Controller | REST endpoints |
| `NewsArticleDTO.java` | Backend DTO | News article model |
| `DashboardSummaryDTO.java` | Backend DTO | Summary data model |
| `DashboardInsightsDTO.java` | Backend DTO | AI insights model |
| `WatchlistAlertDTO.java` | Backend DTO | Watchlist alert model |
| `dashboard.component.ts` | Frontend Component | Dashboard logic |
| `dashboard.component.html` | Frontend Template | Dashboard UI |
| `dashboard.component.scss` | Frontend Styles | Dashboard styling |

### Modified Files:
| File | Change |
|------|--------|
| `app-routing.module.ts` | Add dashboard route, change default |
| `app.component.html` | Add Dashboard nav link |
| `app.module.ts` | Declare DashboardComponent |
| `api.service.ts` | Add dashboard API methods |
| `chat-backend/app.py` | Add `/api/dashboard/insights` endpoint |

---

## 11. Implementation Phases

### Phase 1: Core Dashboard (MVP)
- Portfolio summary cards from existing DB data
- Watchlist summary card (count of tracked instruments)
- Top gainers/losers from live prices (portfolio + watchlist)
- RSS news feed integration
- Basic keyword matching for portfolio-relevant news

### Phase 2: Watchlist Integration
- Fetch news for watchlist instruments
- Tabbed news feed (All / Portfolio / Watchlist)
- Watchlist alerts section with price movements
- Category badges (📦 Portfolio / 👀 Watchlist)

### Phase 3: RAG Integration
- Build RAG prompt with portfolio + watchlist context + fetched news
- Chat-backend endpoint for AI insights generation
- AI-generated market summary, portfolio insights, and watchlist alerts
- Risk alerts based on portfolio and watchlist positions

### Phase 4: Polish
- Skeleton loaders and smooth transitions
- Auto-refresh mechanism
- Caching layer
- Error handling and fallback UI

---

## 12. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| RSS feeds may be blocked/slow | Cache aggressively, show stale data with timestamp |
| Dell GenAI rate limits | Cache AI insights for 15 min, queue requests |
| LLM hallucination in insights | Prefix with "AI-generated" label, provide source links |
| Too many RSS requests per stock | Batch top 10 holdings only, cache results |
