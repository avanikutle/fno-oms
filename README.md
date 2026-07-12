# FnO OMS — Options Order Management System

> A professional, dark-themed trading terminal for F&O options traders.  
> Built on **Java 25 · Tomcat 11 · Jakarta EE 10 · PostgreSQL + TimescaleDB · mStock Broker API**.

---

## 📸 Features

| Feature | Description |
|---------|-------------|
| **Live Watchlist** | Real-time quotes via mStock API, auto-refreshes every 2s |
| **Order Management** | Place, cancel, and modify F&O orders with BUY/SELL toggle |
| **Portfolio** | Net positions and holdings with live P&L |
| **Connectivity Check** | Parallel latency test for all configured brokers |
| **Settings** | Manage multiple broker API keys and JWT tokens |
| **Async Event Bus** | Non-blocking order/audit/tick persistence via `LinkedBlockingQueue` |
| **TimescaleDB** | Hypertable for high-frequency tick data storage |

---

## Architecture of App

```
Browser (SPA)
  dashboard.jsp  <-  6 page JSP fragments (included)
  app.js · quotes.js · orders.js · portfolio.js · connectivity.js
        |
        | HTTP REST
        v
Tomcat 11 (Jakarta Servlets)
  DashboardServlet · OrderServlet · QuoteServlet
  PortfolioServlet · BrokerConfigServlet
  ConnectivityServlet · AuthServlet
        |                        |
        v                        v
BrokerClient SPI         Async Event Bus Layer
  mStock impl              OrderEventBus (10k cap)
        |                  AuditEventBus (50k cap)
        | HTTPS             TickEventBus (100k cap)
        v                        |
mStock API               PostgreSQL + TimescaleDB
(Mirae Asset)            (Docker)
```

---

## Quick Start

### Prerequisites
- Java 25+
- Apache Maven 3.9+
- Docker Desktop
- Tomcat 11 (via Homebrew: `brew install tomcat`)

### 1. Clone and Configure
```bash
git clone <repo-url>
cd fno-oms
```

Edit `src/main/resources/application.properties`:
```properties
# Database (matches docker-compose.yml)
db.url=jdbc:postgresql://localhost:5432/fno_oms
db.username=fnooms
db.password=fnooms123

# mStock broker API
mstock.base.url=https://api.mstock.trade/openapi/typea
```

### 2. Start the Database
```bash
docker compose up -d
```
This starts:
- **TimescaleDB** on `localhost:5432` (data persisted in `./pgdata`)
- **pgAdmin** on `http://localhost:5050`

### 3. Build the WAR
```bash
mvn clean package -DskipTests
```

### 4. Deploy to Tomcat
```bash
cp target/fno-oms.war /opt/homebrew/opt/tomcat/libexec/webapps/
```

### 5. Start Tomcat
```bash
/opt/homebrew/opt/tomcat/bin/catalina run
```

### 6. Open the App
```
http://localhost:8080/fno-oms
```

---

## Configuration

### application.properties

| Key | Default | Description |
|-----|---------|-------------|
| `db.url` | `jdbc:postgresql://localhost:5432/fno_oms` | PostgreSQL JDBC URL |
| `db.username` | `fnooms` | DB username |
| `db.password` | `fnooms123` | DB password |
| `db.pool.max` | `10` | HikariCP max pool size |
| `mstock.base.url` | `https://api.mstock.trade/openapi/typea` | mStock API base URL |
| `mstock.api.version` | `1` | `X-Mirae-Version` header value |

### Docker Compose Services

| Service | Port | Description |
|---------|------|-------------|
| `fno-oms-db` | `5432` | TimescaleDB (PostgreSQL 16) |
| `fno-oms-pgadmin` | `5050` | pgAdmin 4 web UI |

---

## Project Structure

