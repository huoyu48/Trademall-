package com.orderflow.config;

import com.orderflow.security.TenantContext;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderFlowTenantHandlerTest {
    private final OrderFlowTenantHandler handler = new OrderFlowTenantHandler();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void appliesCurrentTenantToBusinessTablesAndCanExplicitlyBypassForPlatform() {
        TenantContext.set(42L, 1L, "merchant");
        assertEquals(42L, ((LongValue) handler.getTenantId()).getValue());
        assertFalse(handler.ignoreTable("orders"));
        assertTrue(handler.ignoreTable("customer"));

        TenantContext.setIgnoreTenant(true);
        assertTrue(handler.ignoreTable("orders"));
    }
}
