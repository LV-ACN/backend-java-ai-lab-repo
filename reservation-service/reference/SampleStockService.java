package com.breezewood.reservation.reference;

// REFERENCE EXAMPLE ONLY -- this package is not part of the build (it lives
// outside src/main and src/test) and does not run against the real app.
//
// It exists purely to show the shape a generated class + its collaborator
// should follow when captured in a Cursor Skill, using a fictitious "stock"
// domain so it can't be mistaken for the real overbooking feature:
//   1. The service depends on an external collaborator via its constructor
//      (here, StockClient), the same shape as ReservationService -> InventoryClient.
//   2. A rejection is a dedicated exception, not a null/boolean return.
//   3. Request/response data are small records, not loose parameters.
import org.springframework.stereotype.Service;

@Service
public class SampleStockService {

    private final SampleStockClient stockClient;

    public SampleStockService(SampleStockClient stockClient) {
        this.stockClient = stockClient;
    }

    public SampleOrderConfirmation reserve(SampleOrderRequest request) {
        SampleStockRecord stock = stockClient.getStock(request.sku());

        if (request.quantity() > stock.availableUnits()) {
            throw new SampleOutOfStockException(
                    "Not enough stock for %s: %d requested, %d available"
                            .formatted(request.sku(), request.quantity(), stock.availableUnits()));
        }

        return new SampleOrderConfirmation(request.sku(), request.quantity());
    }
}

interface SampleStockClient {
    SampleStockRecord getStock(String sku);
}

record SampleStockRecord(String sku, int availableUnits) {}

record SampleOrderRequest(String sku, int quantity) {}

record SampleOrderConfirmation(String sku, int quantity) {}

class SampleOutOfStockException extends RuntimeException {
    SampleOutOfStockException(String message) {
        super(message);
    }
}
