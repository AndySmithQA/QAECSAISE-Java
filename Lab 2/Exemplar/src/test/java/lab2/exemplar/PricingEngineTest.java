package lab2.exemplar;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingEngineTest {
    @Test
    void calculateItemPricePremiumTierAppliesFivePercentDiscount() {
        PricingEngine engine = new PricingEngine();
        Map<String, Double> item = Map.of("price", 100.0);

        double result = engine.calculateItemPrice(item, 2, "premium");

        assertEquals(190.0, result, 0.00001);
    }

    @Test
    void applyDiscountsSave10ReducesTotalByTenPercent() {
        PricingEngine engine = new PricingEngine();
        List<Map<String, Double>> cart = List.of(Map.of("price", 100.0, "quantity", 1.0));

        double result = engine.applyDiscounts(cart, List.of("SAVE10"));

        assertEquals(90.0, result, 0.00001);
    }
}
