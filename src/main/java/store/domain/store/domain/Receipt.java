package store.domain.store.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 구매 영수증을 표현하는 클래스.
 * 구매/증정 상품 내역과 금액 정보를 포함한다.
 */
public final class Receipt {
    private final List<ReceiptItem> items;
    private final List<ReceiptItem> freeItems;
    private final int totalAmount;
    private final int promotionDiscountAmount;
    private final int membershipDiscountAmount;
    private final int finalAmount;

    private Receipt(List<ReceiptItem> items, List<ReceiptItem> freeItems, boolean hasMembership) {
        this.items = new ArrayList<>(items);
        this.freeItems = new ArrayList<>(freeItems);
        this.totalAmount = calculateTotalAmount();
        this.promotionDiscountAmount = calculatePromotionDiscountAmount();
        this.membershipDiscountAmount = calculateMembershipDiscountAmount(hasMembership);
        this.finalAmount = calculateFinalAmount();
    }

    /**
     * 구매 내역과 멤버십 여부로 영수증을 생성한다.
     */
    public static Receipt of(
            final List<ReceiptItem> items,
            final List<ReceiptItem> freeItems,
            final boolean hasMembership
    ) {
        return new Receipt(items, freeItems, hasMembership);
    }

    /**
     * 구매 상품의 총 금액을 계산한다.
     */
    private int calculateTotalAmount() {
        return items.stream()
                .mapToInt(ReceiptItem::getAmount)
                .sum();
    }

    /**
     * 프로모션 할인 금액(증정 상품의 가치)을 계산한다.
     */
    private int calculatePromotionDiscountAmount() {
        return freeItems.stream()
                .mapToInt(item -> item.getQuantity() * items.stream()
                        .filter(purchaseItem -> purchaseItem.getName().equals(item.getName()))
                        .findFirst()
                        .map(ReceiptItem::getUnitPrice)
                        .orElse(0))
                .sum();
    }

    /**
     * 멤버십 할인 금액을 계산한다.
     * 증정 상품을 제외한 구매 금액의 30%, 최대 8,000원
     */
    private int calculateMembershipDiscountAmount(boolean hasMembership) {
        if (!hasMembership) {
            return 0;
        }

        // 프로모션이 없는 상품의 금액만 합산
        int discountableAmount = items.stream()
                .filter(item -> !hasPromotionItem(item.getName()))
                .mapToInt(ReceiptItem::getAmount)
                .sum();

        int discountAmount = (int) (discountableAmount * 0.3);
        return Math.min(discountAmount, 8000);
    }

    private boolean hasPromotionItem(String productName) {
        return freeItems.stream()
                .anyMatch(item -> item.getName().equals(productName));
    }

    /**
     * 최종 결제 금액을 계산한다.
     */
    private int calculateFinalAmount() {
        return totalAmount - promotionDiscountAmount - membershipDiscountAmount;
    }

    public List<ReceiptItem> getItems() {
        return new ArrayList<>(items);
    }

    public List<ReceiptItem> getFreeItems() {
        return new ArrayList<>(freeItems);
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public int getPromotionDiscountAmount() {
        return promotionDiscountAmount;
    }

    public int getMembershipDiscountAmount() {
        return membershipDiscountAmount;
    }

    public int getFinalAmount() {
        return finalAmount;
    }
}