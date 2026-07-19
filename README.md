# FnO OMS — Options Order Management System

> A professional, dark-themed trading terminal for F&O options traders.  
> Built on **Java 25 · Tomcat 11 · Jakarta EE 10 · PostgreSQL + TimescaleDB**.
> Multi-broker architecture supporting **mStock**, **Angel One**, and **Dhan**.

---

## 📸 Features

| Feature | Description |
|---------|-------------|
| **Multi-Broker Integration** | Support for mStock, Angel One, and Dhan APIs |
| **Multi-Module Architecture** | Modularized into `fno-oms-common`, `fno-oms-batch`, and `fno-oms-web` |
| **Dynamic Audit Trail** | Automated `updated_by` tagging for database insertions (`WEB` vs `BATCH`) |
| **Live Watchlist** | Real-time quotes via broker WebSockets |
| **Order Management** | Place, cancel, and modify F&O orders with BUY/SELL toggle |
| **Portfolio** | Net positions and holdings with live P&L |
| **Connectivity Check** | Parallel latency test for all configured brokers |
| **Settings** | Manage multiple broker API keys, JWT tokens, and TOTP authentication |
| **Async Event Bus** | Non-blocking order/audit/tick persistence |
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
fno-oms-web (Tomcat 11 / Jakarta Servlets)
  DashboardServlet · OrderServlet · QuoteServlet
  PortfolioServlet · BrokerConfigServlet
  ConnectivityServlet · AuthServlet
        |                        |
        v                        v
fno-oms-batch (Standalone / Orchestrator)
  AlgoOrchestrator · Broker Clients (mStock, AngelOne, Dhan)
  WebSocket Listeners · Login Managers
        |                  
        | HTTPS / WSS      
        v                        
Broker APIs & Data Feeds
        |
fno-oms-common (Core / DB)
  DatabaseManager · AppConfig · DAO Layers
  OrderEventBus · AuditEventBus · TickEventBus
        |
  PostgreSQL + TimescaleDB (Docker)
```

---

## Quick Start

### Prerequisites
- Java 25+
- Apache Maven 3.9+
- Docker Desktop
- Tomcat 11

### 1. Clone and Configure
```bash
git clone <repo-url>
cd fno-oms
```

Edit `fno-oms-web/src/main/resources/application.properties` (or the common one):
```properties
# Database (matches docker-compose.yml)
db.url=jdbc:postgresql://localhost:5432/fno_oms
db.username=fnooms
db.password=fnooms123
```

### 2. Start the Database
```bash
docker compose up -d
```
This starts:
- **TimescaleDB** on `localhost:5432` (data persisted in `./pgdata`)
- **pgAdmin** on `http://localhost:5050`

### 3. Build the Project
```bash
mvn clean install -DskipTests
```

### 4. Deploy to Tomcat
Deploy the built WAR file to your Tomcat instance.
Alternatively, use the Cargo Maven plugin:
```bash
cd fno-oms-web
mvn cargo:run
```

### 5. Open the App
```
http://localhost:8080/fno-oms
```

---

## Project Structure

```
fno-oms/
├── fno-oms-common/
│   ├── src/main/java/com/fnooms/
│   │   ├── async/              # Non-blocking event buses (Order/Audit/Tick)
│   │   ├── dao/                # DatabaseManager (HikariCP) + DAOs
│   │   ├── model/              # Domain models (Order, BrokerConfig, AuditLog)
│   │   └── util/               # AppConfig, JsonUtil
│   └── src/main/resources/db/schema.sql
│
├── fno-oms-batch/
│   ├── src/main/java/com/fnooms/
│   │   ├── algo/               # AlgoOrchestrator, WebSocket Listeners, Logic
│   │   ├── broker/             # BrokerClient SPI implementations
│   │   ├── mock/               # Mock data feeds for testing
│   │   └── service/            # Business logic (OrderService, QuoteService)
│
├── fno-oms-web/
│   ├── src/main/java/com/fnooms/servlet/  # Jakarta REST servlets
│   └── src/main/webapp/                   # JSP pages, CSS, JS
│
├── .vscode/
│   └── launch.json             # VS Code debugger configs mapped to modules
├── docker-compose.yml
└── pom.xml                     # Parent POM managing the multi-module build
```

---

## Configuration & Audit

The system automatically manages database audit trails (`updated_by` column) by detecting the context it runs in:
- Context is set to `WEB` dynamically via Tomcat's `AppStartupListener`.
- Context is set to `BATCH` when running standalone scripts (e.g. `AngelOneLoginMain`, `MStockLoginMain`, `AlgoOrchestratorMain`).

---

## Running Batch Jobs / Scripts

The `fno-oms-batch` module contains main classes used for authenticating with brokers (such as TOTP flows for Angel One) or running algorithms headlessly. You can run these natively in VS Code utilizing the `.vscode/launch.json` file.

```bash
# Example of running a script manually via Maven
cd fno-oms-batch
mvn exec:java -Dexec.mainClass="com.fnooms.algo.login.AngelOneLoginMain"
```

---

## Roadmap

- [x] Multi-Module Maven Refactoring
- [x] Support for Multiple Brokers (Dhan, AngelOne, mStock)
- [x] Dynamic context-aware DB auditing (`WEB` vs `BATCH`)
- [ ] Options chain viewer with Greeks
- [ ] Strategy builder (straddles, spreads)
- [ ] CI/CD pipeline (GitHub Actions + Docker)
