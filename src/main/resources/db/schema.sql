-- ============================================================
-- FnO OMS Database Schema
-- PostgreSQL 16 + TimescaleDB
-- ============================================================

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ============================================================
-- Orders (local audit trail of all placed orders)
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    id                  SERIAL PRIMARY KEY,
    business_date       DATE         NOT NULL DEFAULT CURRENT_DATE,
    order_source        VARCHAR(50)  NOT NULL DEFAULT 'ALGO', -- ALGO, BROKER_WEB
    broker_order_id     VARCHAR(100),             -- broker-assigned order ID
    broker_type         VARCHAR(50)  NOT NULL,    -- e.g. MSTOCK, ANGELONE
    symbol              VARCHAR(100) NOT NULL,    -- e.g. NIFTY24JUL24000CE
    exchange            VARCHAR(20)  NOT NULL,    -- NSE, NFO, BSE, BFO
    transaction_type    VARCHAR(10)  NOT NULL,    -- BUY, SELL
    order_type          VARCHAR(20)  NOT NULL,    -- MARKET, LIMIT, SL, SL-M
    product             VARCHAR(20)  NOT NULL,    -- MIS, NRML, CNC
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
    updated_by          VARCHAR(50)  DEFAULT 'SYSTEM',
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_placed_at   ON orders (placed_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_symbol       ON orders (symbol);
CREATE INDEX IF NOT EXISTS idx_orders_status       ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_broker_order ON orders (broker_order_id);

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

CREATE OR REPLACE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

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
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by  VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_action     ON audit_log (action);

CREATE OR REPLACE TRIGGER trg_audit_log_updated_at
    BEFORE UPDATE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

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
    broker_type     VARCHAR(50),
    updated_by      VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
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

CREATE OR REPLACE TRIGGER trg_quote_ticks_updated_at
    BEFORE UPDATE ON quote_ticks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- Algo Settings & State Persistence
-- ============================================================
CREATE TABLE IF NOT EXISTS algo_key_value (
    id SERIAL PRIMARY KEY,
    key_name VARCHAR(100) UNIQUE NOT NULL,
    key_value TEXT NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE OR REPLACE TRIGGER trg_algo_key_value_updated_at
    BEFORE UPDATE ON algo_key_value
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('scrip.master.date', '1970-01-01', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;
INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('algo.feedBroker', 'MSTOCK', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;
INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('algo.orderBroker', 'MSTOCK', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;
INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mock.price.min', '1000', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;
INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mock.price.max', '2000', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;
INSERT INTO algo_key_value (key_name, key_value, updated_by) VALUES ('mock.price.volatility', '5', 'SYSTEM') ON CONFLICT (key_name) DO NOTHING;

-- ============================================================
-- Insert Default Credentials from old cred.properties
-- ============================================================
INSERT INTO algo_key_value (key_name, key_value) VALUES 
('mstock.api_key', 'nRRoc4X2uClpzIqNHlUC5Q=='),
('mstock.jwt_token', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVU0VSTkFNRSI6Ik1BNjg3NiIsIkNMSUVOVE5BTUUiOiJTVURIRUVSIFJFRERZIExFTktBTEEiLCJVU0VSX0RFVEFJTFMiOiJwdzVDdS9xMnp0Y3JRcFdtRy9DcmVJaVQzUUJ4VVJ6NXRJZGhTWlRNR1p0NWpCUC9uRHA3OENZWTB0dDhaNGRkeVo0TEdrUlpFSEd0TFN4YzZwaWhveHB6cndGLzVqRmt3NEJ3U2NYYW5DSm43NTVrY2JobkhKT1JvUkRZMGhGZUJDM3hBeTZ3Z0owM0dVZVFHSWs2MnZaVENMOTlFTGhqMEowelVXR0IzbE8rREtmc3dsM25seVhaS1d5TVNXUnhSRFprZXZicnZaRy9MWmxVSFdWd0lzeWNnWVVCN3JKU1ZvTUlTa0xzb1Z1NlQrL1J5R3Y5R3NhWmMxa21uemVYek5mRWRDRnFSU3ZPVzFlVFdjam5HZlZaWHVndWsvRkl5ZldhTGxZL08reDZseUZ2NWJzSXRaNWJCV29lZ0Y1c2dzSmxvbkZzY0FaM3gyQXFKUnRwMHc9PSIsIlVTRVJJRCI6Ik1BNjg3NiIsIkFDQ0VTU19UT0tFTiI6ImV5SmhiR2NpT2lKSVV6STFOaUlzSW5SNWNDSTZJa3BYVkNKOS5leUpoZFdRaU9pSnRhWEpoWlM1cGJpSXNJbVY0Y0NJNk1UYzROREV6TmpVMU15d2lhV0YwSWpveE56ZzBNRFV3TVRVekxDSnBjM01pT2lKdGFYSmhaUzVwYmlJc0ltNWlaaUk2TVRRME5EUTNPRFF3TUN3aWNHWnRJam9pTVNJc0luUnBaQ0k2SWpZeElpd2lkV2xrSWpvaU5EZzFOemt3SWl3aWRtbGtJam9pTWpFaWZRLk5oS2lnWGdwWXdlUmZZd3o4YzUzQzJpX0hJdXRub0psYjlRV1VCZXdTdW8iLCJBUElUWVBFIjoiVFlQRUEiLCJVSUQiOiJjYWM0ZjEzMC0wOTg4LTQ3NjgtODFlYy00NTVlMDQ2ZmI5NDkiLCJuYmYiOjE3ODQwNTAxNTMsImV4cCI6MTc4NDA1MzgwMCwiaWF0IjoxNzg0MDUwMTUzfQ.dNFNbezsE7L2V3q_srkRwbcKIM8eqIn3xqQgVbntNQs'),
('angelone.api_key', 'Cs4E2TqP'),
('angelone.client_code', 'S54534677'),
('angelone.jwt_token', 'eyJhbGciOiJIUzUxMiJ9.eyJ1c2VybmFtZSI6IlM1NDUzNDY3NyIsInJvbGVzIjowLCJ1c2VydHlwZSI6IlVTRVIiLCJ0b2tlbiI6ImV5SmhiR2NpT2lKU1V6STFOaUlzSW5SNWNDSTZJa3BYVkNKOS5leUoxYzJWeVgzUjVjR1VpT2lKamJHbGxiblFpTENKMGIydGxibDkwZVhCbElqb2lkSEpoWkdWZllXTmpaWE56WDNSdmEyVnVJaXdpWjIxZmFXUWlPakV4TENKemIzVnlZMlVpT2lJeklpd2laR1YyYVdObFgybGtJam9pTWpNNFpUUmhOVFl0WmpBd09DMHpOelF5TFdGbE1XSXRObVZrWWpRNVl6Y3daVFl6SWl3aWEybGtJam9pZEhKaFpHVmZhMlY1WDNZeUlpd2liMjF1WlcxaGJtRm5aWEpwWkNJNk1URXNJbkJ5YjJSMVkzUnpJanA3SW1SbGJXRjBJanA3SW5OMFlYUjFjeUk2SW1GamRHbDJaU0o5TENKdFppSTZleUp6ZEdGMGRYTWlPaUpoWTNScGRtVWlmU3dpYm1KMVRHVnVaR2x1WnlJNmV5SnpkR0YwZFhNaU9pSmhZM1JwZG1VaWZYMHNJbWx6Y3lJNkluUnlZV1JsWDJ4dloybHVYM05sY25acFkyVWlMQ0p6ZFdJaU9pSlROVFExTXpRMk56Y2lMQ0psZUhBaU9qRTNPRFF3T0RZek56TXNJbTVpWmlJNk1UYzRNems1T1RjNU15d2lhV0YwSWpveE56ZzPVGs1TnprekxDSnFkR2tpT2lKa056RmlNakF5T1Mxak1XTmlMVFF5WldVdFlqYzFOUzAzTVdFelpXVTVaVFptTldVaUxDSlViMnRsYmlJNklpSjkuTHhMS1dmM2hZUUtOZDdXcXBtR2hYdnNzRW5PWmlOdG00bHhJc0t6aV92YlMxNU1sbFFfVWhTbmFFalVCM2hDLTd3TlR2cXRUTk9mMG42SkVma0tqZnBFRndwbDNtaV9vN1RxdVpYZ1laUGF0MFEyS3RKSnlRR19LUWNfNG53SERBaG5rd3VFdGxQU21mZGtDTlZIRzFMWUdaSnFVeGhDYlJ3UG5aQXFSTjc4IiwiQVBJLUtFWSI6IkNzNEUyVHFQIiwiaWF0IjoxNzgzOTk5OTczLCJleHAiOjE3ODQwNTM4MDB9._yqaBT1S50JyXwces4awGiSOKVKw9BD9p1B20_piPbyiicYhGTEm1-PNh4lklOlBLRpMMM_8ELS8qAI0MczmGA'),
('angelone.feed_token', 'eyJhbGciOiJIUzUxMiJ9.eyJ1c2VybmFtZSI6IlM1NDUzNDY3NyIsImlhdCI6MTc4Mzk5OTk3MywiZXhwIjoxNzg0MDg2MzczfQ.Sul2m8izu01SCgQfGLL0_mzfs6u08bqj_1R8W6gJYKeMfVncAOVbw0hV6b-JhOgmpWiUfxQFZAGlXHC3KSxddg')
ON CONFLICT (key_name) DO NOTHING;

-- ============================================================
-- Scrip Master (Instruments)
-- ============================================================
CREATE TABLE IF NOT EXISTS scrip_master (
    token VARCHAR(50) PRIMARY KEY,
    symbol VARCHAR(100) NOT NULL,
    name VARCHAR(100),
    expiry VARCHAR(50),
    strike DECIMAL(12, 2),
    lot_size INT,
    instrument_type VARCHAR(20),
    exch_seg VARCHAR(20),
    tick_size DECIMAL(12, 4),
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_scrip_master_symbol ON scrip_master(symbol);

CREATE OR REPLACE TRIGGER trg_scrip_master_updated_at
    BEFORE UPDATE ON scrip_master
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- Order Details
-- ============================================================
CREATE TABLE IF NOT EXISTS order_details (
    id SERIAL PRIMARY KEY,
    business_date DATE NOT NULL DEFAULT CURRENT_DATE,
    order_source VARCHAR(50) NOT NULL DEFAULT 'ALGO', -- ALGO, BROKER_WEB
    transaction_type VARCHAR(10) NOT NULL, -- BUY, SELL
    symbol VARCHAR(100) NOT NULL,
    order_id VARCHAR(100),
    price DECIMAL(12,2),
    order_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE OR REPLACE TRIGGER trg_order_details_updated_at
    BEFORE UPDATE ON order_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- Strategies Configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS strategies (
    id SERIAL PRIMARY KEY,
    scrip_name VARCHAR(100) NOT NULL,
    exchange_id VARCHAR(20) DEFAULT 'NFO',
    name VARCHAR(100),
    entry_price DECIMAL(12, 2) NOT NULL,
    stop_loss DECIMAL(12, 2) NOT NULL,
    target_price DECIMAL(12, 2) NOT NULL,
    trailing_sl_points DECIMAL(12, 2) DEFAULT 0,
    quantity INT NOT NULL,
    transaction_type VARCHAR(10) DEFAULT 'BUY',
    entry_condition VARCHAR(50) DEFAULT 'GREATER_THAN_EQUAL',
    product VARCHAR(20) DEFAULT 'MIS',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE OR REPLACE TRIGGER trg_strategies_updated_at
    BEFORE UPDATE ON strategies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- mStock Scrip Master
-- ============================================================
CREATE TABLE IF NOT EXISTS mstock_scrip_master (
    id SERIAL PRIMARY KEY,
    msg_code VARCHAR(10),
    instrument_token VARCHAR(50) NOT NULL UNIQUE,
    instrument_name VARCHAR(100),
    tradingsymbol VARCHAR(100) NOT NULL,
    expiry_date VARCHAR(50),
    strike_price DECIMAL(12, 2),
    option_type VARCHAR(10),
    exchange_segment VARCHAR(20),
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_mstock_scrip_symbol ON mstock_scrip_master(tradingsymbol);

CREATE OR REPLACE TRIGGER trg_mstock_scrip_master_updated_at
    BEFORE UPDATE ON mstock_scrip_master
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
