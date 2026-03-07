# Derivative Search Fix - Complete Guide

## Issues Fixed

### 1. **Instrument Search Not Showing Derivatives**
**Problem**: The search was limited to CASH segment only, excluding FNO derivatives.

**Solution**: Updated `InstrumentRepository.searchNseCashInstruments()` to include all NSE instruments (both CASH and FNO), with CASH instruments prioritized in results.

### 2. **Improved Derivative Detection**
**Problem**: Backend was only detecting derivatives with exact keywords like "FUT", "CE", "PE".

**Solution**: Enhanced detection to include:
- Month patterns (JAN, FEB, MAR, etc.)
- Symbols ending with CE or PE
- Date patterns in derivative symbols

## Changes Made

### Backend Java Changes

#### 1. `InstrumentRepository.java`
```java
// OLD - Only searched CASH segment
@Query("SELECT i FROM Instrument i WHERE i.segment = 'CASH' AND i.exchange = 'NSE' ...")

// NEW - Searches all NSE instruments, prioritizes CASH
@Query("SELECT i FROM Instrument i WHERE i.exchange = 'NSE' " +
       "AND (LOWER(i.tradingSymbol) LIKE LOWER(CONCAT('%', :query, '%')) " +
       "OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
       "ORDER BY CASE WHEN i.segment = 'CASH' THEN 0 ELSE 1 END, i.tradingSymbol")
```

Added dedicated FNO search:
```java
@Query("SELECT i FROM Instrument i WHERE i.exchange = 'NSE' AND i.segment = 'FNO' " +
       "AND LOWER(i.tradingSymbol) LIKE LOWER(CONCAT('%', :query, '%'))")
List<Instrument> searchNseFnoInstruments(@Param("query") String query);
```

#### 2. `InstrumentService.java`
Added FNO count to status:
```java
long nseFnoCount = instrumentRepository.countBySegment("FNO");
status.put("nseFnoCount", nseFnoCount);
```

### Backend Python Changes

#### `chat-backend/app.py`
Improved derivative detection:
```python
is_derivative = (
    'FUT' in query or 
    query.endswith('CE') or 
    query.endswith('PE') or
    'CALL' in query or 
    'PUT' in query or
    # Check for date patterns like 26MAR25, 26MAR, JAN, FEB, etc
    any(month in query for month in ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'])
)
```

## How to Use

### Step 1: Ensure Instruments are Loaded
1. Navigate to any screen that uses instruments (Watchlist, Purchases)
2. Check if derivatives are in the database:
   - Go to H2 Console: http://localhost:8080/h2-console
   - Run: `SELECT COUNT(*) FROM INSTRUMENTS WHERE SEGMENT = 'FNO';`
   - If count is 0, you need to refresh instruments

### Step 2: Refresh Instruments (if needed)
If no FNO instruments exist:
1. Call the refresh endpoint: `POST http://localhost:8080/api/instruments/refresh`
2. This will download the latest instruments from Groww (includes ~200k+ instruments)
3. Wait for completion (may take 30-60 seconds)
4. Check status: `GET http://localhost:8080/api/instruments/status`

### Step 3: Search for Derivatives
Now you can search for derivatives in:

#### **Watchlist Screen**
- Type in search box: `NIFTY` - will show both NIFTY stock and NIFTY derivatives
- Type: `NIFTY24MAR` - will show March expiry derivatives
- Type: `BANKNIFTY` - will show BANKNIFTY derivatives

#### **Live Stock Screen**
- Search for: `NIFTY24MAR25FUT` - Nifty March 2025 Future
- Search for: `NIFTY24000CE` - Nifty 24000 Call Option
- Search for: `BANKNIFTY50000PE` - Bank Nifty 50000 Put Option

#### **Purchases Screen**
- When adding a transaction, the company field will now suggest derivatives
- Type `NIFTY` and you'll see futures and options in the dropdown

## Derivative Symbol Format

