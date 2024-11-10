package store.domain.store.dto.response;

import store.domain.store.domain.Product;

public class ProductResponse {
    private final String name;
    private final int price;
    private final String quantity;
    private final String promotionName;

    private ProductResponse(String name, int price, String quantity, String promotionName) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.promotionName = promotionName;
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getName(),
                product.getPrice(),
                product.getQuantity() + "개",
                product.getPromotionName()
        );
    }

    public static ProductResponse createOutOfStock(Product product) {
        return new ProductResponse(
                product.getName(),
                product.getPrice(),
                "재고 없음",
                product.getPromotionName()
        );
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public boolean hasPromotion() {
        return promotionName != null && !promotionName.equals("null");
    }
}
