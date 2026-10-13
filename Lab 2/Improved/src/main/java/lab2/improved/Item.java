package lab2.improved;

import java.math.BigDecimal;

public class Item {
    private final String id;
    private final String name;
    private final BigDecimal price;
    private final String category;
    private final boolean taxExempt;

    public Item(String id, String name, BigDecimal price, String category) {
        this(id, name, price, category, false);
    }

    public Item(String id, String name, BigDecimal price, String category, boolean taxExempt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.taxExempt = taxExempt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public boolean isTaxExempt() {
        return taxExempt;
    }
}