### Futures
- **Format**: `{UNDERLYING}{DDMMMYY}FUT`
- **Examples**: 
  - `NIFTY26MAR25FUT`
  - `BANKNIFTY26MAR25FUT`
  - `RELIANCE26MAR25FUT`

### Call Options
- **Format**: `{UNDERLYING}{DDMMMYY}{STRIKE}CE` or `{UNDERLYING}{STRIKE}CE`
- **Examples**:
  - `NIFTY26MAR2524000CE`
  - `NIFTY24000CE`
  - `BANKNIFTY50000CE`

### Put Options
- **Format**: `{UNDERLYING}{DDMMMYY}{STRIKE}PE` or `{UNDERLYING}{STRIKE}PE`
- **Examples**:
  - `NIFTY26MAR2524000PE`
  - `NIFTY24000PE`
  - `BANKNIFTY50000PE`

## Verifying the Fix

### Test 1: Check Instrument Count
```sql
-- In H2 Console
SELECT SEGMENT, COUNT(*) FROM INSTRUMENTS GROUP BY SEGMENT;
```
Expected output:
- CASH: ~2000-3000 instruments
- FNO: ~200,000+ instruments

### Test 2: Search for Derivatives
```sql
-- Search for NIFTY derivatives
SELECT * FROM INSTRUMENTS 
WHERE TRADING_SYMBOL LIKE '%NIFTY%' 
AND SEGMENT = 'FNO' 
LIMIT 10;
```

### Test 3: API Test
```bash
# Search via API
curl "http://localhost:8080/api/instruments/search?q=NIFTY"

# Check status
curl "http://localhost:8080/api/instruments/status"
```

### Test 4: Live Stock Test
1. Go to Live Stock screen
2. Search for: `NIFTY26MAR25FUT`
3. Should see:
   - Segment badge showing "FNO"
   - Live price data
   - Derivatives Data section with OI and IV

## Troubleshooting

### Issue: "No derivatives found in search"
**Solution**: 
1. Refresh instruments: `POST http://localhost:8080/api/instruments/refresh`
2. Wait for completion
3. Verify FNO count in status endpoint

### Issue: "Wrong segment for trading symbol"
**Causes**:
1. Symbol format is incorrect
2. Derivative contract doesn't exist
3. Expiry date has passed
4. Symbol not in Groww's database

**Solution**:
- Check exact symbol format in instruments table
- Use current month/year for expiry
- Verify symbol exists: `SELECT * FROM INSTRUMENTS WHERE TRADING_SYMBOL = 'YOUR_SYMBOL';`

### Issue: "Derivatives not appearing in autocomplete"
**Solution**:
1. Restart backend server (port 8080)
2. Clear browser cache
3. Verify instruments are loaded
4. Check that search includes FNO segment

## Important Notes

1. **Restart Required**: After making these changes, restart:
   - Backend Java server (port 8080)
   - Chat backend Python server (port 5000)

2. **Instrument Refresh**: The Groww instruments CSV is updated regularly. Refresh periodically to get latest derivatives.

3. **Search Performance**: With 200k+ instruments, searches may take 1-2 seconds. Results are limited to prevent overwhelming the UI.

4. **Segment Priority**: Search results prioritize CASH instruments first, then FNO. This ensures stocks appear before their derivatives.

5. **Data Source**: All instrument data comes from Groww's official CSV: https://growwapi-assets.groww.in/instruments/instrument.csv

## Next Steps

To fully utilize derivatives:
1. Refresh instruments to get latest FNO contracts
2. Test search in Watchlist screen
3. Try adding a derivative to your portfolio via Purchases
4. Use Live Stock screen to monitor derivative prices
5. Check Open Interest and Implied Volatility data

## API Endpoints Summary

- `GET /api/instruments/search?q={query}` - Search all NSE instruments (CASH + FNO)
- `GET /api/instruments/status` - Check instrument counts by segment
- `POST /api/instruments/refresh` - Refresh from Groww CSV
- `GET /api/stock/search?q={symbol}` - Get live quote (auto-detects segment)
