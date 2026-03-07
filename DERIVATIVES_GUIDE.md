# Derivatives Support in Live Stock Screen

## Problem
The Live Stock screen was not showing derivative data because the backend was hardcoded to use `SEGMENT_CASH` for all queries, which only works for equity stocks.

## Solution
Updated the backend to automatically detect and handle derivatives (Futures & Options) by:

1. **Auto-detecting segment** based on symbol format
2. **Using SEGMENT_FNO** for derivatives
3. **Returning FNO-specific data** fields

## How It Works

### Backend Changes (`chat-backend/app.py`)
The `/api/stock/search` endpoint now:
- Detects if a symbol contains derivative keywords: `FUT`, `CE`, `PE`, `CALL`, `PUT`
- Uses `SEGMENT_FNO` for derivatives, `SEGMENT_CASH` for stocks
- Returns additional FNO fields: `open_interest`, `oi_day_change`, `oi_day_change_percentage`, `implied_volatility`

### Frontend Changes
1. **Updated interface** to include FNO fields
2. **Added segment badge** to show CASH or FNO
3. **Added Derivatives Data section** that displays:
   - Open Interest
   - OI Change (with percentage)
   - Implied Volatility

## Derivative Symbol Format

According to Groww API, derivatives use specific naming conventions:

### Futures
- Format: `{SYMBOL}{DATE}FUT`
- Example: `NIFTY26MAR25FUT`, `BANKNIFTY26MAR25FUT`

### Options (Call)
- Format: `{SYMBOL}{DATE}C{STRIKE}`
- Example: `NIFTY26MAR25C24000`, `BANKNIFTY26MAR25C50000`

### Options (Put)
- Format: `{SYMBOL}{DATE}P{STRIKE}`
- Example: `NIFTY26MAR25P24000`, `BANKNIFTY26MAR25P50000`

## Testing

To test derivative support:

1. **Start the chat backend**: `python app.py` (port 5000)
2. **Navigate to Live Stock screen**
3. **Search for a derivative**:
   - Try: `NIFTY26MAR25FUT`
   - Try: `BANKNIFTY26MAR25C50000`
   - Try: `NIFTY26MAR25P24000`

You should see:
- Segment badge showing "FNO"
- Standard stock data (LTP, OHLC, Volume, etc.)
- Additional "Derivatives Data" section with OI and IV

## Error Message
If you see "Wrong segment for trading symbol", it means:
- The symbol format is incorrect
- The derivative contract doesn't exist
- The expiry date has passed

## Groww API Documentation
- Main docs: https://groww.in/trade-api/docs/python-sdk
- Live Data: https://groww.in/trade-api/docs/python-sdk/live-data
- Segments: Use `SEGMENT_FNO` for derivatives, `SEGMENT_CASH` for stocks
- Exchanges: `EXCHANGE_NSE` or `EXCHANGE_BSE`

## Additional Features Available

The Groww API also supports:
- **Option Chain**: `get_option_chain()` - Get all strikes for an underlying
- **Greeks**: Delta, Gamma, Theta, Vega for options
- **LTP Streaming**: Real-time price updates via WebSocket

These can be added to the application if needed.
