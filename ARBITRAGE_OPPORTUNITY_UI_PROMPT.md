# Create an "Arbitrage Opportunity" UI Screen for Futures-Spot Arbitrage Detection

# UI Should start geting the arbitrage data on clicking to the tab "Arbitrage Opportunity"

## Background
We have an existing Java-based arbitrage detection system that:
- Scrapes NSE data to find cash-and-carry arbitrage opportunities
- Calculates returns based on futures-spot price differences
- Currently exports results to Excel with 5%+ annual return filter

## Instrument Data Source

To get instruments and lot sizes, query the instruments table. The code should be smart enough to:
1. **Find EQ (equity) stocks** for spot prices
2. **Find FUT (futures) contracts** for the same underlying stock
3. **Fetch live prices** from Groww API for both spot and futures instruments

### SQL Query:
```sql
SELECT * FROM INSTRUMENTS 
WHERE EXCHANGE='NSE' AND INSTRUMENT_TYPE IN ('FUT', 'EQ') 
ORDER BY TRADING_SYMBOL
```

### Sample Data Structure:
| ID | EXCHANGE | TRADING_SYMBOL | INSTRUMENT_TYPE | LOT_SIZE | NAME | SEGMENT |
|----|----------|----------------|-----------------|----------|------|---------|
| 106444 | NSE | SBIN | EQ | 1 | SBI | CASH |
| 82268 | NSE | SBIN26APRFUT | FUT | 750 | null | FNO |
| 82675 | NSE | SBIN26MARFUT | FUT | 750 | null | FNO |
| 82999 | NSE | SBIN26MAYFUT | FUT | 750 | null | FNO |

### Smart Matching Logic:
The backend should:
1. **Extract base symbol** from futures contracts (e.g., "SBIN" from "SBIN26MAYFUT")
2. **Find matching EQ stock** (e.g., "SBIN")
3. **Pair them for arbitrage calculations**:
   - Spot price from EQ instrument (SBIN)
   - Futures prices from FUT instruments (SBIN26MAYFUT, etc.)
   - Lot size from FUT instrument (750)


## Current Logic (Reference Implementation)
```java
// Core calculations from NseFuturesAutomationParallel1.java:
priceDiff = futuresPrice - spotPrice
totalInvestment = spotInvestment + (futuresInvestment × 20% margin)
netProfit = (priceDiff × lotSize) - ₹2000 fixedCost
annualReturn% = ((dailyProfit × 365) / totalInvestment) × 100

// Key constants:
FUTURES_MARGIN_FRACTION = 0.20  // 20% margin for futures
FIXED_COST = 2000.0             // ₹2000 fixed cost per trade

// Filtering criteria:
if (priceDiff <= 0) continue;  // Only positive differences
if (perAnnumPct >= 5) {        // 5% minimum annual return
    // Add to results
}
```

## Requirements

### 1. Replace Data Source
- **Remove web scraping** and integrate with **Groww APIs** for:
  - Spot prices (underlying values)
  - Futures prices (last/close prices)
  - Lot sizes and expiry dates
- Use existing Groww API integration from portfolio system

### 2. Create New Angular Screen: "Arbitrage Opportunity"
**Location**: `frontend/src/app/components/arbitrage-opportunity/`

#### Required Features:
- **Data Table** displaying all arbitrage opportunities (currently Excel rows)
- **Real-time Updates** from Groww API
- **Professional UI** matching existing portfolio design

#### Table Columns (same as current Excel output):
1. Company Code (Symbol)
2. Date/Time (Current timestamp)
3. Expiry Date
4. Futures Price (Last)
5. Futures Price (Close)
6. Spot Price
7. Lot Size
8. Holding Days
9. Price Difference
10. % Price Difference
11. Futures Investment Amount
12. Spot Investment Amount
13. Total Investment
14. Total Profit
15. Per Day Return
16. Per Annum Return %

### 3. Enhanced UI Features

#### Filter Controls:
- Minimum annual return % (default: 5%)
- Symbol search/filter
- Expiry date range filter
- Price difference threshold

#### Sorting & Display:
- Click-to-sort on all numeric columns
- Default sort: Annual Return % (descending)
- Color coding for opportunities:
  - **Green**: >15% annual return
  - **Blue**: 10-15% annual return
  - **Orange**: 5-10% annual return

#### Actions:
- Manual refresh button
- Export to CSV/Excel
- Row selection for detailed view
- Real-time auto-refresh toggle

### 4. Technical Implementation

#### Backend Changes:
- **New Controller**: `ArbitrageController.java`
- **New Endpoint**: `/api/arbitrage/opportunities`
- **Service Layer**: `ArbitrageService.java`
- **Integration**: Use existing Groww API service
- **Calculation Logic**: Port from Java scraper code

#### Key Backend Methods:
```java
@RestController
@RequestMapping("/api/arbitrage")
public class ArbitrageController {
    
    @GetMapping("/opportunities")
    public ResponseEntity<List<ArbitrageOpportunity>> getOpportunities(
        @RequestParam(defaultValue = "5.0") double minAnnualReturn,
        @RequestParam(required = false) String symbolFilter
    );
    
    @GetMapping("/refresh")
    public ResponseEntity<String> refreshData();
}

public class ArbitrageService {
    public List<ArbitrageOpportunity> calculateArbitrageOpportunities();
    private double calculateAnnualReturn(FutureData data);
    private int calculateHoldingDays(LocalDate now, LocalDate expiry);
}
```

#### Frontend Changes:
- **Component**: `ArbitrageOpportunityComponent`
- **Service**: `ArbitrageService`
- **Models**: `ArbitrageOpportunity` interface
- **Table**: Angular Material Table with pagination
- **Real-time**: WebSocket or polling for updates

