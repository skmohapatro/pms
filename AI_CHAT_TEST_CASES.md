# AI Chat Feature - Test Cases

This document outlines test cases for the AI-powered Portfolio Assistant chat feature.

## Prerequisites

Before testing, ensure:
1. Backend is running on port 8080
2. Chat-backend is running on port 5000
3. Frontend is running on port 4200
4. You have some portfolio data uploaded

---

## Test Cases

### TC-01: Basic Portfolio Queries

| ID | Query | Expected Response |
|----|-------|-------------------|
| 1.1 | "What is my total investment?" | Returns the sum of all investments with currency formatting |
| 1.2 | "How many companies do I own?" | Returns count of unique companies in portfolio |
| 1.3 | "What is my total quantity of shares?" | Returns sum of all share quantities |
| 1.4 | "List all my holdings" | Returns list of all companies with quantities and values |

### TC-02: Company-Specific Queries

| ID | Query | Expected Response |
|----|-------|-------------------|
| 2.1 | "How much did I invest in [Company Name]?" | Returns total investment in specified company |
| 2.2 | "What is my average cost for [Company Name]?" | Returns calculated average purchase price |
| 2.3 | "How many shares of [Company Name] do I own?" | Returns quantity of shares for that company |
| 2.4 | "When did I last buy [Company Name]?" | Returns most recent purchase date |

### TC-03: Analytical Queries

| ID | Query | Expected Response |
|----|-------|-------------------|
| 3.1 | "Which company has the highest investment?" | Returns company name with highest total investment |
| 3.2 | "Show my top 5 holdings by value" | Returns ranked list of top 5 investments |
| 3.3 | "What percentage of my portfolio is in [Company]?" | Returns percentage calculation |
| 3.4 | "Compare my investments in [Company A] vs [Company B]" | Returns comparative analysis |

### TC-04: Time-Based Queries

| ID | Query | Expected Response |
|----|-------|-------------------|
| 4.1 | "What did I buy this month?" | Returns purchases from current month |
| 4.2 | "How much did I invest in 2024?" | Returns yearly investment total |
| 4.3 | "Show my purchase history" | Returns chronological list of purchases |
| 4.4 | "What was my biggest single purchase?" | Returns details of largest single transaction |

### TC-05: Group-Related Queries

| ID | Query | Expected Response |
|----|-------|-------------------|
| 5.1 | "What groups do I have?" | Lists all investment groups |
| 5.2 | "Which companies are in [Group Name]?" | Lists companies in specified group |
| 5.3 | "What is the total investment in [Group Name]?" | Returns group's total investment |
| 5.4 | "Which group has the most companies?" | Returns group with highest company count |

### TC-06: Calculation Queries

| ID | Query | Expected Response |
|----|-------|-------------------|
| 6.1 | "What is my average investment per company?" | Calculates and returns average |
| 6.2 | "If I sold everything at current prices, what would I have?" | Requires current price data - should explain limitation |
| 6.3 | "What is my most diversified group?" | Analyzes groups and returns most diversified |

### TC-07: Edge Cases

| ID | Query | Expected Response |
|----|-------|-------------------|
| 7.1 | Empty message | Error: "Message cannot be empty" |
| 7.2 | Query about non-existent company | Gracefully handles and suggests similar companies |
| 7.3 | Very long query (1000+ chars) | Processes normally or gracefully truncates |
| 7.4 | Special characters in query | Handles without errors |
| 7.5 | Query when no data exists | Explains no data available, suggests uploading |

### TC-08: Conversation Context

| ID | Query | Expected Response |
|----|-------|-------------------|
| 8.1 | Follow-up: "Tell me more about that" | Uses conversation history for context |
| 8.2 | "Compare that to my second largest holding" | References previous response |
| 8.3 | Multi-turn conversation about same topic | Maintains context throughout |

### TC-09: Model Selection

| ID | Action | Expected Result |
|----|--------|-----------------|
| 9.1 | Change model to GPT-4o | Response uses GPT-4o model |
| 9.2 | Change model to Claude | Response uses Claude model |
| 9.3 | Model dropdown shows available options | All configured models listed |

### TC-10: UI/UX Tests

| ID | Action | Expected Result |
|----|--------|-----------------|
| 10.1 | Send message | Loading spinner appears |
| 10.2 | Receive response | Message appears with timestamp |
| 10.3 | Click suggestion chip | Query auto-populates and sends |
| 10.4 | Click clear chat | All messages cleared, welcome message shown |
| 10.5 | Long response | Scrolls to show new content |
| 10.6 | Enter key in input | Sends message |
| 10.7 | Shift+Enter | Creates new line (doesn't send) |

---

## Dynamic Context Tests

### TC-11: Auto-Discovery of New Tables

When new tables are added to the database:

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| 11.1 | Add new entity class to backend | Chat context automatically includes new table schema |
| 11.2 | New table has data | Data from new table appears in AI context |
| 11.3 | Query about new table's data | AI can answer questions about new data |

**How to verify:**
1. Add a new JPA entity to the backend
2. Restart the backend service
3. Ask the AI about the new table: "What data do you have about [new table]?"
4. AI should describe the schema and data

---

## Error Handling Tests

### TC-12: Service Unavailability

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| 12.1 | Chat-backend is down | Error message: "Error communicating with AI service" |
| 12.2 | Backend is down | Frontend shows connection error |
| 12.3 | Network timeout | Graceful timeout message |
| 12.4 | Invalid API credentials | Authentication error message |

---

## Performance Tests

### TC-13: Response Times

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| 13.1 | Simple query | Response within 5 seconds |
| 13.2 | Complex analytical query | Response within 15 seconds |
| 13.3 | Large dataset (1000+ records) | Context builds successfully |

---

## Sample Test Session

```
User: What is my total investment?
AI: Your total investment across all holdings is ₹1,234,567.89 spread across 25 different companies.

User: Which company has the highest investment?
AI: Your highest investment is in **Reliance Industries** with a total of ₹245,000 (approximately 19.8% of your portfolio).

User: How many shares of that company do I own?
AI: You own 150 shares of Reliance Industries at an average cost of ₹1,633.33 per share.

User: When did I buy them?
AI: Your Reliance Industries purchases:
- 50 shares on 2024-01-15 at ₹1,500
- 100 shares on 2024-03-20 at ₹1,700

User: Show me my top 5 holdings
AI: Your top 5 holdings by investment value:
1. **Reliance Industries** - ₹245,000 (150 shares)
2. **HDFC Bank** - ₹180,000 (200 shares)
3. **TCS** - ₹150,000 (45 shares)
4. **Infosys** - ₹120,000 (80 shares)
5. **ICICI Bank** - ₹95,000 (100 shares)
```

---

## Regression Testing

After any code changes, run:
1. All TC-01 through TC-06 (core functionality)
2. TC-07 edge cases
3. TC-11 dynamic context verification
4. TC-12 error handling

---

## Notes

- The AI context automatically includes all JPA entities and their data
- Maximum of 100 rows per table are included in context to avoid token limits
- Conversation history is limited to last 10 messages
- The system prompt instructs the AI to format currency and be precise with numbers
