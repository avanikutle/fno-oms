package com.fnooms.mock;

import com.fnooms.dao.AlgoKeyValueDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MockDataGenerator {
    private static final Logger log = LoggerFactory.getLogger(MockDataGenerator.class);
    private final AlgoKeyValueDAO kvDao = new AlgoKeyValueDAO();
    private final Random random = new Random();

    private final ConcurrentMap<String, Double> currentPrices = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> trendUp = new ConcurrentHashMap<>();

    private double minPrice = 1000.0;
    private double maxPrice = 2000.0;
    private int volatility = 5; // 1 to 10
    
    public MockDataGenerator() {
        reloadConfig();
    }

    public void reloadConfig() {
        try {
            String minStr = kvDao.getValue("mock.price.min");
            if (minStr != null) minPrice = Double.parseDouble(minStr);

            String maxStr = kvDao.getValue("mock.price.max");
            if (maxStr != null) maxPrice = Double.parseDouble(maxStr);

            String volStr = kvDao.getValue("mock.price.volatility");
            if (volStr != null) volatility = Integer.parseInt(volStr);
            
            // Ensure bounds
            volatility = Math.max(1, Math.min(10, volatility));
        } catch (Exception e) {
            log.warn("Failed to load mock config, using defaults", e);
        }
    }

    public double getNextPrice(String token) {
        double current = currentPrices.computeIfAbsent(token, k -> minPrice + (maxPrice - minPrice) / 2.0);
        boolean isUp = trendUp.computeIfAbsent(token, k -> true);

        // Volatility scale: 1 = very small moves, 10 = large moves
        // If price is 1000, 1% is 10. Max move could be 0.1% to 1% based on volatility.
        double maxMove = (maxPrice - minPrice) * (volatility / 100.0);
        double move = random.nextDouble() * maxMove;

        // 10% chance to reverse trend abruptly (spikes/dives)
        if (random.nextDouble() < 0.10) {
            isUp = !isUp;
        }

        if (isUp) {
            current += move;
            if (current > maxPrice) {
                current = maxPrice;
                isUp = false;
            }
        } else {
            current -= move;
            if (current < minPrice) {
                current = minPrice;
                isUp = true;
            }
        }

        currentPrices.put(token, current);
        trendUp.put(token, isUp);
        return current;
    }

    /**
     * Creates a single MStock Mode 3 Quote Packet (379 bytes)
     */
    public ByteBuffer createMStockPacket(String token, double price) {
        ByteBuffer buf = ByteBuffer.allocate(379);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) 3); // subscriptionMode
        buf.put((byte) 2); // exchangeType (NFO)

        byte[] tokenBytes = new byte[25];
        byte[] srcToken = token.getBytes();
        System.arraycopy(srcToken, 0, tokenBytes, 0, Math.min(srcToken.length, 25));
        buf.put(tokenBytes);

        buf.putLong(System.currentTimeMillis()); // sequenceNumber
        buf.putLong(System.currentTimeMillis()); // exchangeTimestamp
        
        long rawLtp = (long) (price * 100);
        buf.putLong(rawLtp);
        
        // Pad the rest to reach 379
        byte[] padding = new byte[379 - 51];
        buf.put(padding);
        
        buf.flip();
        return buf;
    }

    public void savePacketsToFile(List<ByteBuffer> packets, String filePath) {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (ByteBuffer packet : packets) {
                byte[] arr = new byte[packet.remaining()];
                packet.get(arr);
                fos.write(arr);
                packet.rewind();
            }
            log.info("Saved {} mock packets to {}", packets.size(), filePath);
        } catch (IOException e) {
            log.error("Failed to save mock packets", e);
        }
    }
}
