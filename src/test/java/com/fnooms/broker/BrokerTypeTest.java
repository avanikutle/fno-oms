package com.fnooms.broker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BrokerType enum parsing — critical because the factory switch-case
 * depends entirely on this. A typo here = wrong broker = real money at risk.
 */
class BrokerTypeTest {

    @Test
    void fromString_mstock_caseInsensitive() {
        assertEquals(BrokerType.MSTOCK, BrokerType.fromString("MSTOCK"));
        assertEquals(BrokerType.MSTOCK, BrokerType.fromString("mstock"));
        assertEquals(BrokerType.MSTOCK, BrokerType.fromString("MStock"));
    }

    @Test
    void fromString_unknownValue_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> BrokerType.fromString("UNKNOWN_BROKER"));
    }

    @Test
    void fromString_blankValue_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> BrokerType.fromString(""));
    }

    @Test
    void allTypes_haveDisplayNames() {
        for (BrokerType bt : BrokerType.values()) {
            assertNotNull(bt.getDisplayName(), bt.name() + " has null displayName");
            assertFalse(bt.getDisplayName().isBlank(), bt.name() + " has blank displayName");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MSTOCK", "ZERODHA", "UPSTOX", "ANGEL", "FYERS"})
    void fromString_allDefinedTypes_roundTrip(String typeName) {
        BrokerType bt = BrokerType.fromString(typeName);
        assertEquals(typeName, bt.name());
    }
}
