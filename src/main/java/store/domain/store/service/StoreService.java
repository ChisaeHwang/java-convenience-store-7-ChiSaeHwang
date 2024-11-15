package store.domain.store.service;

import java.util.List;
import store.domain.store.dto.request.PurchaseRequest;
import store.domain.store.dto.response.ProductResponse;
import store.domain.store.dto.response.ReceiptResponse;

public interface StoreService {
    static StoreService getInstance() {
        return StoreServiceImpl.getInstance();
    }

    /**
     * 구매를 진행하고 영수증을 생성한다.
     */
    ReceiptResponse purchase(List<PurchaseRequest> requests, boolean usePromotion, boolean hasMembership);

    /**
     * 현재 판매 중인 모든 상품 목록을 반환
     */
    List<ProductResponse> getProducts();

    /**
     * 프로모션 추가 구매가 가능한지 확인
     */
    boolean canAddPromotionPurchase(String productName, int quantity);

    /**
     * 정가 구매가 필요한 수량을 반환
     */
    int getNormalPurchaseQuantity(String productName, int quantity);

    /**
     * 프로모션의 증정 수량을 반환
     */
    int getPromotionFreeCount(String productName);
}
