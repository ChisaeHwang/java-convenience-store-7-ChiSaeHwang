package store.domain.store.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import store.domain.store.domain.Receipt;
import store.domain.store.domain.ReceiptItem;

/**
 * 구매 영수증 응답.
 */
public class ReceiptResponse {
    private final List<PurchaseResponse> items;
    private final List<PurchaseResponse> freeItems;
    private final int totalAmount;
    private final int promotionDiscountAmount;
    private final int membershipDiscountAmount;
    private final int finalAmount;

    private ReceiptResponse(
            List<PurchaseResponse> items,
            List<PurchaseResponse> freeItems,
            int totalAmount,
            int promotionDiscountAmount,
            int membershipDiscountAmount,
            int finalAmount
    ) {
        this.items = new ArrayList<>(items);
        this.freeItems = new ArrayList<>(freeItems);
        this.totalAmount = totalAmount;
        this.promotionDiscountAmount = promotionDiscountAmount;
        this.membershipDiscountAmount = membershipDiscountAmount;
        this.finalAmount = finalAmount;
    }

    public static ReceiptResponse from(Receipt receipt) {
        return new ReceiptResponse(
                convertToItemResponses(receipt.getItems()),
                convertToItemResponses(receipt.getFreeItems()),
                receipt.getTotalAmount(),
                receipt.getPromotionDiscountAmount(),
                receipt.getMembershipDiscountAmount(),
                receipt.getFinalAmount()
        );
    }

    private static List<PurchaseResponse> convertToItemResponses(List<ReceiptItem> items) {
        return items.stream()
                .map(item -> PurchaseResponse.of(
                        item.getName(),
                        item.getQuantity(),
                        item.getAmount()
                ))
                .collect(Collectors.toList());
    }

    public List<PurchaseResponse> getItems() {
        return new ArrayList<>(items);
    }

    public List<PurchaseResponse> getFreeItems() {
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
