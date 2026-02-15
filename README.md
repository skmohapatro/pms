# Investment Portfolio Application

A full-stack investment portfolio management application with Angular frontend and Spring Boot backend.

## Features

- **Excel Upload**: Import holdings from "Purchase Date Wise" worksheet
- **CRUD Operations**: Manage purchase transactions
- **Company-wise Aggregation**: View aggregated data with weighted average cost
- **Group Management**: Create groups and assign instruments
- **Time-based Analytics**: Monthly and yearly investment charts with filters

## Technology Stack

### Backend
- Spring Boot 3.2.2
- Java 17
- H2 Database (file-based persistent storage)
- Apache POI for Excel parsing
- Maven

### Frontend
- Angular 16
- Angular Material
- ng2-charts (Chart.js)
- TypeScript

## Quick Start

### Prerequisites
- Java 17 or higher
- Node.js and npm
- Maven

### Single-Click Startup

**Windows:**
```bash
start-servers.bat
```

This will:
1. Start the backend server on port 8080
2. Wait 10 seconds for backend initialization
3. Start the frontend server on port 4200
4. Open the application in your default browser

### Manual Startup

**Backend:**
```bash
cd backend
mvn clean package -DskipTests
java -jar target/portfolio-1.0.0.jar
```

**Frontend:**
```bash
cd frontend
npm install
ng serve --open
```

### Stopping Servers

**Windows:**
```bash
stop-servers.bat
```

Or manually close the terminal windows.

## Application URLs

- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080/api
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/portfoliodb`
  - Username: `sa`
  - Password: (leave empty)

## Database Storage

The H2 database is configured for **persistent file-based storage**:
- Database files are stored in: `backend/data/`
- Data persists across server restarts
- Files: `portfoliodb.mv.db` and `portfoliodb.trace.db`

## Excel File Format

The application expects an Excel file with a worksheet named **"Purchase Date Wise"** containing:

| Column | Type | Description |
|--------|------|-------------|
| Date | Date | Purchase date |
| Company | String | Company/Instrument name |
| Quantity | Number | Number of units |
| Price | Number | Price per unit |
| Investment | Number | Total investment amount |

## API Endpoints

### Upload
- `POST /api/upload` - Upload Excel file

### Purchases
- `GET /api/purchases` - Get all purchases
- `GET /api/purchases/{id}` - Get purchase by ID
- `POST /api/purchases` - Create new purchase
- `PUT /api/purchases/{id}` - Update purchase
- `DELETE /api/purchases/{id}` - Delete purchase
- `GET /api/purchases/companies` - Get distinct companies

### Company-wise Data
- `GET /api/company-wise` - Get aggregated company-wise data

### Groups
- `GET /api/groups` - Get all groups
- `POST /api/groups` - Create new group
- `DELETE /api/groups/{id}` - Delete group
- `GET /api/groups/{id}/detail` - Get group details
- `POST /api/groups/{id}/instruments` - Assign instruments to group

### Analytics
- `GET /api/analytics/monthly` - Get monthly investment data
- `GET /api/analytics/yearly` - Get yearly investment data

Query parameters for analytics:
- `startDate` (optional): Filter start date (yyyy-MM-dd)
- `endDate` (optional): Filter end date (yyyy-MM-dd)
- `company` (optional): Filter by company name

## Development

### Backend Build
```bash
cd backend
mvn clean install
```

### Frontend Build
```bash
cd frontend
ng build
```

Production build output: `frontend/dist/frontend/`

## Troubleshooting

### Port Already in Use
- Backend (8080): Stop any running Java processes
- Frontend (4200): Stop any running Node/Angular processes

### Database Issues
- Delete `backend/data/` folder to reset the database
- Check H2 Console for direct database access

### Excel Upload Fails
- Ensure worksheet is named "Purchase Date Wise" (case-insensitive)
- Verify all required columns are present
- Check date format in Excel

## License

Private project for personal investment tracking.
