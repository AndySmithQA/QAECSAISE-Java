package lab2.improved;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingEngineTest {
    private final PricingEngine engine = new PricingEngine();
    private final Item sampleItem = new Item("1", "Test Product", new BigDecimal("100.00"), "electronics");

    @Test
    void calculateItemPriceBasicTierChargesFullPrice() {
        assertDecimal("200.00", engine.calculateItemPrice(sampleItem, 2, CustomerTier.BASIC));
    }

    @Test
    void calculateItemPricePremiumTierAppliesFivePercentDiscount() {
        assertDecimal("190.00", engine.calculateItemPrice(sampleItem, 2, CustomerTier.PREMIUM));
    }

    @Test
    void calculateItemPriceZeroQuantityThrows() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> engine.calculateItemPrice(sampleItem, 0, CustomerTier.BASIC));
        assertTrue(error.getMessage().contains("Quantity must be positive"));
    }

    @Test
    void calculateItemPriceNegativeQuantityThrows() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> engine.calculateItemPrice(sampleItem, -1, CustomerTier.BASIC));
        assertTrue(error.getMessage().contains("Quantity must be positive"));
    }

    @Test
    void applyDiscountsValidCodeReturnsBreakdown() {
        PricingBreakdown result = engine.applyDiscounts(List.of(new CartItem(sampleItem, 1)), List.of("SAVE10"), CustomerTier.BASIC);

        assertDecimal("100.00", result.getSubtotal());
        assertDecimal("10.00", result.getDiscountAmount());
        assertDecimal("90.00", result.getFinalTotal());
        assertTrue(result.getAppliedCodes().contains("SAVE10"));
    }

    @Test
    void applyDiscountsMinimumOrderNotMetAppliesNoDiscount() {
        Item lowPriceItem = new Item("2", "Cheap Item", new BigDecimal("30.00"), "misc");
        PricingBreakdown result = engine.applyDiscounts(
                List.of(new CartItem(lowPriceItem, 1)), List.of("WELCOME"), CustomerTier.BASIC);

        assertDecimal("0.00", result.getDiscountAmount());
        assertTrue(result.getAppliedCodes().isEmpty());
    }

    @Test
    void applyDiscountsInvalidCodeAppliesNoDiscount() {
        PricingBreakdown result = engine.applyDiscounts(
                List.of(new CartItem(sampleItem, 1)), List.of("INVALID"), CustomerTier.BASIC);

        assertDecimal("0.00", result.getDiscountAmount());
        assertTrue(result.getAppliedCodes().isEmpty());
    }

    @Test
    void applyDiscountsMultipleCodesStacksPercentageAndFixedAmount() {
        Item expensiveItem = new Item("3", "Expensive Item", new BigDecimal("200.00"), "luxury");
        PricingBreakdown result = engine.applyDiscounts(
                List.of(new CartItem(expensiveItem, 1)), List.of("SAVE10", "NEWUSER"), CustomerTier.BASIC);

        BigDecimal expected = new BigDecimal("200.00").multiply(new BigDecimal("0.10")).add(new BigDecimal("20.00"));
        assertEquals(0, result.getDiscountAmount().compareTo(expected));
    }

    @Test
    void applyDiscountsEmptyCartThrows() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> engine.applyDiscounts(List.of(), List.of("SAVE10"), CustomerTier.BASIC));
        assertTrue(error.getMessage().contains("Cart cannot be empty"));
    }

    @Test
    void calculateTaxStandardUsRateChargesEightPercent() {
        BigDecimal tax = engine.calculateTax(new BigDecimal("100.00"), "US", List.of(new CartItem(sampleItem, 1)));
        assertDecimal("8.00", tax);
    }

    @Test
    void calculateTaxExemptItemsTaxesOnlyTaxableAmount() {
        Item taxableItem = new Item("1", "Taxable", new BigDecimal("100.00"), "electronics");
        Item exemptItem = new Item("2", "Exempt", new BigDecimal("50.00"), "medicine", true);
        BigDecimal tax = engine.calculateTax(
                new BigDecimal("150.00"),
                "US",
                List.of(new CartItem(taxableItem, 1), new CartItem(exemptItem, 1)));
        assertDecimal("8.00", tax);
    }

    @Test
    void calculateTaxUnknownLocationThrows() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> engine.calculateTax(new BigDecimal("100.00"), "INVALID", List.of(new CartItem(sampleItem, 1))));
        assertTrue(error.getMessage().contains("Tax rate not available"));
    }

    @Test
    void calculateItemPriceMaintainsDecimalPrecision() {
        Item item = new Item("1", "Precision Test", new BigDecimal("33.33"), "test");
        assertDecimal("94.99", engine.calculateItemPrice(item, 3, CustomerTier.PREMIUM));
    }

    @Test
    void applyDiscountsFixedDiscountDoesNotMakeTotalNegative() {
        Item cheapItem = new Item("1", "Cheap", new BigDecimal("10.00"), "test");
        PricingBreakdown result = engine.applyDiscounts(
                List.of(new CartItem(cheapItem, 1)), List.of("NEWUSER"), CustomerTier.BASIC);

        // Same assertions as the Python lab. NEWUSER requires a 100 minimum, so this 10 order is unchanged.
        assertDecimal("0.00", result.getFinalTotal());
        assertDecimal("10.00", result.getDiscountAmount());
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)), actual + " != " + expected);
    }
}
