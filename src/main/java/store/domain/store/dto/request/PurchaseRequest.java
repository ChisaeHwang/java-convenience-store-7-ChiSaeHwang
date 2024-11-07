package store.domain.store.dto.request;

/**
 * 상품 구매 요청 정보.
 */
public class PurchaseRequest {
    private final String productName;
    private int quantity;

    private PurchaseRequest(String productName, int quantity) {
        this.productName = productName;
        this.quantity = quantity;
    }

    public static PurchaseRequest of(
            final String productName,
            final int quantity
    ) {
        return new PurchaseRequest(productName, quantity);
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void addPromotionQuantity(int additionalQuantity) {
        this.quantity += additionalQuantity;
    }
}
