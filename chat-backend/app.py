"""Flask backend for Dell GenAI Chat Application."""
import os
import re
import uuid
import logging
import requests
from flask import Flask, request, jsonify, Response, stream_with_context
from flask_cors import CORS
from dotenv import load_dotenv
import json

try:
    from aia_auth import auth
    AIA_AUTH_AVAILABLE = True
except ImportError:
    AIA_AUTH_AVAILABLE = False
    print("Warning: aia_auth module not available. Using mock authentication.")

load_dotenv('.env', override=True)

app = Flask(__name__)
CORS(app)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

USE_SSO = os.getenv("USE_SSO", "true").lower() == "true"
CLIENT_ID = os.getenv("CLIENT_ID")
CLIENT_SECRET = os.getenv("CLIENT_SECRET")
DELL_GENAI_BASE_URL = "https://aia.gateway.dell.com/genai/dev/v1"
DEFAULT_MODEL = "pixtral-12b-2409"

GROWW_API_KEY = os.getenv("groww_api_key")
GROWW_API_SECRET = os.getenv("groww_api_secret")
JAVA_BACKEND_URL = os.getenv("JAVA_BACKEND_URL", "http://localhost:8080")

groww_client = None
GrowwAPI = None
try:
    from growwapi import GrowwAPI as GrowwAPIClass
    GrowwAPI = GrowwAPIClass
    if GROWW_API_KEY and GROWW_API_SECRET:
        try:
            access_token = GrowwAPIClass.get_access_token(api_key=GROWW_API_KEY, secret=GROWW_API_SECRET)
            groww_client = GrowwAPIClass(access_token)
            logger.info("Groww API client initialized successfully")
        except Exception as auth_error:
            logger.error(f"Groww API authentication failed: {auth_error}")
    else:
        logger.warning("Groww API key or secret not found in environment")
except ImportError:
    logger.warning("growwapi package not installed. Live stock data will not be available.")

AVAILABLE_MODELS = [
    "gemma-3-27b-it",
    "pixtral-12b-2409"
]


def get_correlation_id():
    return str(uuid.uuid4())


def get_auth_token():
    """Get authentication token based on configuration."""
    if not AIA_AUTH_AVAILABLE:
        return "mock-token-for-testing"
    
    try:
        if USE_SSO:
            token = auth.sso()
            return token.token
        else:
            if not CLIENT_ID or not CLIENT_SECRET:
                raise ValueError("CLIENT_ID and CLIENT_SECRET must be set when USE_SSO is false")
            token = auth.client_credentials(CLIENT_ID, CLIENT_SECRET)
            return token.token
    except Exception as e:
        logger.error(f"Authentication error: {e}")
        raise


def call_dell_genai(messages, model=DEFAULT_MODEL, stream=False, temperature=0.7, max_tokens=2000):
    """Call Dell GenAI API with proper authentication."""
    import requests
    import certifi
    
    url = f"{DELL_GENAI_BASE_URL}/chat/completions"
    
    try:
        token = get_auth_token()
        headers = {
            "Authorization": f"Bearer {token}",
            "x-correlation-id": get_correlation_id(),
            "Content-Type": "application/json",
            "accept": "*/*"
        }
        
        payload = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
            "stream": stream
        }
        
        logger.info(f"Calling Dell GenAI API with model: {model}, stream: {stream}")
        
        response = requests.post(
            url,
            headers=headers,
            json=payload,
            stream=stream,
            verify=certifi.where(),
            timeout=60
        )
        
        response.raise_for_status()
        return response
        
    except Exception as e:
        logger.error(f"Error calling Dell GenAI: {e}")
        raise


@app.route('/api/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    return jsonify({
        "status": "healthy",
        "use_sso": USE_SSO,
        "aia_auth_available": AIA_AUTH_AVAILABLE,
        "base_url": DELL_GENAI_BASE_URL
    })


@app.route('/api/models', methods=['GET'])
def get_models():
    """Get available models."""
    return jsonify({
        "models": AVAILABLE_MODELS,
        "default": DEFAULT_MODEL
    })


