package com.breezewood.reservation.reference;

// REFERENCE EXAMPLE ONLY -- see SampleStockService.java. Paired with that
// fictitious service purely so the integration-test reference has a real
// endpoint to exercise through MockMvc.
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reference/sample-stock")
public class SampleStockController {

    private final SampleStockService sampleStockService;

    public SampleStockController(SampleStockService sampleStockService) {
        this.sampleStockService = sampleStockService;
    }

    @PostMapping("/reserve")
    public SampleOrderConfirmation reserve(@RequestBody SampleOrderRequest request) {
        return sampleStockService.reserve(request);
    }
}