```
fno-oms/
├── src/main/java/com/fnooms/
│   ├── async/              # Non-blocking event buses (Order/Audit/Tick)
│   ├── broker/             # BrokerClient SPI + mStock implementation
│   │   └── mstock/
│   ├── dao/                # DatabaseManager (HikariCP) + DAOs
│   ├── model/              # Domain models (Order, BrokerConfig, AuditLog)
│   ├── service/            # Business logic (OrderService, QuoteService, PortfolioService)
│   ├── servlet/            # Jakarta REST servlets (8 endpoints)
│   └── util/               # AppConfig, JsonUtil (with Instant TypeAdapter)
│
├── src/main/resources/
│   ├── application.properties
│   └── db/schema.sql       # Auto-runs on first startup
│
├── src/main/webapp/
│   ├── WEB-INF/
│   │   ├── web.xml                  # metadata-complete=true
│   │   └── jsp/
│   │       ├── dashboard.jsp        # Main SPA shell
│   │       ├── error.jsp
│   │       └── pages/               # Page fragments (jsp:include)
│   │           ├── dashboard.jsp
│   │           ├── watchlist.jsp
│   │           ├── orders.jsp
│   │           ├── portfolio.jsp
│   │           ├── connectivity.jsp
│   │           └── settings.jsp
│   ├── static/
│   │   ├── css/main.css             # Dark terminal theme
│   │   └── js/
│   │       ├── app.js
│   │       ├── quotes.js
│   │       ├── orders.js
│   │       ├── portfolio.js
│   │       └── connectivity.js
│   └── index.jsp
│
├── src/test/java/          # JUnit 5 + Mockito (20 tests)
├── docker-compose.yml
└── pom.xml
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/orders/local` | Get locally stored orders |
| `POST` | `/api/orders` | Place a new order via broker |
| `DELETE` | `/api/orders/{id}` | Cancel an order |
| `GET` | `/api/quote` | Get live quotes for instruments |
| `GET` | `/api/portfolio/positions` | Get net positions |
| `GET` | `/api/portfolio/holdings` | Get holdings |
| `GET` | `/api/broker-config` | List all broker configurations |
| `POST` | `/api/broker-config` | Add a new broker configuration |
| `PUT` | `/api/broker-config/{id}/activate` | Set a broker as active |
| `DELETE` | `/api/broker-config/{id}` | Remove a broker configuration |
| `POST` | `/api/connectivity/test` | Test all broker connections |
| `GET` | `/api/auth/status` | Get authentication status |

---

## Broker Setup (mStock)

1. Register at [mstock.com/trading-api](https://www.mstock.com/trading-api)
2. Generate your **API Key** from the developer dashboard
3. Each trading day, generate a fresh **JWT Access Token** via:
   - `POST /openapi/typea/connect/login` (triggers OTP)
   - `POST /openapi/typea/session/token` (returns JWT)
4. In FnO OMS → **Settings** → Add Broker → paste API Key + JWT

> JWT tokens expire daily at 18:30 IST. You must refresh them each morning.

---

## Running Tests

```bash
# Run all tests
mvn test

# Skip tests during build
mvn clean package -DskipTests
```

---

## Database Schema

| Table | Description |
|-------|-------------|
| `broker_configs` | Broker API keys and tokens |
| `orders` | All placed orders with status |
| `audit_logs` | Immutable audit trail |
| `ticks` | TimescaleDB hypertable for price ticks |

---

## Stopping

```bash
# Stop Tomcat: Ctrl+C in the terminal running catalina run

# Stop Docker services
docker compose down

# Stop and wipe DB data (destructive)
docker compose down -v
```

---

## Viewing Logs

```bash
# Live Tomcat log
tail -f /opt/homebrew/opt/tomcat/libexec/logs/catalina.$(date +%Y-%m-%d).log

# HTTP access log
tail -f /opt/homebrew/opt/tomcat/libexec/logs/localhost_access_log.$(date +%Y-%m-%d).txt
```

---

## Roadmap

- [ ] Daily JWT token auto-refresh via mStock OTP flow
- [ ] WebSocket real-time tick streaming (`wss://ws.mstock.trade`)
- [ ] Zerodha / Upstox broker adapters
- [ ] Options chain viewer with Greeks
- [ ] Strategy builder (straddles, spreads)
- [ ] CI/CD pipeline (GitHub Actions + Docker)
