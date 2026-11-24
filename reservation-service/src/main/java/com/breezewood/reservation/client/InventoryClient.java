package com.breezewood.reservation.client;

import com.breezewood.reservation.model.InventoryRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder restClientBuilder,
                            @Value("${inventory.service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public InventoryRecord getInventory(String propertyId, String roomType) {
        return restClient.get()
                .uri("/api/properties/{propertyId}/inventory/{roomType}", propertyId, roomType)
                .retrieve()
                .body(InventoryRecord.class);
    }

    public InventoryRecord incrementBooked(String propertyId, String roomType, int rooms) {
        return restClient.post()
                .uri("/api/properties/{propertyId}/inventory/{roomType}/book", propertyId, roomType)
                .body(Map.of("rooms", rooms))
                .retrieve()
                .body(InventoryRecord.class);
    }
}
