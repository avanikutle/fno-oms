package com.fnooms.broker.mstock.api;

import com.fnooms.broker.BrokerException;
import com.fnooms.broker.dto.Holding;
import com.fnooms.broker.dto.Position;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles mStock API endpoints for Portfolio (Holdings & Positions).
 */
public class MStockPortfolioApi {

    private static final Logger log = LoggerFactory.getLogger(MStockPortfolioApi.class);
    private final MStockCoreClient core;

    public MStockPortfolioApi(MStockCoreClient core) {
        this.core = core;
    }

    public List<Position> getPositions() throws BrokerException {
        JsonObject body = core.executeGet(core.getBaseUrl() + "/portfolio/positions");
        List<Position> positions = new ArrayList<>();

        JsonObject data = core.safeGetObject(body, "data");
        if (data == null) return positions;

        JsonArray net = core.safeGetArray(data, "net");
        if (net != null) {
            for (JsonElement el : net) {
                positions.add(parsePosition(el.getAsJsonObject(), true));
            }
        }
        return positions;
    }

    private Position parsePosition(JsonObject o, boolean isNet) {
        Position p = new Position();
        p.setSymbol(core.safeString(o, "tradingsymbol"));
        p.setExchange(core.safeString(o, "exchange"));
        p.setProduct(core.safeString(o, "product"));
        p.setQuantity(core.safeInt(o, "quantity"));
        p.setOvernightQty(core.safeInt(o, "overnight_quantity"));
        p.setDayQty(core.safeInt(o, "day_quantity"));
        p.setAveragePrice(core.safeDecimal(o, "average_price"));
        p.setLastPrice(core.safeDecimal(o, "last_price"));
        p.setPnl(core.safeDecimal(o, "pnl"));
        p.setRealisedPnl(core.safeDecimal(o, "realised"));
        p.setUnrealisedPnl(core.safeDecimal(o, "unrealised"));
        p.setBuyPrice(core.safeDecimal(o, "buy_price"));
        p.setSellPrice(core.safeDecimal(o, "sell_price"));
        p.setBuyQuantity(core.safeInt(o, "buy_quantity"));
        p.setSellQuantity(core.safeInt(o, "sell_quantity"));
        p.setBuyValue(core.safeDecimal(o, "buy_value"));
        p.setSellValue(core.safeDecimal(o, "sell_value"));
        p.setMultiplier(core.safeDecimal(o, "multiplier"));
        p.setNetPosition(isNet);
        return p;
    }

    public List<Holding> getHoldings() throws BrokerException {
        JsonObject body = core.executeGet(core.getBaseUrl() + "/portfolio/holdings");
        List<Holding> holdings = new ArrayList<>();

        JsonArray data = core.safeGetArray(body, "data");
        if (data == null) return holdings;

        for (JsonElement el : data) {
            JsonObject o = el.getAsJsonObject();
            Holding h = new Holding();
            h.setSymbol(core.safeString(o, "tradingsymbol"));
            h.setExchange(core.safeString(o, "exchange"));
            h.setIsin(core.safeString(o, "isin"));
            h.setQuantity(core.safeInt(o, "quantity"));
            h.setT1Quantity(core.safeInt(o, "t1_quantity"));
            h.setAveragePrice(core.safeDecimal(o, "average_price"));
            h.setLastPrice(core.safeDecimal(o, "last_price"));
            h.setClosePrice(core.safeDecimal(o, "close_price"));
            h.setPnl(core.safeDecimal(o, "pnl"));
            h.setPnlPct(core.safeDecimal(o, "day_change_percentage"));
            holdings.add(h);
        }
        return holdings;
    }
}
