# Arbitrage Opportunity Backend Implementation Summary

## Overview
Successfully implemented the backend API for the Arbitrage Opportunity feature based on the specification in `ARBITRAGE_OPPORTUNITY_UI_PROMPT.md`. The implementation uses the exact calculation logic from the FNO scraper code and integrates with Groww APIs for live price data.

## Implementation Status: ✅ COMPLETED

### Files Created

#### 1. ArbitrageOpportunity.java (DTO)
**Location**: `backend/src/main/java/com/investment/portfolio/dto/ArbitrageOpportunity.java`

**Purpose**: Data Transfer Object containing all arbitrage opportunity fields

**Fields**:
- `companyCode` - Base symbol (e.g., SBIN)
- `currentDateTime` - Timestamp of calculation
- `selectedExpiry` - Futures expiry date
- `featurePriceL` - Futures last price
- `featurePriceC` - Futures close price
- `spotPrice` - Spot/underlying price
- `lotSize` - Futures lot size
- `holdingDays` - Days until expiry
- `priceDifference` - Futures - Spot price
- `pctPriceDifference` - % difference
- `futuresInvestment` - Futures price × lot size
- `spotInvestment` - Spot price × lot size
- `totalInvestment` - Total capital required
- `totalProfit` - Net profit after costs
- `perDayReturn` - Daily return
- `perAnnumReturn` - Annualized return %

#### 2. ArbitrageService.java (Business Logic)
**Location**: `backend/src/main/java/com/investment/portfolio/service/ArbitrageService.java`

**Key Features**:
- **Smart Symbol Matching**: Extracts base symbols from futures contracts (e.g., "SBIN" from "SBIN26MAYFUT")
- **Parallel Processing**: Uses thread pool for concurrent calculations
- **Groww API Integration**: Fetches live prices for spot and futures
- **Exact FNO Logic**: Implements same calculations as scraper code

**Core Methods**:
```java
public List<ArbitrageOpportunity> calculateArbitrageOpportunities(double minAnnualReturn)
private void processOneFuture(Instrument futInst, Map<String, Instrument> equityMap, ...)
private String extractBaseSymbol(String futuresSymbol)
private String extractExpiryDate(String futuresSymbol)
private int calculateHoldingDays(String nowIstStr, String expiryStr)
private Double fetchSpotPrice(String symbol)
private Map<String, Double> fetchFuturesPrices(String symbol)
```

**Financial Constants** (from FNO code):
```java
FUTURES_MARGIN_FRACTION = 0.20  // 20% margin
FIXED_COST = 2000.0             // ₹2000 fixed cost
```

**Calculation Logic** (exact port from FNO):
```java
double priceDiff = futPriceLast - spotPrice;
if (priceDiff <= 0) return; // Only positive differences

double pctPriceDiff = (spotPrice == 0) ? 0.0 : (priceDiff / (spotPrice / 100.0));
double futInv = futPriceLast * lotSize;
double spotInv = spotPrice * lotSize;
double totalInv = spotInv + futInv * FUTURES_MARGIN_FRACTION;
double totalProfit = (priceDiff * lotSize) - FIXED_COST;
double perDay = (holdingDays > 0) ? (totalProfit / holdingDays) : 0.0;
double perAnnumPct = (totalInv == 0) ? 0.0 : ((perDay * 365.0) / (totalInv / 100.0));

if (perAnnumPct >= minAnnualReturn) {
    // Add to opportunities list
}
```

#### 3. ArbitrageController.java (REST API)
**Location**: `backend/src/main/java/com/investment/portfolio/controller/ArbitrageController.java`

**Endpoints**:

##### GET `/api/arbitrage/opportunities`
- **Description**: Get all arbitrage opportunities
- **Parameters**: 
  - `minAnnualReturn` (optional, default: 5.0) - Minimum annual return %
- **Response**: List of ArbitrageOpportunity objects
- **Example**: `GET /api/arbitrage/opportunities?minAnnualReturn=7.0`

##### GET `/api/arbitrage/refresh`
- **Description**: Refresh and get latest opportunities with metadata
- **Parameters**: 
  - `minAnnualReturn` (optional, default: 5.0)
