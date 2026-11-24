package com.breezewood.reservation.reference;

// REFERENCE EXAMPLE ONLY -- demonstrates integration-test conventions:
// Spring Boot Test + MockMvc against the real controller/service wiring,
// with the external collaborator mocked at the Spring bean level via
// @MockitoBean instead of calling a real HTTP dependency, and a rejection
// asserted as the mapped HTTP status (via SampleApiExceptionHandler) --
// the same pattern the real ApiExceptionHandler uses for BookingRejectedException.
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SampleStockApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SampleStockClient stockClient;

    @Test
    void reserveWithinStockReturnsConfirmation() throws Exception {
        when(stockClient.getStock(anyString()))
                .thenReturn(new SampleStockRecord("SAMPLE-SKU-1", 5));

        mockMvc.perform(post("/api/reference/sample-stock/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SAMPLE-SKU-1","quantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SAMPLE-SKU-1"))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void reserveOverStockReturnsConflict() throws Exception {
        when(stockClient.getStock(anyString()))
                .thenReturn(new SampleStockRecord("SAMPLE-SKU-1", 5));

        mockMvc.perform(post("/api/reference/sample-stock/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SAMPLE-SKU-1","quantity":6}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
