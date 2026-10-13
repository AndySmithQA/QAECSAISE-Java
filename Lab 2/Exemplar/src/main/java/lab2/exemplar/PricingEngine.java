package lab2.exemplar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typical AI-generated pricing engine for Lab 2.
 * Happy-path only: no validation, no tax, and discounts compound on the running total.
 */
public class PricingEngine {
    private final Map<String, Double> taxRates = new LinkedHashMap<>();
    private final Map<String, Double> discountCodes = new LinkedHashMap<>();

    public PricingEngine() {
        taxRates.put("US", 0.08);
        taxRates.put("CA", 0.12);
        discountCodes.put("SAVE10", 0.10);
        discountCodes.put("WELCOME", 0.15);
    }

    public Map<String, Double> getTaxRates() {
        return taxRates;
    }

    public Map<String, Double> getDiscountCodes() {
        return discountCodes;
    }

    public double calculateItemPrice(Map<String, Double> item, int quantity, String customerTier) {
        double basePrice = item.get("price") * quantity;
        if ("premium".equals(customerTier)) {
            basePrice *= 0.95;
        }
        return basePrice;
    }

    public double applyDiscounts(List<Map<String, Double>> cart, List<String> promotionalCodes) {
        double total = 0;
        for (Map<String, Double> item : cart) {
            total += item.get("price") * item.get("quantity");
        }
        for (String code : promotionalCodes) {
            if (discountCodes.containsKey(code)) {
                total *= (1 - discountCodes.get(code));
            }
        }
        return total;
    }
}