- **Response**: 
```json
{
  "opportunities": [...],
  "count": 25,
  "minAnnualReturn": 5.0,
  "durationMs": 2345,
  "timestamp": 1234567890
}
```

##### GET `/api/arbitrage/opportunities/{symbol}`
- **Description**: Get opportunities for specific symbol
- **Parameters**: 
  - `symbol` (path) - Base symbol (e.g., SBIN)
  - `minAnnualReturn` (optional, default: 5.0)
- **Response**: Filtered list of opportunities
- **Example**: `GET /api/arbitrage/opportunities/SBIN?minAnnualReturn=5.0`

##### GET `/api/arbitrage/health`
- **Description**: Health check endpoint
- **Response**: Service status

#### 4. PortfolioApplication.java (Configuration)
**Location**: `backend/src/main/java/com/investment/portfolio/PortfolioApplication.java`

**Added**: RestTemplate bean for HTTP client calls to Groww API

## How It Works

### Data Flow
```
1. Frontend calls /api/arbitrage/opportunities
2. Backend queries INSTRUMENTS table for EQ and FUT instruments
3. Smart matching pairs spot stocks with futures contracts
4. Parallel processing fetches live prices from Groww API
5. Calculation logic computes arbitrage metrics
6. Results filtered by minimum annual return
7. Sorted by annual return (descending)
8. JSON response sent to frontend
```

### Symbol Matching Logic
```
Futures: SBIN26MAYFUT
         ↓
Extract base: SBIN
         ↓
Find EQ instrument: SBIN (INSTRUMENT_TYPE='EQ')
         ↓
Fetch spot price for SBIN
Fetch futures price for SBIN26MAYFUT
         ↓
Calculate arbitrage opportunity
```

### Instrument Query
```sql
SELECT * FROM INSTRUMENTS 
WHERE EXCHANGE='NSE' AND INSTRUMENT_TYPE IN ('FUT', 'EQ')
```

**Example Data**:
| TRADING_SYMBOL | INSTRUMENT_TYPE | LOT_SIZE | SEGMENT |
|----------------|-----------------|----------|---------|
| SBIN | EQ | 1 | CASH |
| SBIN26APRFUT | FUT | 750 | FNO |
| SBIN26MARFUT | FUT | 750 | FNO |
| SBIN26MAYFUT | FUT | 750 | FNO |

## Testing

### Prerequisites
1. **Backend running**: Port 8080
2. **Chat backend running**: Port 5000 (for Groww API)
3. **Instruments loaded**: Run `/api/instruments/refresh` if needed
4. **Groww API configured**: API key set in chat backend

### Test Endpoints

#### 1. Health Check
```bash
curl http://localhost:8080/api/arbitrage/health
```

Expected:
```json
{
  "status": "UP",
  "service": "Arbitrage Opportunity Service"
}
```

#### 2. Get All Opportunities (5% minimum)
```bash
curl "http://localhost:8080/api/arbitrage/opportunities?minAnnualReturn=5.0"
```

#### 3. Get High-Return Opportunities (10% minimum)
```bash
curl "http://localhost:8080/api/arbitrage/opportunities?minAnnualReturn=10.0"
```

#### 4. Refresh with Metadata
```bash
curl "http://localhost:8080/api/arbitrage/refresh?minAnnualReturn=5.0"
```

#### 5. Get Opportunities for Specific Symbol
```bash
curl "http://localhost:8080/api/arbitrage/opportunities/SBIN?minAnnualReturn=5.0"
```

### Expected Response Format
```json
[
  {
    "companyCode": "SBIN",
    "currentDateTime": "07-MAR-2026-20:30:00",
    "selectedExpiry": "26-MAY-2026",
    "featurePriceL": 650.50,
    "featurePriceC": 649.75,
    "spotPrice": 645.00,
    "lotSize": 750.0,
    "holdingDays": 80,
    "priceDifference": 5.50,
    "pctPriceDifference": 0.85,
    "futuresInvestment": 487875.0,
    "spotInvestment": 483750.0,
    "totalInvestment": 581325.0,
    "totalProfit": 2125.0,
    "perDayReturn": 26.56,
    "perAnnumReturn": 16.67
  }
]
```

