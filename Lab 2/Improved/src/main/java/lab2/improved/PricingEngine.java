package lab2.improved;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference pricing engine: decimal money, tier discounts, promotional rules, and tax exemptions.
 */
public class PricingEngine {
    private final Map<String, BigDecimal> taxRates = new LinkedHashMap<>();
    private final Map<CustomerTier, BigDecimal> tierDiscounts = new LinkedHashMap<>();
    private final Map<String, DiscountRule> discountRules = new LinkedHashMap<>();

    public PricingEngine() {
        taxRates.put("US", new BigDecimal("0.08"));
        taxRates.put("CA", new BigDecimal("0.12"));
        taxRates.put("UK", new BigDecimal("0.20"));

        tierDiscounts.put(CustomerTier.BASIC, BigDecimal.ZERO);
        tierDiscounts.put(CustomerTier.PREMIUM, new BigDecimal("0.05"));
        tierDiscounts.put(CustomerTier.VIP, new BigDecimal("0.10"));

        discountRules.put("SAVE10", new DiscountRule("SAVE10", DiscountType.PERCENTAGE, new BigDecimal("0.10")));
        discountRules.put("WELCOME", new DiscountRule("WELCOME", DiscountType.PERCENTAGE, new BigDecimal("0.15"), new BigDecimal("50")));
        discountRules.put("NEWUSER", new DiscountRule("NEWUSER", DiscountType.FIXED_AMOUNT, new BigDecimal("20"), new BigDecimal("100")));
    }

    /** Calculate price for a single item with quantity and tier discount. */
    public BigDecimal calculateItemPrice(Item item, int quantity, CustomerTier customerTier) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Item price cannot be negative");
        }

        BigDecimal basePrice = item.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal tierDiscount = tierDiscounts.getOrDefault(customerTier, BigDecimal.ZERO);
        BigDecimal discountedPrice = basePrice.multiply(BigDecimal.ONE.subtract(tierDiscount));
        return discountedPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /** Apply discounts and return a detailed breakdown. */
    public PricingBreakdown applyDiscounts(List<CartItem> cart, List<String> promotionalCodes, CustomerTier customerTier) {
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart) {
            subtotal = subtotal.add(calculateItemPrice(cartItem.getItem(), cartItem.getQuantity(), customerTier));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        List<String> appliedCodes = new ArrayList<>();
        for (String code : promotionalCodes) {
            DiscountRule rule = discountRules.get(code);
            if (rule == null) {
                logWarning("Invalid promotional code: " + code);
                continue;
            }
            if (subtotal.compareTo(rule.getMinOrderAmount()) < 0) {
                logInfo("Code " + code + " requires minimum order of " + rule.getMinOrderAmount());
                continue;
            }

            BigDecimal codeDiscount;
            if (rule.getDiscountType() == DiscountType.PERCENTAGE) {
                codeDiscount = subtotal.multiply(rule.getValue());
            } else if (rule.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                codeDiscount = rule.getValue().min(subtotal);
            } else {
                codeDiscount = BigDecimal.ZERO;
            }

            discountAmount = discountAmount.add(codeDiscount);
            appliedCodes.add(code);
        }

        BigDecimal finalTotal = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        return new PricingBreakdown(subtotal, discountAmount, finalTotal, appliedCodes);
    }

    /** Calculate tax with exemption handling. The subtotal argument is retained to match the lab signature. */
    public BigDecimal calculateTax(BigDecimal subtotal, String location, List<CartItem> cart) {
        if (!taxRates.containsKey(location)) {
            throw new IllegalArgumentException("Tax rate not available for location: " + location);
        }

        BigDecimal taxRate = taxRates.get(location);
        BigDecimal taxableAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cart) {
            if (!cartItem.getItem().isTaxExempt()) {
                taxableAmount = taxableAmount.add(cartItem.getItem().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }
        }
        return taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    private static void logInfo(String message) {
        System.err.println("INFO: " + message);
    }

    private static void logWarning(String message) {
        System.err.println("WARNING: " + message);
    }
}
