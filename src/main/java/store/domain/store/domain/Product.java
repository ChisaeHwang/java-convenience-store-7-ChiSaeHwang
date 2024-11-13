package store.domain.store.domain;

import java.util.Objects;

/**
 * 편의점에서 판매되는 상품을 표현하는 클래스.
 * 상품의 이름, 가격, 재고 수량, 적용 가능한 프로모션 정보를 관리한다.
 */
public class Product {
    private final String name;
    private final int price;
    private final int quantity;
    private final String promotionName;

    private Product(String name, int price, int quantity, String promotionName) {
        validateProduct(name, price, quantity);
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.promotionName = promotionName;
    }

    /**
     * 상품 객체를 생성한다.
     *
     * @param name 상품명
     * @param price 가격
     * @param quantity 수량
     * @param promotionName 프로모션 이름
     * @return 생성된 상품 객체
     * @throws IllegalArgumentException 유효하지 않은 입력값이 있는 경우
     */
    public static Product of(
            final String name,
            final int price,
            final int quantity,
            final String promotionName
    ) {
        return new Product(name, price, quantity, promotionName);
    }

    /**
     * 프로모션 적용 가능 여부를 확인한다.
     *
     * @return 프로모션 적용 가능 여부
     */
    public boolean hasValidPromotion() {
        return promotionName != null && !promotionName.equals("null");
    }


    /**
     * 재고를 차감한 새로운 상품 객체를 반환한다.
     *
     * @param quantity 차감할 수량
     * @return 재고가 차감된 새로운 상품 객체
     * @throws IllegalArgumentException 재고가 부족한 경우
     */
    public Product removeStock(int quantity) {
        if (!hasEnoughStock(quantity)) {
            throw new IllegalArgumentException("[ERROR] 재고가 부족합니다.");
        }
        return new Product(this.name, this.price, this.quantity - quantity, this.promotionName);
    }

    private void validateProduct(String name, int price, int quantity) {
        validateName(name);
        validatePrice(price);
        validateQuantity(quantity);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("[ERROR] 상품명은 비어있을 수 없습니다.");
        }
    }

    private void validatePrice(int price) {
        if (price <= 0) {
            throw new IllegalArgumentException("[ERROR] 상품 가격은 0보다 커야 합니다.");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("[ERROR] 상품 수량은 0보다 작을 수 없습니다.");
        }
    }

    /**
     * 요청된 수량만큼의 재고가 있는지 확인한다.
     *
     * @param requestedQuantity 요청 수량
     * @return 재고 충분 여부
     */
    public boolean hasEnoughStock(int requestedQuantity) {
        return this.quantity >= requestedQuantity;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getPromotionName() {
        return promotionName;
    }
}
