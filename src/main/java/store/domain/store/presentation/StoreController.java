package store.domain.store.presentation;

import java.util.List;
import store.domain.store.dto.request.PurchaseRequest;
import store.domain.store.dto.response.ProductResponse;
import store.domain.store.dto.response.ReceiptResponse;
import store.domain.store.service.StoreService;

public class StoreController {
    private static StoreController instance;
    private final StoreService storeService;

    private StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    public static StoreController getInstance(StoreService storeService) {
        if (instance == null) {
            instance = new StoreController(storeService);
        }
        return instance;
    }

    public ReceiptResponse purchase(List<PurchaseRequest> requests, boolean usePromotion, boolean hasMembership) {
        return storeService.purchase(requests, usePromotion, hasMembership);
    }

    public List<ProductResponse> getProducts() {
        return storeService.getProducts();
    }

    public boolean canAddPromotionPurchase(String productName, int quantity) {
        return storeService.canAddPromotionPurchase(productName, quantity);
    }

    public int getNormalPurchaseQuantity(String productName, int quantity) {
        return storeService.getNormalPurchaseQuantity(productName, quantity);
    }

    public int getPromotionFreeCount(String productName) {
        return storeService.getPromotionFreeCount(productName);
    }
}
