-- ============================================================
-- FnO OMS Database Schema
-- PostgreSQL 16 + TimescaleDB
-- ============================================================

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ============================================================
-- Broker Configuration (pluggable broker store)
-- ============================================================
CREATE TABLE IF NOT EXISTS broker_config (
    id              SERIAL PRIMARY KEY,
    broker_type     VARCHAR(50)  NOT NULL,       -- e.g. 'MSTOCK', 'ZERODHA'
    display_name    VARCHAR(100) NOT NULL,
    api_key         VARCHAR(500),                -- encrypted at app level in future
    private_key     VARCHAR(500),
    access_token    TEXT,                        -- JWT, valid till midnight
    token_expiry    TIMESTAMP WITH TIME ZONE,
    client_id       VARCHAR(100),                -- broker's client/user ID
    is_active       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Only one broker can be active at a time
CREATE UNIQUE INDEX IF NOT EXISTS idx_broker_config_active
    ON broker_config (is_active)
    WHERE is_active = TRUE;

-- ============================================================
-- Orders (local audit trail of all placed orders)
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    id                  SERIAL PRIMARY KEY,
    broker_order_id     VARCHAR(100),             -- broker-assigned order ID
    broker_type         VARCHAR(50)  NOT NULL,
    broker_config_id    INT REFERENCES broker_config(id),
    symbol              VARCHAR(100) NOT NULL,     -- e.g. NIFTY24JUL24000CE
    exchange            VARCHAR(20)  NOT NULL,     -- NSE, NFO, BSE, BFO
    transaction_type    VARCHAR(10)  NOT NULL,     -- BUY, SELL
    order_type          VARCHAR(20)  NOT NULL,     -- MARKET, LIMIT, SL, SL-M
    product             VARCHAR(20)  NOT NULL,     -- MIS, NRML, CNC
    quantity            INT          NOT NULL,
    price               DECIMAL(12, 2),
    trigger_price       DECIMAL(12, 2),
    validity            VARCHAR(20)  DEFAULT 'DAY',
    status              VARCHAR(50),              -- OPEN, COMPLETE, CANCELLED, REJECTED
    status_message      VARCHAR(1000),
    filled_quantity     INT          DEFAULT 0,
    average_price       DECIMAL(12, 2),
    exchange_order_id   VARCHAR(100),
    exchange_timestamp  TIMESTAMP WITH TIME ZONE,
    placed_at           TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_placed_at   ON orders (placed_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_symbol       ON orders (symbol);
CREATE INDEX IF NOT EXISTS idx_orders_status       ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_broker_order ON orders (broker_order_id);

-- ============================================================
-- Audit Log (every broker API call)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id          SERIAL PRIMARY KEY,
    action      VARCHAR(100) NOT NULL,           -- e.g. PLACE_ORDER, CANCEL_ORDER, GET_QUOTE
    broker_type VARCHAR(50),
    endpoint    VARCHAR(500),
    request     TEXT,                            -- JSON payload sent
    response    TEXT,                            -- JSON response received
    status_code INT,
    latency_ms  BIGINT,
    error       TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_action     ON audit_log (action);

-- ============================================================
-- Quote Ticks (TimescaleDB hypertable — high-frequency time-series)
-- Used to persist live quote snapshots from broker polling
-- ============================================================
CREATE TABLE IF NOT EXISTS quote_ticks (
    time            TIMESTAMPTZ  NOT NULL,
    symbol          VARCHAR(100) NOT NULL,
    exchange        VARCHAR(20)  NOT NULL,
    ltp             DECIMAL(12, 2),              -- Last Traded Price
    open            DECIMAL(12, 2),
    high            DECIMAL(12, 2),
    low             DECIMAL(12, 2),
    close           DECIMAL(12, 2),              -- Previous close
    bid             DECIMAL(12, 2),
    ask             DECIMAL(12, 2),
    volume          BIGINT,
    oi              BIGINT,                      -- Open Interest (critical for options)
    oi_day_high     BIGINT,
    oi_day_low      BIGINT,
    change          DECIMAL(12, 2),
    change_pct      DECIMAL(8, 4),
    broker_type     VARCHAR(50)
);

-- Convert to TimescaleDB hypertable (partitioned by time, 1-day chunks)
SELECT create_hypertable(
    'quote_ticks',
    'time',
    chunk_time_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

-- Composite index for fast symbol+time queries
CREATE INDEX IF NOT EXISTS idx_quote_ticks_symbol_time
    ON quote_ticks (symbol, time DESC);

-- ============================================================
-- Helper: auto-update updated_at timestamp
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_broker_config_updated_at
    BEFORE UPDATE ON broker_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
