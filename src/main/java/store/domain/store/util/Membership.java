package store.domain.store.util;

/**
 * 멤버십 할인을 처리하는 클래스.
 */
public final class Membership {
    private static final double DISCOUNT_RATE = 0.3;  // 30% 할인
    private static final int MAX_DISCOUNT_AMOUNT = 8_000;  // 최대 8,000원 할인

    private Membership() {
    }

    /**
     * 멤버십 할인 금액을 계산한다.
     * 프로모션이 적용되지 않은 금액의 30%를 할인하되, 최대 8,000원까지만 할인된다.
     *
     * @param originalAmount 프로모션 적용 후 금액
     * @return 할인 금액
     */
    public static int calculateDiscountAmount(int originalAmount) {
        int discountAmount = (int) (originalAmount * DISCOUNT_RATE);
        return Math.min(discountAmount, MAX_DISCOUNT_AMOUNT);
    }
}