#### Frontend Structure:
```typescript
export interface ArbitrageOpportunity {
  companyCode: string;
  currentDateTime: string;
  selectedExpiry: string;
  featurePriceL: number;
  featurePriceC: number;
  spotPrice: number;
  lotSize: number;
  holdingDays: number;
  priceDifference: number;
  pctPriceDifference: number;
  futuresInvestment: number;
  spotInvestment: number;
  totalInvestment: number;
  totalProfit: number;
  perDayReturn: number;
  perAnnumReturn: number;
}
```

### 5. Integration Points

#### Navigation:
- Add to main menu: **Arbitrage Opportunity**
- Route: `/arbitrage-opportunity`
- Icon: `trending_up` or `analytics`

#### API Integration:
- Leverage existing Groww API service
- Use same authentication/headers
- Handle rate limiting with caching

#### Styling:
- Match existing Material Design theme
- Use same color scheme as Watchlist/Live Stock
- Responsive design for mobile/tablet

### 6. Performance Considerations

#### Backend:
- **Batch Processing**: Calculate multiple symbols concurrently
- **Caching**: Cache results for 2 minutes
- **Async Processing**: Use @Async for heavy calculations
- **Rate Limiting**: Respect Groww API limits

#### Frontend:
- **Lazy Loading**: Pagination (50 rows per page)
- **Debounced Filters**: 300ms delay on search
- **Virtual Scrolling**: For large datasets
- **Loading States**: Skeleton loaders during API calls

### 7. Data Flow Architecture

```
Groww API → ArbitrageService → ArbitrageController → Angular Service → Component UI
     ↓                ↓                ↓                    ↓              ↓
  Raw Data    →   Calculations   →   JSON Response   →   TypeScript   →   Table Display
```

### 8. Expected Deliverables

#### Backend:
1. `ArbitrageController.java` - REST endpoints
2. `ArbitrageService.java` - Business logic
3. `ArbitrageOpportunity.java` - Entity/DTO
4. Integration tests for calculations

#### Frontend:
1. `arbitrage-opportunity/` component folder
2. `arbitrage-opportunity.component.ts/html/scss`
3. `arbitrage.service.ts` - API client
4. Routing configuration
5. Unit tests

#### Integration:
1. Menu navigation updates
2. API endpoint testing
3. Cross-browser compatibility
4. Mobile responsiveness

### 9. Success Criteria

#### Accuracy:
- ✅ Calculations match existing Excel output exactly
- ✅ Same filtering logic (5% minimum, positive price diff)
- ✅ Proper margin and cost calculations

#### Performance:
- ✅ Initial load < 3 seconds for 100+ opportunities
- ✅ Refresh < 2 seconds
- ✅ Smooth filtering and sorting

#### Usability:
- ✅ Intuitive filter controls
- ✅ Clear visual indicators for opportunities
- ✅ Responsive design

#### Integration:
- ✅ Seamless navigation from main menu
- ✅ Consistent UI/UX with existing screens
- ✅ Proper error handling and loading states

### 10. Testing Strategy

#### Unit Tests:
- Arbitrage calculation accuracy
- Edge cases (zero prices, negative values)
- Filter and sort logic

#### Integration Tests:
- API endpoint responses
- Groww API integration
- Error handling scenarios

#### E2E Tests:
- Complete user workflows
- Filter/sort interactions
- Export functionality

### 11. Future Enhancements

#### Phase 2 Features:
- **Detailed View**: Click row for breakdown analysis
- **Alert System**: Notifications for high-return opportunities
- **Historical Data**: Track arbitrage opportunities over time
- **Portfolio Integration**: Track actual arbitrage positions

#### Advanced Analytics:
- **Market Conditions**: Correlate with market volatility
- **Risk Metrics**: Add risk-adjusted return calculations
- **Optimization**: Suggest optimal position sizes

### 12. Reference Implementation Details

#### Calculation Logic (from reference code):
```java
// Exact implementation to port:
int holdingDays = holdingDays(fd.currentDateTime, fd.selectedExpiry);
double priceDiff = fd.featurePriceL - fd.spotPrice;
if (priceDiff <= 0) continue; // Only positive differences

double pctPriceDiff = (fd.spotPrice == 0) ? 0.0 : (priceDiff / (fd.spotPrice / 100.0));
double futInv = fd.featurePriceL * fd.lotSize;
double spotInv = fd.spotPrice * fd.lotSize;
double totalInv = spotInv + futInv * FUTURES_MARGIN_FRACTION;
double totalProfit = (priceDiff * fd.lotSize) - FIXED_COST;
double perDay = (holdingDays > 0) ? (totalProfit / holdingDays) : 0.0;
double perAnnumPct = (totalInv == 0) ? 0.0 : ((perDay * 365.0) / (totalInv / 100.0));

if (perAnnumPct >= 5) {
    // Add to results table
}
```

#### Data Sources:
- **Spot Prices**: Groww API `/api/stock/search` for underlying stocks
- **Futures Prices**: Groww API with FNO segment for futures contracts
- **Lot Sizes**: From instruments database or Groww API
- **Expiry Dates**: From futures contract symbols

---

## Implementation Priority

### Phase 1 (MVP):
1. Basic backend API with calculations
2. Simple Angular table display
3. Manual refresh functionality
4. Basic filtering (minimum return %)

### Phase 2 (Enhanced):
1. Real-time updates
2. Advanced filtering and sorting
3. Export functionality
4. Detailed row view

### Phase 3 (Advanced):
1. Alert system
2. Historical tracking
3. Portfolio integration
4. Advanced analytics

This prompt provides a comprehensive roadmap for transforming the Excel-based arbitrage detection system into a modern, real-time web application while maintaining calculation accuracy and adding enhanced user experience features.
