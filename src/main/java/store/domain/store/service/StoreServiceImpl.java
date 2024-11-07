package store.domain.store.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import store.domain.store.dao.ProductRepository;
import store.domain.store.dao.PromotionRepository;
import store.domain.store.domain.Product;
import store.domain.store.domain.Promotion;
import store.domain.store.domain.Receipt;
import store.domain.store.domain.ReceiptItem;
import store.domain.store.dto.request.PurchaseRequest;
import store.domain.store.dto.response.ProductResponse;
import store.domain.store.dto.response.ReceiptResponse;

public class StoreServiceImpl implements StoreService {
    private static final String ERROR_INVALID_PROMOTION = "[ERROR] 유효하지 않은 프로모션입니다.";
    private static final String ERROR_INSUFFICIENT_STOCK = "[ERROR] 재고가 부족합니다.";
    private static final String ERROR_NO_ITEMS = "[ERROR] 구매 상품이 없습니다.";

    private static final StoreServiceImpl instance = new StoreServiceImpl();
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;

    private StoreServiceImpl() {
        this.productRepository = ProductRepository.getInstance();
        this.promotionRepository = PromotionRepository.getInstance();
    }

    public static StoreServiceImpl getInstance() {
        return instance;
    }

    @Override
    public ReceiptResponse purchase(List<PurchaseRequest> requests, boolean usePromotion, boolean hasMembership) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException(ERROR_NO_ITEMS);
        }

        ArrayList<ReceiptItem> items = new ArrayList<>();
        ArrayList<ReceiptItem> freeItems = new ArrayList<>();

        // 각 요청 처리
        for (PurchaseRequest request : requests) {
            processRequest(request, items, freeItems, usePromotion);
        }

        // 프로모션 적용된 상품의 수량을 ReceiptItem에 표시
        items.forEach(item -> {
            if (hasValidPromotion(item.getName())) {
                item.markAsPromotionItem();
            }
        });

        return ReceiptResponse.from(Receipt.of(items, freeItems, hasMembership));
    }

    @Override
    public List<ProductResponse> getProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Override
    public boolean canAddPromotionPurchase(String productName, int quantity) {
        Optional<Product> promotionProduct = productRepository.findPromotionProduct(productName)
                .filter(Product::hasValidPromotion);
        
        if (promotionProduct.isEmpty()) {
            return false;
        }

        Promotion promotion = promotionRepository.findByName(promotionProduct.get().getPromotionName())
                .orElse(null);
        if (promotion == null) {
            return false;
        }

        // 구매 수량이 프로모션 구매 수량과 정확히 일치할 때만 추가 구매 메시지 표시
        return quantity == promotion.getBuyCount();
    }

    @Override
    public int getNormalPurchaseQuantity(String productName, int quantity) {
        Optional<Product> promotionProduct = productRepository.findPromotionProduct(productName)
                .filter(Product::hasValidPromotion);

        if (promotionProduct.isEmpty()) {
            return 0;
        }

        Promotion promotion = promotionRepository.findByName(promotionProduct.get().getPromotionName())
                .orElse(null);
        if (promotion == null) {
            return 0;
        }

        int promotionStock = promotionProduct.get().getQuantity();
        int possibleSets = promotionStock / (promotion.getBuyCount() + promotion.getGetCount());
        int maxPromotionQuantity = possibleSets * (promotion.getBuyCount() + promotion.getGetCount());
        
        return quantity > maxPromotionQuantity ? quantity - maxPromotionQuantity : 0;
    }

    private void processRequest(
            PurchaseRequest request,
            List<ReceiptItem> items,
            List<ReceiptItem> freeItems,
            boolean usePromotion
    ) {
        Optional<Product> promotionProduct = productRepository.findPromotionProduct(request.getProductName())
                .filter(Product::hasValidPromotion);

        if (!usePromotion || promotionProduct.isEmpty()) {
            processNormalPurchase(request, items);
            return;
        }

        Promotion promotion = promotionRepository.findByName(promotionProduct.get().getPromotionName())
                .orElseThrow(() -> new IllegalArgumentException(ERROR_INVALID_PROMOTION));

        handlePromotionPurchase(request, promotionProduct.get(), promotion, items, freeItems);
    }

    private void handlePromotionPurchase(
            PurchaseRequest request,
            Product promotionProduct,
            Promotion promotion,
            List<ReceiptItem> items,
            List<ReceiptItem> freeItems
    ) {
        int promotionStock = promotionProduct.getQuantity();
        
        if (promotionStock >= request.getQuantity()) {
            processPromotionPurchase(request, promotionProduct, promotion, items, freeItems);
            return;
        }

        // 프로모션 재고로 처리 가능한 만큼 처리
        if (promotionStock > 0) {
            processPromotionPurchase(
                PurchaseRequest.of(request.getProductName(), promotionStock),
                promotionProduct,
                promotion,
                items,
                freeItems
            );
        }

        // 남은 수량은 일반 재고로 처리
        int normalQuantity = request.getQuantity() - promotionStock;
        if (normalQuantity > 0) {
            processNormalPurchase(
                PurchaseRequest.of(request.getProductName(), normalQuantity),
                items
            );
        }
    }

    private void processPromotionPurchase(
            PurchaseRequest request,
            Product promotionProduct,
            Promotion promotion,
            List<ReceiptItem> items,
            List<ReceiptItem> freeItems
    ) {
        int quantity = Math.min(request.getQuantity(), promotionProduct.getQuantity());
        
        // 입력한 수량 그대로 items에 추가
        items.add(ReceiptItem.of(
                request.getProductName(),
                quantity,
                promotionProduct.getPrice()
        ));

        // 프로모션 세트 계산 (증정품만을 위해)
        int fullSetCount = quantity / (promotion.getBuyCount() + promotion.getGetCount());
        int freeQuantity = fullSetCount * promotion.getGetCount();

        // 증정 상품 추가
        if (freeQuantity > 0) {
            freeItems.add(ReceiptItem.createFreeItem(
                    request.getProductName(),
                    freeQuantity
            ));
        }

        productRepository.save(promotionProduct.removeStock(quantity));
    }

    private void processNormalPurchase(
            PurchaseRequest request,
            List<ReceiptItem> items
    ) {
        Product normalProduct = productRepository.findNormalProduct(request.getProductName())
                .filter(p -> p.hasEnoughStock(request.getQuantity()))
                .orElseThrow(() -> new IllegalArgumentException(ERROR_INSUFFICIENT_STOCK));

        items.add(ReceiptItem.of(
                request.getProductName(),
                request.getQuantity(),
                normalProduct.getPrice()
        ));

        productRepository.save(normalProduct.removeStock(request.getQuantity()));
    }

    private boolean hasValidPromotion(String productName) {
        return productRepository.findPromotionProduct(productName)
                .filter(Product::hasValidPromotion)
                .isPresent();
    }
}