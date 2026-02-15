## Polished Requirement (with clarifications)

### 1) Goal
Build a small portfolio/holdings application with:

- **Frontend**: Angular UI
- **Backend**: Spring Boot REST API
- **Database**: In-memory database such as **H2** (data resets on restart unless later changed)

The application ingests an Excel file and provides CRUD screens and aggregated analytics views.

---

## 2) Excel Upload & Parsing

### 2.1 Upload
- UI must allow uploading an **Excel file** (`.xlsx`)
- Backend must parse the file and read **only** the worksheet named: **`Purchase Date Wise`**.

### 2.2 Source columns (from “Purchase Date Wise” sheet)
Expected columns (case/spacing tolerant if possible):

- `Date`
- `Company`
- `Quantity`
- `Price`
- `Investment`

### 2.3 Validation rules (recommended)
- Reject/flag rows with missing required values.
- Convert `Date` reliably (Excel date types + string formats).
- Ensure numeric fields are numeric (`Quantity`, `Price`, `Investment`).

---

## 3) Persistence: Purchase Date Wise (Raw Transactions)

### 3.1 Table and Entity
- Persist parsed rows into an in-memory DB table (entity) representing purchase transactions.
- **Table name**: you wrote *“Purchase Date Wise”*; technically DB table names with spaces are awkward.
  - Recommendation: physical table name `purchase_date_wise` (and UI label “Purchase Date Wise”).

### 3.2 Behavior
After upload:
- Clear + replace existing data OR append? (needs your choice; see questions below)
- Store each transaction row as a record.

### 3.3 UI screen (CRUD)
Provide a grid/table UI for **Purchase Date Wise** with:
- **Create** a transaction
- **Edit** a transaction
- **Delete** a transaction
- **View/search/sort** (recommended)

Any CRUD change should update aggregation outputs (see section 4).

---

## 4) Derived Aggregation: Company Wise Aggregated Data

### 4.1 Aggregation rule
Once “Purchase Date Wise” data is stored, backend must generate and store an aggregated view grouped by `Company` (Instrument).

- **Target table**: “Company Wise Aggregated Data”
  - Recommendation physical table name: `company_wise_aggregated_data`

Expected columns (matching your Excel reference sheet):
- `Instrument` (Company name)
- `Qty` (sum of quantity)
- `Avg. cost` (definition needed; usually weighted avg)
- `Invested` (sum of investment)

### 4.2 Notes / Clarifications needed
- **Avg. cost** should typically be:
  `total_investment / total_quantity` (weighted average cost)


### 4.3 UI screen (read/optional edit)
Provide UI to view aggregated company-wise data.
- This grid is **read-only** 

---

## 5) Stock Grouping / Tagging (Many-to-Many)

### 5.1 Group management
From “Company Wise Aggregated Data” UI, allow defining **Groups** such as:
- `ETF`
- `Small Cap`
- `Nifty50`
- `Large Cap`
(…user-defined)

### 5.2 Assignment rules
- A **stock/instrument can belong to multiple groups** (many-to-many).
- Group definitions and assignments must be persisted in the in-memory DB:
  - `groups` table
  - `instrument_group_map` table (or similar)

### 5.3 Group view
When a user selects a group:
- show list of instruments in that group with:
  - aggregated `Qty`
  - aggregated `Total Investment` (sum invested for instruments in that group)
- (Optional) also show derived totals for the group (total invested across all instruments, etc.)

---

## 6) Time-based Analytics from Purchase Date Wise

Provide UI views/charts/tables derived from Purchase Date Wise, showing:

- **Monthly investment aggregation**: sum of `Investment` grouped by month (and year)
- **Yearly investment aggregation**: sum of `Investment` grouped by year

Recommended:
- Filters: date range, company, group
- Chart + table view (mandatory)

---

# Open Questions (I need your input on these)

## A) Upload behavior
- **A1**: On upload, should it **replace** existing Purchase Date Wise data 
and resync all other dependent tables.


## C) Avg. cost formula
- **C1**: Confirm `Avg. cost = total_invested / total_qty` (weighted average).
 

## D) CRUD impact on aggregation
- **D1**: When Purchase Date Wise CRUD changes happen, it should update the dependent table accordingly.

## E) Naming conventions
- **E1**: Are you okay with DB table names like `purchase_date_wise` while UI labels keep your friendly names? yes