## Performance Optimization

### Parallel Processing
- Uses thread pool (2-8 threads based on CPU cores)
- Concurrent API calls to Groww
- Synchronized list for thread-safe results

### Caching Considerations
- Consider adding cache for instrument pairs (1-2 minutes)
- Cache Groww API responses (30-60 seconds)
- Reduce API rate limit issues

### Scalability
- Current: Processes all NSE futures contracts
- Optimization: Add pagination for large result sets
- Future: WebSocket for real-time updates

## Integration with Frontend

### Next Steps for Frontend Implementation
1. **Create Angular Component**: `arbitrage-opportunity.component.ts`
2. **Create Service**: `arbitrage.service.ts`
3. **Add Routing**: Route `/arbitrage-opportunity`
4. **Add Menu Item**: Navigation with icon
5. **Implement UI**: Material table with filters and sorting

### API Service Example
```typescript
export class ArbitrageService {
  private apiUrl = 'http://localhost:8080/api/arbitrage';
  
  getOpportunities(minReturn: number = 5.0): Observable<ArbitrageOpportunity[]> {
    return this.http.get<ArbitrageOpportunity[]>(
      `${this.apiUrl}/opportunities?minAnnualReturn=${minReturn}`
    );
  }
  
  refreshOpportunities(minReturn: number = 5.0): Observable<any> {
    return this.http.get(`${this.apiUrl}/refresh?minAnnualReturn=${minReturn}`);
  }
}
```

## Verification Checklist

- [x] DTO created with all required fields
- [x] Service implements exact FNO calculation logic
- [x] Smart symbol matching (EQ ↔ FUT)
- [x] Groww API integration for live prices
- [x] Parallel processing for performance
- [x] REST endpoints with filtering
- [x] RestTemplate bean configured
- [x] Code compiles successfully
- [ ] Manual API testing (requires running servers)
- [ ] Frontend integration (next phase)

## Known Limitations & Future Enhancements

### Current Limitations
1. **No Caching**: Every request fetches fresh data (slower but accurate)
2. **No Pagination**: Returns all results (may be large)
3. **Synchronous API Calls**: Could be optimized with reactive programming
4. **Basic Error Handling**: Could add retry logic for API failures

### Future Enhancements
1. **Caching Layer**: Redis for 1-2 minute cache
2. **WebSocket Support**: Real-time updates
3. **Advanced Filtering**: By expiry date, lot size, etc.
4. **Historical Tracking**: Store opportunities in database
5. **Alert System**: Notify on high-return opportunities
6. **Risk Metrics**: Add volatility, beta calculations
7. **Portfolio Integration**: Track actual arbitrage positions

## Troubleshooting

### Issue: No opportunities found
**Causes**:
- Instruments not loaded in database
- Chat backend not running (port 5000)
- Groww API not configured
- No futures contracts with positive price difference

**Solutions**:
1. Check instruments: `GET /api/instruments/status`
2. Refresh instruments: `POST /api/instruments/refresh`
3. Verify chat backend: `GET http://localhost:5000/api/stock/health`
4. Check Groww API key in chat backend

### Issue: Slow response times
**Causes**:
- Too many futures contracts to process
- Groww API rate limiting
- Network latency

**Solutions**:
1. Add caching layer
2. Implement pagination
3. Reduce minAnnualReturn threshold
4. Use async processing

### Issue: Incorrect calculations
**Causes**:
- Missing lot size data
- Incorrect expiry date extraction
- API returning null prices

**Solutions**:
1. Verify instrument data completeness
2. Check symbol pattern matching
3. Add logging for debugging
4. Validate API responses

## Summary

✅ **Backend implementation complete and ready for testing**

The backend now provides a fully functional REST API that:
- Matches spot and futures instruments intelligently
- Fetches live prices from Groww API
- Calculates arbitrage opportunities using exact FNO logic
- Filters by minimum annual return
- Returns sorted, ready-to-display data

**Next Phase**: Frontend Angular implementation to display the data in a professional UI table with filters, sorting, and real-time refresh capabilities.
