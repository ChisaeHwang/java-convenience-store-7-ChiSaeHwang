package store.domain.store.domain;

/**
 * 영수증의 개별 항목을 표현하는 클래스.
 * 구매 상품 내역과 증정 상품 내역에 모두 사용됨.
 */
public final class ReceiptItem {
    private final String name;
    private final int quantity;
    private final int unitPrice;
    private final int amount;

    private ReceiptItem(String name, int quantity, int unitPrice) {
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = quantity * unitPrice;
    }

    /**
     * 구매 상품 항목을 생성한다.
     */
    public static ReceiptItem of(
            final String name,
            final int quantity,
            final int unitPrice
    ) {
        return new ReceiptItem(name, quantity, unitPrice);
    }

    /**
     * 증정 상품 항목을 생성한다. (단가는 있지만 금액은 0원)
     */
    public static ReceiptItem createFreeItem(
            final String name,
            final int quantity
    ) {
        return new ReceiptItem(name, quantity, 0);  // 증정품은 금액이 0원
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getAmount() {
        return amount;
    }
}