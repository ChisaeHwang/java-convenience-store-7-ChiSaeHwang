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
    private final Promotion promotion;

    private Product(String name, int price, int quantity, Promotion promotion) {
        validateProduct(name, price, quantity);
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.promotion = promotion;
    }

    /**
     * 상품 객체를 생성한다.
     *
     * @param name 상품명
     * @param price 가격
     * @param quantity 수량
     * @param promotion 프로모션
     * @return 생성된 상품 객체
     * @throws IllegalArgumentException 유효하지 않은 입력값이 있는 경우
     */
    public static Product of(
            final String name,
            final int price,
            final int quantity,
            final Promotion promotion
    ) {
        return new Product(name, price, quantity, promotion);
    }

    /**
     * 프로모션을 적용하여 무료로 제공되는 수량을 계산한다.
     *
     * @param quantity 구매 수량
     * @return 무료 제공 수량
     */
    public int calculateFreeQuantity(int quantity) {
        if (!hasValidPromotion()) {
            return 0;
        }
        return promotion.calculateFreeQuantity(quantity);
    }

    /**
     * 프로모션 적용 가능 여부를 확인한다.
     *
     * @return 프로모션 적용 가능 여부
     */
    public boolean hasValidPromotion() {
        return promotion != null && promotion.isValid();
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
        return new Product(this.name, this.price, this.quantity - quantity, this.promotion);
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

    public Promotion getPromotion() {
        return promotion;
    }
}
