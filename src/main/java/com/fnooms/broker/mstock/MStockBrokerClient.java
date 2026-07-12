package com.fnooms.broker.mstock;

import com.fnooms.broker.BrokerClient;
import com.fnooms.broker.BrokerException;
import com.fnooms.broker.BrokerType;
import com.fnooms.broker.dto.*;
import com.fnooms.model.BrokerConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * mStock (Mirae Asset) broker client implementation.
 *
 * <p>API Base URL : https://api.mstock.trade
 * <p>Auth Headers : Authorization: token {api_key}:{jwt_token}
 *                   X-Mirae-Version: {version}
 *                   X-PrivateKey: {private_key}
 */
public class MStockBrokerClient implements BrokerClient {

    private static final Logger log = LoggerFactory.getLogger(MStockBrokerClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final MStockConfig config;
    private final OkHttpClient http;

    public MStockBrokerClient(BrokerConfig brokerConfig) {
        this.config = new MStockConfig(brokerConfig);
        this.http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /** Overridable for testing — returns the configured base URL. */
    protected String getBaseUrl() {
        return config.getBaseUrl();
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.MSTOCK;
    }

    // =========================================================
    //  QUOTES
    // =========================================================

    @Override
    public Map<String, Quote> getQuotes(List<String> instruments) throws BrokerException {
        if (instruments == null || instruments.isEmpty()) {
            return Collections.emptyMap();
        }

        // Build query: ?i=NSE:NIFTY50&i=NFO:NIFTY24JUL24000CE
        StringBuilder url = new StringBuilder(getBaseUrl())
                .append("/market-quote/quotes");
        for (int i = 0; i < instruments.size(); i++) {
            url.append(i == 0 ? "?" : "&").append("i=").append(instruments.get(i));
        }

        JsonObject body = executeGet(url.toString());
        Map<String, Quote> result = new LinkedHashMap<>();

        JsonObject data = safeGetObject(body, "data");
        if (data == null) return result;

        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            String key = entry.getKey(); // "NSE:SYMBOL"
            JsonObject q = entry.getValue().getAsJsonObject();
            result.put(key, parseQuote(key, q));
        }

        return result;
    }

    private Quote parseQuote(String key, JsonObject q) {
        Quote quote = new Quote();
        String[] parts = key.split(":", 2);
        quote.setExchange(parts.length > 0 ? parts[0] : "");
        quote.setSymbol(parts.length > 1 ? parts[1] : key);
        quote.setLtp(safeDecimal(q, "last_price"));
        quote.setVolume(safeLong(q, "volume"));
        quote.setOi(safeLong(q, "oi"));
        quote.setOiDayHigh(safeLong(q, "oi_day_high"));
        quote.setOiDayLow(safeLong(q, "oi_day_low"));
        quote.setBid(safeDecimal(q, "depth.buy.0.price"));
        quote.setAsk(safeDecimal(q, "depth.sell.0.price"));
        quote.setChange(safeDecimal(q, "net_change"));

        // OHLC block
        if (q.has("ohlc") && !q.get("ohlc").isJsonNull()) {
            JsonObject ohlc = q.getAsJsonObject("ohlc");
            quote.setOpen(safeDecimal(ohlc, "open"));
            quote.setHigh(safeDecimal(ohlc, "high"));
            quote.setLow(safeDecimal(ohlc, "low"));
            quote.setClose(safeDecimal(ohlc, "close"));
        }

        // Compute % change
        if (quote.getClose() != null && quote.getClose().compareTo(BigDecimal.ZERO) != 0
                && quote.getLtp() != null) {
            BigDecimal chg = quote.getLtp().subtract(quote.getClose());
            BigDecimal pct = chg.divide(quote.getClose(), 6, java.math.RoundingMode.HALF_UP)
                               .multiply(BigDecimal.valueOf(100));
            quote.setChange(chg);
            quote.setChangePct(pct);
        }

        quote.setTimestamp(Instant.now());
        return quote;
    }

    // =========================================================
    //  ORDERS
    // =========================================================

    @Override
    public OrderResponse placeOrder(OrderRequest req) throws BrokerException {
        Map<String, String> formParams = new LinkedHashMap<>();
        formParams.put("tradingsymbol",    req.getSymbol());
        formParams.put("exchange",         req.getExchange());
        formParams.put("transaction_type", req.getTransactionType());
        formParams.put("order_type",       req.getOrderType());
        formParams.put("product",          req.getProduct());
        formParams.put("quantity",         String.valueOf(req.getQuantity()));
        formParams.put("validity",         req.getValidity() != null ? req.getValidity() : "DAY");

        if (req.getPrice() != null)
            formParams.put("price", String.valueOf(req.getPrice()));
        if (req.getTriggerPrice() != null)
            formParams.put("trigger_price", String.valueOf(req.getTriggerPrice()));
        if (req.getTag() != null)
            formParams.put("tag", req.getTag());

        JsonObject resp = executePostForm(getBaseUrl() + "/orders/regular", formParams);

        OrderResponse order = new OrderResponse();
        JsonObject data = safeGetObject(resp, "data");
        if (data != null && data.has("order_id")) {
            order.setBrokerOrderId(data.get("order_id").getAsString());
        }
        order.setSymbol(req.getSymbol());
        order.setExchange(req.getExchange());
        order.setTransactionType(req.getTransactionType());
        order.setOrderType(req.getOrderType());
        order.setProduct(req.getProduct());
        order.setQuantity(req.getQuantity());
        order.setPrice(req.getPrice());
        order.setTriggerPrice(req.getTriggerPrice());
        order.setStatus("OPEN");
        order.setOrderTimestamp(Instant.now());

        log.info("Order placed: {} {} {} {}@{}", req.getTransactionType(), req.getQuantity(),
                req.getSymbol(), req.getOrderType(), req.getPrice());
        return order;
    }

    @Override
    public List<OrderResponse> getOrderBook() throws BrokerException {
        JsonObject body = executeGet(getBaseUrl() + "/orders");
        List<OrderResponse> orders = new ArrayList<>();

        JsonArray data = safeGetArray(body, "data");
        if (data == null) return orders;

        for (JsonElement el : data) {
            JsonObject o = el.getAsJsonObject();
            OrderResponse resp = new OrderResponse();
            resp.setBrokerOrderId(safeString(o, "order_id"));
            resp.setExchangeOrderId(safeString(o, "exchange_order_id"));
            resp.setSymbol(safeString(o, "tradingsymbol"));
            resp.setExchange(safeString(o, "exchange"));
            resp.setTransactionType(safeString(o, "transaction_type"));
            resp.setOrderType(safeString(o, "order_type"));
            resp.setProduct(safeString(o, "product"));
            resp.setQuantity(safeInt(o, "quantity"));
            resp.setFilledQuantity(safeInt(o, "filled_quantity"));
            resp.setPendingQuantity(safeInt(o, "pending_quantity"));
            resp.setPrice(safeDecimal(o, "price"));
            resp.setTriggerPrice(safeDecimal(o, "trigger_price"));
            resp.setAveragePrice(safeDecimal(o, "average_price"));
            resp.setStatus(safeString(o, "status"));
            resp.setStatusMessage(safeString(o, "status_message"));
            resp.setValidity(safeString(o, "validity"));
            resp.setTag(safeString(o, "tag"));
            orders.add(resp);
        }

        return orders;
    }

    @Override
    public boolean cancelOrder(String brokerOrderId) throws BrokerException {
        Request request = buildRequest(
                getBaseUrl() + "/orders/" + brokerOrderId,
                null, "DELETE");
        try (Response resp = http.newCall(request).execute()) {
            String body = resp.body() != null ? resp.body().string() : "{}";
            if (!resp.isSuccessful()) {
                throw new BrokerException("Cancel failed: " + body, resp.code(), null);
            }
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return safeBoolean(json, "status");
        } catch (IOException e) {
            throw new BrokerException("Cancel order network error", e);
        }
    }

    @Override
    public OrderResponse modifyOrder(String brokerOrderId, OrderRequest updated) throws BrokerException {
        JsonObject payload = new JsonObject();
        if (updated.getQuantity() > 0)      payload.addProperty("quantity",      updated.getQuantity());
        if (updated.getPrice() != null)      payload.addProperty("price",         updated.getPrice());
        if (updated.getTriggerPrice() != null) payload.addProperty("trigger_price", updated.getTriggerPrice());
        if (updated.getOrderType() != null)  payload.addProperty("order_type",    updated.getOrderType());

        Request request = buildRequest(
                getBaseUrl() + "/orders/" + brokerOrderId,
                payload.toString(), "PUT");
        try (Response resp = http.newCall(request).execute()) {
            String bodyStr = resp.body() != null ? resp.body().string() : "{}";
            if (!resp.isSuccessful()) {
                throw new BrokerException("Modify failed: " + bodyStr, resp.code(), null);
            }
            OrderResponse order = new OrderResponse();
            order.setBrokerOrderId(brokerOrderId);
            order.setStatus("MODIFIED");
            return order;
        } catch (IOException e) {
            throw new BrokerException("Modify order network error", e);
        }
    }

    // =========================================================
    //  PORTFOLIO
    // =========================================================

    @Override
    public List<Position> getPositions() throws BrokerException {
        JsonObject body = executeGet(getBaseUrl() + "/portfolio/positions");
        List<Position> positions = new ArrayList<>();

        JsonObject data = safeGetObject(body, "data");
        if (data == null) return positions;

        // mStock returns net + day positions
        JsonArray net = safeGetArray(data, "net");
        if (net != null) {
            for (JsonElement el : net) {
                positions.add(parsePosition(el.getAsJsonObject(), true));
            }
        }
        return positions;
    }

    private Position parsePosition(JsonObject o, boolean isNet) {
        Position p = new Position();
        p.setSymbol(safeString(o, "tradingsymbol"));
        p.setExchange(safeString(o, "exchange"));
        p.setProduct(safeString(o, "product"));
        p.setQuantity(safeInt(o, "quantity"));
        p.setOvernightQty(safeInt(o, "overnight_quantity"));
        p.setDayQty(safeInt(o, "day_quantity"));
        p.setAveragePrice(safeDecimal(o, "average_price"));
        p.setLastPrice(safeDecimal(o, "last_price"));
        p.setPnl(safeDecimal(o, "pnl"));
        p.setRealisedPnl(safeDecimal(o, "realised"));
        p.setUnrealisedPnl(safeDecimal(o, "unrealised"));
        p.setBuyPrice(safeDecimal(o, "buy_price"));
        p.setSellPrice(safeDecimal(o, "sell_price"));
        p.setBuyQuantity(safeInt(o, "buy_quantity"));
        p.setSellQuantity(safeInt(o, "sell_quantity"));
        p.setBuyValue(safeDecimal(o, "buy_value"));
        p.setSellValue(safeDecimal(o, "sell_value"));
        p.setMultiplier(safeDecimal(o, "multiplier"));
        p.setNetPosition(isNet);
        return p;
    }

    @Override
    public List<Holding> getHoldings() throws BrokerException {
        JsonObject body = executeGet(getBaseUrl() + "/portfolio/holdings");
        List<Holding> holdings = new ArrayList<>();

        JsonArray data = safeGetArray(body, "data");
        if (data == null) return holdings;

        for (JsonElement el : data) {
            JsonObject o = el.getAsJsonObject();
            Holding h = new Holding();
            h.setSymbol(safeString(o, "tradingsymbol"));
            h.setExchange(safeString(o, "exchange"));
            h.setIsin(safeString(o, "isin"));
            h.setQuantity(safeInt(o, "quantity"));
            h.setT1Quantity(safeInt(o, "t1_quantity"));
            h.setAveragePrice(safeDecimal(o, "average_price"));
            h.setLastPrice(safeDecimal(o, "last_price"));
            h.setClosePrice(safeDecimal(o, "close_price"));
            h.setPnl(safeDecimal(o, "pnl"));
            h.setPnlPct(safeDecimal(o, "day_change_percentage"));
            holdings.add(h);
        }
        return holdings;
    }

    // =========================================================
    //  CONNECTIVITY TEST
    // =========================================================

    @Override
    public void testConnection() throws BrokerException {
        if (!config.isConfigured()) {
            throw new BrokerException("Broker is not fully configured (missing API key or tokens)");
        }
        // The mStock API does not have a generic /user/profile, 
        // so we test connectivity against /portfolio/holdings since it is known to work
        executeGet(getBaseUrl() + "/portfolio/holdings");
    }

    // =========================================================
    //  HTTP HELPERS
    // =========================================================

    private JsonObject executeGet(String url) throws BrokerException {
        return execute(buildRequest(url, null, "GET"));
    }

    private JsonObject executePost(String url, String jsonBody) throws BrokerException {
        return execute(buildRequest(url, jsonBody, "POST"));
    }

    private JsonObject executePostForm(String url, Map<String, String> formParams) throws BrokerException {
        String authHeader = config.getAuthorizationHeader();
        String apiVersion = config.getApiVersion();
        String privateKey = config.getPrivateKey() != null ? config.getPrivateKey() : "";

        log.info("MSTOCK DEBUG - Request POST URL: {}", url);
        log.info("MSTOCK DEBUG - Request Headers: Authorization={}, X-Mirae-Version={}, X-PrivateKey={}", 
                 authHeader, apiVersion, privateKey);

        FormBody.Builder formBuilder = new FormBody.Builder();
        StringBuilder formLog = new StringBuilder("MSTOCK DEBUG - Form Params: ");
        for (Map.Entry<String, String> entry : formParams.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
            formLog.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }
        log.info(formLog.toString());

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Authorization",  authHeader)
                .header("X-Mirae-Version", apiVersion)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .post(formBuilder.build());

        if (privateKey != null && !privateKey.isBlank()) {
            builder.header("X-PrivateKey", privateKey);
        }

        return execute(builder.build());
    }

    private JsonObject execute(Request request) throws BrokerException {
        long start = System.currentTimeMillis();
        try (Response resp = http.newCall(request).execute()) {
            String bodyStr = resp.body() != null ? resp.body().string() : "{}";
            long elapsed = System.currentTimeMillis() - start;
            log.debug("{} {} → {} ({}ms)", request.method(), request.url(), resp.code(), elapsed);

            JsonObject json = new JsonObject();
            try {
                JsonElement root = JsonParser.parseString(bodyStr);
                if (root.isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray();
                    if (arr.size() > 0) json = arr.get(0).getAsJsonObject();
                } else if (root.isJsonObject()) {
                    json = root.getAsJsonObject();
                }
            } catch (Exception e) {
                // Ignore parse errors, fallback to raw body string for error reporting
            }

            if (!resp.isSuccessful()) {
                String errorMsg = safeString(json, "message");
                if (errorMsg == null) errorMsg = bodyStr;
                throw new BrokerException(
                        "mStock API error " + resp.code() + ": " + errorMsg,
                        resp.code(), null);
            }

            // mStock wraps responses with status:true/false or status:"success"
            if (json.has("status") && !json.get("status").isJsonNull()) {
                JsonElement statusEl = json.get("status");
                boolean ok = false;
                if (statusEl.isJsonPrimitive()) {
                    if (statusEl.getAsJsonPrimitive().isBoolean()) {
                        ok = statusEl.getAsBoolean();
                    } else if (statusEl.getAsJsonPrimitive().isString()) {
                        ok = "success".equalsIgnoreCase(statusEl.getAsString());
                    }
                }
                if (!ok) {
                    String msg = safeString(json, "message");
                    throw new BrokerException("mStock returned status=" + statusEl.getAsString() + ": " + msg, resp.code(), null);
                }
            }
            return json;
        } catch (IOException e) {
            throw new BrokerException("Network error calling mStock API: " + e.getMessage(), e);
        }
    }

    private Request buildRequest(String url, String jsonBody, String method) {
        String authHeader = config.getAuthorizationHeader();
        String apiVersion = config.getApiVersion();
        String privateKey = config.getPrivateKey() != null ? config.getPrivateKey() : "";
        
        log.info("MSTOCK DEBUG - URL: {}", url);
        log.info("MSTOCK DEBUG - Auth Header: {}", authHeader);
        log.info("MSTOCK DEBUG - X-Mirae-Version: {}", apiVersion);
        log.info("MSTOCK DEBUG - X-PrivateKey: {}", privateKey);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Authorization",  authHeader)
                .header("X-Mirae-Version", apiVersion)
                .header("Content-Type",   "application/json")
                .header("Accept",         "application/json");

        if (privateKey != null && !privateKey.isBlank()) {
            builder.header("X-PrivateKey", privateKey);
        }

        switch (method) {
            case "GET"    -> builder.get();
            case "POST"   -> builder.post(RequestBody.create(
                                jsonBody != null ? jsonBody : "{}", JSON));
            case "PUT"    -> builder.put(RequestBody.create(
                                jsonBody != null ? jsonBody : "{}", JSON));
            case "DELETE" -> builder.delete(jsonBody != null
                                ? RequestBody.create(jsonBody, JSON) : null);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        return builder.build();
    }

    // =========================================================
    //  JSON SAFE ACCESSORS
    // =========================================================

    private String safeString(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }

    private BigDecimal safeDecimal(JsonObject o, String key) {
        try {
            return (o.has(key) && !o.get(key).isJsonNull())
                    ? o.get(key).getAsBigDecimal() : null;
        } catch (Exception e) { return null; }
    }

    private long safeLong(JsonObject o, String key) {
        try {
            return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsLong() : 0L;
        } catch (Exception e) { return 0L; }
    }

    private int safeInt(JsonObject o, String key) {
        try {
            return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsInt() : 0;
        } catch (Exception e) { return 0; }
    }

    private boolean safeBoolean(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) && o.get(key).getAsBoolean();
    }

    private JsonObject safeGetObject(JsonObject o, String key) {
        return (o.has(key) && o.get(key).isJsonObject()) ? o.getAsJsonObject(key) : null;
    }

    private JsonArray safeGetArray(JsonObject o, String key) {
        return (o.has(key) && o.get(key).isJsonArray()) ? o.getAsJsonArray(key) : null;
    }
}
