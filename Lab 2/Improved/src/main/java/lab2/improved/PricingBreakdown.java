package lab2.improved;

import java.math.BigDecimal;
import java.util.List;

public class PricingBreakdown {
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal finalTotal;
    private final List<String> appliedCodes;

    public PricingBreakdown(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal finalTotal, List<String> appliedCodes) {
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.finalTotal = finalTotal;
        this.appliedCodes = appliedCodes;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public List<String> getAppliedCodes() {
        return appliedCodes;
    }
}
