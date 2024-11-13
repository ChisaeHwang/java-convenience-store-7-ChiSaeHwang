package store.domain.store.domain;

import camp.nextstep.edu.missionutils.DateTimes;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상품에 적용되는 프로모션을 표현하는 클래스.
 */
public final class Promotion {
    private final String name;
    private final int buyCount;
    private final int getCount;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private Promotion(
            String name,
            int buyCount,
            int getCount,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateCounts(buyCount, getCount);
        validateDates(startDate, endDate);

        this.name = name;
        this.buyCount = buyCount;
        this.getCount = getCount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Promotion 객체를 생성한다.
     *
     * @param name 프로모션명
     * @param buyCount 구매 수량
     * @param getCount 증정 수량
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 생성된 프로모션 객체
     */
    public static Promotion of(
            final String name,
            final int buyCount,
            final int getCount,
            final LocalDate startDate,
            final LocalDate endDate
    ) {
        return new Promotion(name, buyCount, getCount, startDate, endDate);
    }

    /**
     * 프로모션이 현재 유효한지 확인한다.
     *
     * @return 프로모션 유효 여부
     */
    public boolean isValid() {
        return isValid(DateTimes.now());
    }

    /**
     * 프로모션이 주어진 시간에 유효한지 확인한다.
     *
     * @param dateTime 확인할 시간
     * @return 프로모션 유효 여부
     */
    public boolean isValid(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        return date.isEqual(startDate) || 
               date.isEqual(endDate) || 
               (date.isAfter(startDate) && date.isBefore(endDate));
    }

    /**
     * 주어진 구매 수량에 대해 무료로 제공되는 수량을 계산한다.
     *
     * @param quantity 구매 수량
     * @param now
     * @return 무료 제공 수량
     */
    public int calculateFreeQuantity(int quantity, LocalDateTime now) {
        if (!isValid()) {
            return 0;
        }
        int promotionSets = quantity / buyCount;
        return promotionSets * getCount;
    }

    private void validateCounts(int buyCount, int getCount) {
        if (buyCount <= 0) {
            throw new IllegalArgumentException("[ERROR] 구매 수량은 0보다 커야 합니다.");
        }
        if (getCount <= 0) {
            throw new IllegalArgumentException("[ERROR] 증정 수량은 0보다 커야 합니다.");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("[ERROR] 날짜는 null일 수 없습니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("[ERROR] 시작일이 종료일보다 늦을 수 없습니다.");
        }
    }

    public String getName() {
        return name;
    }

    public int getBuyCount() {
        return buyCount;
    }

    public int getGetCount() {
        return getCount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}