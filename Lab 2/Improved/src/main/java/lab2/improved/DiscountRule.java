package lab2.improved;

import java.math.BigDecimal;
import java.util.List;

public class DiscountRule {
    private final String code;
    private final DiscountType discountType;
    private final BigDecimal value;
    private final BigDecimal minOrderAmount;
    private final Integer maxUsesPerCustomer;
    private final List<String> validCategories;

    public DiscountRule(String code, DiscountType discountType, BigDecimal value) {
        this(code, discountType, value, BigDecimal.ZERO, null, null);
    }

    public DiscountRule(String code, DiscountType discountType, BigDecimal value, BigDecimal minOrderAmount) {
        this(code, discountType, value, minOrderAmount, null, null);
    }

    public DiscountRule(
            String code,
            DiscountType discountType,
            BigDecimal value,
            BigDecimal minOrderAmount,
            Integer maxUsesPerCustomer,
            List<String> validCategories) {
        this.code = code;
        this.discountType = discountType;
        this.value = value;
        this.minOrderAmount = minOrderAmount;
        this.maxUsesPerCustomer = maxUsesPerCustomer;
        this.validCategories = validCategories;
    }

    public String getCode() {
        return code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public Integer getMaxUsesPerCustomer() {
        return maxUsesPerCustomer;
    }

    public List<String> getValidCategories() {
        return validCategories;
    }
}
