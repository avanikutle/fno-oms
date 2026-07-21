package com.fnooms.algo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryParserUtil {
    private static final Logger log = LoggerFactory.getLogger(BinaryParserUtil.class);

    public static void parseMStockMarketData(ByteBuffer data, StrategyEngine engine) {
        data.order(ByteOrder.LITTLE_ENDIAN);
        int length = data.remaining();
        if (length < 4)
            return;

        log.debug("parseMStockMarketData: remaining bytes={}", length);

        while (data.remaining() >= 379) {
            int startPos = data.position();

            byte subscriptionMode = data.get();
            byte exchangeType = data.get();

            byte[] tokenBytes = new byte[25];
            data.get(tokenBytes);
            String tokenStr = new String(tokenBytes).trim();
            tokenStr = tokenStr.replace("\u0000", "");

            long sequenceNumber = data.getLong();
            long exchangeTimestamp = data.getLong();
            long rawLtp = data.getLong();
            double ltp = rawLtp / 100.0;

            String symbol = ScripMasterServiceProvider.getInstance("MSTOCK").getSymbol(tokenStr);

            if (symbol != null) {
                engine.onPriceUpdate(symbol, ltp);
            }

            data.position(startPos + 379);
        }
    }

    public static void parseAngelOneMarketData(ByteBuffer data, StrategyEngine engine) {
        data.order(ByteOrder.LITTLE_ENDIAN);

        if (data.remaining() < 1)
            return;
        byte subscriptionMode = data.get();

        if (subscriptionMode == 1 || subscriptionMode == 2) {
            if (subscriptionMode == 1 && data.remaining() < 46)
                return;
            if (subscriptionMode == 2 && data.remaining() < 66)
                return;

            byte exchangeType = data.get();

            byte[] tokenBytes = new byte[25];
            data.get(tokenBytes);
            String token = new String(tokenBytes).trim();
            token = token.replace("\u0000", "");

            long sequenceNumber = data.getLong();
            long exchangeTimestamp = data.getLong();

            int rawLtp = data.getInt();
            double ltp = rawLtp / 100.0;

            String symbol = ScripMasterServiceProvider.getInstance("ANGELONE").getSymbol(token);
            if (symbol != null) {
                log.info("[AngelOne] Symbol: {}, Price:{}", symbol, ltp);
                engine.onPriceUpdate(symbol, ltp);
            }
        }
    }
}