@app.route('/api/chat', methods=['POST'])
def chat():
    """Chat endpoint - non-streaming."""
    try:
        data = request.json
        messages = data.get('messages', [])
        model = data.get('model', DEFAULT_MODEL)
        temperature = data.get('temperature', 0.7)
        max_tokens = data.get('max_tokens', 2000)
        
        if not messages:
            return jsonify({"error": "No messages provided"}), 400
        
        response = call_dell_genai(
            messages=messages,
            model=model,
            stream=False,
            temperature=temperature,
            max_tokens=max_tokens
        )
        
        result = response.json()
        
        return jsonify({
            "message": result['choices'][0]['message']['content'],
            "model": model,
            "finish_reason": result['choices'][0].get('finish_reason')
        })
        
    except Exception as e:
        logger.error(f"Chat error: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/api/chat/stream', methods=['POST'])
def chat_stream():
    """Chat endpoint - streaming."""
    try:
        data = request.json
        messages = data.get('messages', [])
        model = data.get('model', DEFAULT_MODEL)
        temperature = data.get('temperature', 0.7)
        max_tokens = data.get('max_tokens', 2000)
        
        if not messages:
            return jsonify({"error": "No messages provided"}), 400
        
        def generate():
            try:
                response = call_dell_genai(
                    messages=messages,
                    model=model,
                    stream=True,
                    temperature=temperature,
                    max_tokens=max_tokens
                )
                
                for line in response.iter_lines():
                    if line:
                        decoded_line = line.decode('utf-8')
                        
                        if decoded_line == "data: [DONE]":
                            yield f"data: {json.dumps({'done': True})}\n\n"
                            break
                        
                        if decoded_line.startswith("data: "):
                            try:
                                json_str = decoded_line[6:]
                                chunk_data = json.loads(json_str)
                                
                                if 'choices' in chunk_data and len(chunk_data['choices']) > 0:
                                    delta = chunk_data['choices'][0].get('delta', {})
                                    content = delta.get('content', '')
                                    
                                    if content:
                                        yield f"data: {json.dumps({'content': content})}\n\n"
                                        
                            except json.JSONDecodeError:
                                continue
                                
            except Exception as e:
                logger.error(f"Streaming error: {e}")
                yield f"data: {json.dumps({'error': str(e)})}\n\n"
        
        return Response(
            stream_with_context(generate()),
            mimetype='text/event-stream',
            headers={
                'Cache-Control': 'no-cache',
                'X-Accel-Buffering': 'no'
            }
        )
        
    except Exception as e:
        logger.error(f"Chat stream error: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/api/stock/quote', methods=['GET'])
def get_stock_quote():
    """Get detailed quote for a stock symbol."""
    if not groww_client:
        return jsonify({"error": "Groww API not configured"}), 503
    
    symbol = request.args.get('symbol', '').upper()
    exchange = request.args.get('exchange', 'NSE').upper()
    
    if not symbol:
        return jsonify({"error": "Symbol parameter is required"}), 400
    
    try:
        exchange_val = groww_client.EXCHANGE_NSE if exchange == 'NSE' else groww_client.EXCHANGE_BSE
        
        quote = groww_client.get_quote(
            exchange=exchange_val,
            segment=groww_client.SEGMENT_CASH,
            trading_symbol=symbol
        )
        
        return jsonify({
            "symbol": symbol,
            "exchange": exchange,
            "data": quote
        })
    except Exception as e:
        logger.error(f"Error fetching quote for {symbol}: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/api/stock/ltp', methods=['GET'])
def get_stock_ltp():
    """Get last traded price for one or more stocks."""
    if not groww_client:
        return jsonify({"error": "Groww API not configured"}), 503
    
    symbols = request.args.get('symbols', '')
    
    if not symbols:
        return jsonify({"error": "Symbols parameter is required (comma-separated)"}), 400
    
    try:
        symbol_list = [f"NSE_{s.strip().upper()}" for s in symbols.split(',')]
        
        if len(symbol_list) == 1:
            ltp_data = groww_client.get_ltp(
                segment=groww_client.SEGMENT_CASH,
                exchange_trading_symbols=symbol_list[0]
            )
        else:
            ltp_data = groww_client.get_ltp(
                segment=groww_client.SEGMENT_CASH,
                exchange_trading_symbols=tuple(symbol_list)
            )
        
        return jsonify({
            "data": ltp_data
        })
    except Exception as e:
        logger.error(f"Error fetching LTP: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/api/stock/batch-ltp', methods=['POST'])
def batch_ltp():
    """Batch fetch LTP for multiple symbols across CASH and FNO segments.
    
    Request body:
    {
        "cash": ["SBIN", "MARUTI", ...],
        "fno": ["SBIN26MAYFUT", "MARUTI26APRFUT", ...]
    }
    
    Response:
    {
        "cash": {"SBIN": 650.5, "MARUTI": 12000.0, ...},
        "fno": {"SBIN26MAYFUT": 655.0, ...},
        "errors": {"BADSTOCK": "Not found"}
    }
    """
    if not groww_client:
        return jsonify({"error": "Groww API not configured"}), 503
    
    body = request.get_json(silent=True) or {}
    cash_symbols = body.get("cash", [])
    fno_symbols = body.get("fno", [])
    
    result = {"cash": {}, "fno": {}, "errors": {}}
    
    # Fetch CASH LTPs in batch
    if cash_symbols:
        try:
            symbol_keys = [f"NSE_{s.strip().upper()}" for s in cash_symbols]
            if len(symbol_keys) == 1:
                ltp_data = groww_client.get_ltp(
                    segment=groww_client.SEGMENT_CASH,
                    exchange_trading_symbols=symbol_keys[0]
                )
            else:
                ltp_data = groww_client.get_ltp(
                    segment=groww_client.SEGMENT_CASH,
                    exchange_trading_symbols=tuple(symbol_keys)
                )
            # Parse response: keys are like "NSE_SBIN", values have "last_price"
            if isinstance(ltp_data, dict):
                for key, val in ltp_data.items():
                    symbol = key.replace("NSE_", "") if key.startswith("NSE_") else key
                    if isinstance(val, dict) and val.get("last_price") is not None:
                        result["cash"][symbol] = val["last_price"]
                    elif isinstance(val, (int, float)):
                        result["cash"][symbol] = val
        except Exception as e:
            logger.error(f"Error in batch CASH LTP: {e}")
            result["errors"]["cash_batch"] = str(e)
    
    # Fetch FNO LTPs in batch
    if fno_symbols:
        try:
            symbol_keys = [f"NSE_{s.strip().upper()}" for s in fno_symbols]
            if len(symbol_keys) == 1:
                ltp_data = groww_client.get_ltp(
                    segment=groww_client.SEGMENT_FNO,
                    exchange_trading_symbols=symbol_keys[0]
                )
            else:
                ltp_data = groww_client.get_ltp(
                    segment=groww_client.SEGMENT_FNO,
                    exchange_trading_symbols=tuple(symbol_keys)
                )
            if isinstance(ltp_data, dict):
                for key, val in ltp_data.items():
                    symbol = key.replace("NSE_", "") if key.startswith("NSE_") else key
                    if isinstance(val, dict) and val.get("last_price") is not None:
                        result["fno"][symbol] = val["last_price"]
                    elif isinstance(val, (int, float)):
                        result["fno"][symbol] = val
        except Exception as e:
            logger.error(f"Error in batch FNO LTP: {e}")
            result["errors"]["fno_batch"] = str(e)
    
    return jsonify(result)

@app.route('/api/stock/search', methods=['GET'])
def search_stock():
    """Search for stocks/derivatives by symbol - returns quote data."""
    if not groww_client:
        return jsonify({"error": "Groww API not configured"}), 503
    
    query = request.args.get('q', '').upper()
    
    if not query:
        return jsonify({"error": "Query parameter 'q' is required"}), 400
    
    # Determine segment based on symbol format
    # Derivatives have specific patterns: ends with FUT, or digit+CE/PE (e.g., NIFTY24000CE)
    # Use precise regex to avoid false positives (e.g., MARUTI contains "MAR" but is equity)
    segment = groww_client.SEGMENT_CASH
    
    # Precise FNO detection: symbol must end with FUT, or have digits followed by CE/PE
    # Examples: SBIN26MAYFUT, NIFTY26MAR25FUT, NIFTY24000CE, BANKNIFTY25000PE
    _FNO_PATTERN = re.compile(
        r'(?:'
        r'FUT$'                                     # Ends with FUT (futures)
        r'|'
        r'\d+(?:CE|PE)$'                            # Digits followed by CE/PE (options with strike)
        r'|'
        r'\d{2}(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\d{0,2}(?:CE|PE)$'  # Date-style options
        r')'
    )
    is_derivative = bool(_FNO_PATTERN.search(query))
    
    if is_derivative:
        segment = groww_client.SEGMENT_FNO
    
    # Fetch lot size from instruments database
    lot_size = None
    try:
        # Call backend Java API to get instrument details
        instrument_response = requests.get(f"{JAVA_BACKEND_URL}/api/instruments/symbol/{query}", timeout=5)
        if instrument_response.status_code == 200:
            instrument_data = instrument_response.json()
            lot_size = instrument_data.get("lotSize")
    except Exception as e:
        logger.debug(f"Could not fetch lot size from backend: {e}")
    
    try:
        quote = groww_client.get_quote(
            exchange=groww_client.EXCHANGE_NSE,
            segment=segment,
            trading_symbol=query
        )
        
        result = {
            "symbol": query,
            "exchange": "NSE",
            "segment": "FNO" if segment == groww_client.SEGMENT_FNO else "CASH",
            "last_price": quote.get("last_price"),
            "day_change": quote.get("day_change"),
            "day_change_perc": quote.get("day_change_perc"),
            "ohlc": quote.get("ohlc"),
            "volume": quote.get("volume"),
            "bid_price": quote.get("bid_price"),
            "offer_price": quote.get("offer_price"),
            "week_52_high": quote.get("week_52_high"),
            "week_52_low": quote.get("week_52_low"),
            "upper_circuit_limit": quote.get("upper_circuit_limit"),
            "lower_circuit_limit": quote.get("lower_circuit_limit"),
            "total_buy_quantity": quote.get("total_buy_quantity"),
            "total_sell_quantity": quote.get("total_sell_quantity"),
            "market_cap": quote.get("market_cap"),
            "dividend_yield": quote.get("dividend_yield"),
            # FNO specific fields
            "open_interest": quote.get("open_interest"),
            "oi_day_change": quote.get("oi_day_change"),
            "oi_day_change_percentage": quote.get("oi_day_change_percentage"),
            "implied_volatility": quote.get("implied_volatility"),
            # Instrument metadata
            "lot_size": lot_size
        }
        
        return jsonify(result)
    except Exception as e:
        logger.error(f"Error searching for {query}: {e}")
        return jsonify({"error": f"Could not find stock: {query}. Wrong segment for trading symbol: {query}"}), 404


@app.route('/api/stock/test', methods=['GET'])
def test_stock():
    """Test a specific symbol with raw Groww API response."""
    if not groww_client:
        return jsonify({"error": "Groww API not configured"}), 503
    
    symbol = request.args.get('symbol', '').upper()
    
    if not symbol:
        return jsonify({"error": "Symbol parameter is required"}), 400
    
    # Determine segment using precise pattern matching
    segment = groww_client.SEGMENT_CASH
    _FNO_TEST = re.compile(r'(?:FUT$|\d+(?:CE|PE)$|\d{2}(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\d{0,2}(?:CE|PE)$)')
    if _FNO_TEST.search(symbol):
        segment = groww_client.SEGMENT_FNO
    
    try:
        # Get raw quote
        quote = groww_client.get_quote(
            exchange=groww_client.EXCHANGE_NSE,
            segment=segment,
            trading_symbol=symbol
        )
        
        return jsonify({
            "symbol": symbol,
            "segment": "FNO" if segment == groww_client.SEGMENT_FNO else "CASH",
            "raw_response": quote,
            "available_fields": list(quote.keys()) if isinstance(quote, dict) else "Not a dict"
        })
    except Exception as e:
        return jsonify({
            "error": str(e),
            "symbol": symbol,
            "segment": "FNO" if segment == groww_client.SEGMENT_FNO else "CASH"
        }), 500


@app.route('/api/stock/health', methods=['GET'])
def stock_api_health():
    """Check if Groww API is configured and available."""
    return jsonify({
        "available": groww_client is not None,
        "api_key_configured": GROWW_API_KEY is not None
    })


@app.route('/api/dashboard/insights', methods=['POST'])
def dashboard_insights():
    """Generate AI-powered dashboard insights using RAG."""
    try:
        data = request.json
        portfolio = data.get('portfolio', {})
        watchlist = data.get('watchlist', {})
        news = data.get('news', [])

        holdings = portfolio.get('holdings', [])
        total_invested = portfolio.get('totalInvested', 0)
        total_companies = portfolio.get('totalCompanies', 0)
        wl_instruments = watchlist.get('instruments', [])
        wl_total = watchlist.get('totalTracking', 0)

        # Build news context string
        news_text = ""
        for i, article in enumerate(news[:15], 1):
            cat = article.get('category', 'general')
            news_text += f"{i}. [{cat.upper()}] {article.get('title', '')} - {article.get('source', '')} ({article.get('date', '')})\n"
            if article.get('snippet'):
                news_text += f"   {article['snippet'][:200]}\n"

        prompt = f"""You are a financial analyst for an Indian stock market portfolio. Analyze the following data and provide insights.

PORTFOLIO:
- Holdings: {', '.join(holdings[:20])}
- Total Invested: ₹{total_invested:,.0f}
- Total Companies: {total_companies}

WATCHLIST (stocks being tracked for potential investment):
- Tracking: {', '.join(wl_instruments[:20])}
- Total Instruments: {wl_total}

RECENT NEWS:
{news_text}

Based on this information, provide a JSON response with EXACTLY this structure:
{{
  "marketSummary": "A 2-3 sentence summary of the current Indian market conditions based on the news",
  "portfolioInsights": ["insight1 about portfolio holdings", "insight2", "insight3"],
  "watchlistAlerts": ["alert1 about watchlist stocks", "alert2", "alert3"],
  "riskAlerts": ["risk1", "risk2"],
  "headlines": ["headline1 most relevant to the portfolio", "headline2", "headline3", "headline4", "headline5"]
}}

IMPORTANT: Return ONLY valid JSON, no markdown formatting, no code blocks, no extra text."""

        messages = [{"role": "user", "content": prompt}]

        response = call_dell_genai(
            messages=messages,
            model=DEFAULT_MODEL,
            stream=False,
            temperature=0.5,
            max_tokens=2000
        )

        result = response.json()
        ai_content = result['choices'][0]['message']['content']

        # Try to parse the AI response as JSON
        try:
            # Clean up potential markdown code blocks
            cleaned = ai_content.strip()
            if cleaned.startswith('```'):
                cleaned = cleaned.split('\n', 1)[1] if '\n' in cleaned else cleaned[3:]
            if cleaned.endswith('```'):
                cleaned = cleaned[:-3]
            cleaned = cleaned.strip()
            if cleaned.startswith('json'):
                cleaned = cleaned[4:].strip()

            insights = json.loads(cleaned)
            return jsonify(insights)
        except json.JSONDecodeError:
            logger.warning("AI response was not valid JSON, returning raw content")
            return jsonify({
                "marketSummary": ai_content[:500],
                "portfolioInsights": [],
                "watchlistAlerts": [],
                "riskAlerts": [],
                "headlines": []
            })

    except Exception as e:
        logger.error(f"Dashboard insights error: {e}")
        return jsonify({
            "marketSummary": "Unable to generate insights at this time.",
            "portfolioInsights": [],
            "watchlistAlerts": [],
            "riskAlerts": [],
            "headlines": []
        }), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
