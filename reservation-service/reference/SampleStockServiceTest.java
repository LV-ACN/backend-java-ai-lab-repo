package com.breezewood.reservation.reference;

// REFERENCE EXAMPLE ONLY -- demonstrates unit-test conventions: mock the
// external collaborator, load scenarios from a companion JSON fixture
// instead of hardcoding them, and assert one outcome per scenario.
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SampleStockServiceTest {

    static Stream<SampleStockScenario> scenarios() throws Exception {
        String json = Files.readString(Path.of("reference", "sample-stock-scenarios.json"));
        List<SampleStockScenario> scenarios =
                new ObjectMapper().readValue(json, new TypeReference<>() {});
        return scenarios.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void reserveMatchesExpectedOutcome(SampleStockScenario scenario) {
        SampleStockClient stockClient = Mockito.mock(SampleStockClient.class);
        Mockito.when(stockClient.getStock(scenario.sku()))
                .thenReturn(new SampleStockRecord(scenario.sku(), scenario.availableUnits()));

        SampleStockService service = new SampleStockService(stockClient);
        SampleOrderRequest request = new SampleOrderRequest(scenario.sku(), scenario.requestedQuantity());

        if (scenario.expectRejected()) {
            assertThatThrownBy(() -> service.reserve(request))
                    .isInstanceOf(SampleOutOfStockException.class);
        } else {
            SampleOrderConfirmation confirmation = service.reserve(request);
            assertThat(confirmation.sku()).isEqualTo(scenario.sku());
            assertThat(confirmation.quantity()).isEqualTo(scenario.requestedQuantity());
        }
    }
}

record SampleStockScenario(
        String name,
        String sku,
        int availableUnits,
        int requestedQuantity,
        boolean expectRejected
) {
    @Override
    public String toString() {
        return name;
    }
}
