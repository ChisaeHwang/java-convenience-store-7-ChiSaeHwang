package store.domain.store.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 구매 영수증을 표현하는 클래스.
 * 구매/증정 상품 내역과 금액 정보를 포함한다.
 */
public final class Receipt {
    private final List<ReceiptItem> items;
    private final List<ReceiptItem> freeItems;
    private final Map<String, Promotion> promotionMap;
    private final int totalAmount;
    private final int promotionDiscountAmount;
    private final int membershipDiscountAmount;
    private final int finalAmount;

    private Receipt(
            List<ReceiptItem> items,
            List<ReceiptItem> freeItems,
            boolean hasMembership,
            Map<String, Promotion> promotionMap
    ) {
        this.items = new ArrayList<>(items);
        this.freeItems = new ArrayList<>(freeItems);
        this.promotionMap = promotionMap;
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
            final boolean hasMembership,
            final Map<String, Promotion> promotionMap
    ) {
        return new Receipt(items, freeItems, hasMembership, promotionMap);
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
        // 증정품 수량만큼만 할인 적용
        return freeItems.stream()
                .mapToInt(item -> {
                    // 증정품의 원래 가격 찾기
                    ReceiptItem originalItem = items.stream()
                            .filter(i -> i.getName().equals(item.getName()))
                            .findFirst()
                            .orElse(null);
                    
                    if (originalItem == null) {
                        return 0;
                    }
                    
                    // 증정 수량 * 단가 = 할인 금액
                    return item.getQuantity() * originalItem.getUnitPrice();
                })
                .sum();
    }

    /**2
     * 멤버십 할인 금액을 계산한다.
     * 증정 상품을 제외한 구매 금액의 30%, 최대 8,000원
     */
    private int calculateMembershipDiscountAmount(boolean hasMembership) {
        if (!hasMembership) {
            return 0;
        }

        // 프로모션이 적용된 상품은 멤버십 할인에서 제외
        int discountableAmount = items.stream()
                .filter(item -> !item.isPromotionItem())  // isPromotionItem으로 체크
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