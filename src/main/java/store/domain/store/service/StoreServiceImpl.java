package store.domain.store.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String ERROR_INSUFFICIENT_STOCK = "[ERROR] 재고 수량을 초과하여 구매할 수 없습니다. 다시 입력해 주세요.";
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
        Map<String, Promotion> promotionMap = new HashMap<>();

        // 프로모션 적용되지 않는 수량과 금액 계산
        int normalQuantity = 0;
        int normalAmount = 0;
        
        if (requests.size() == 1) {  // 단일 상품 구매일 때만
            PurchaseRequest request = requests.get(0);
            normalQuantity = getNormalPurchaseQuantity(request.getProductName(), request.getQuantity());
            if (normalQuantity > 0) {
                Product product = productRepository.findByNameAndQuantityGreaterThanEqual(
                        request.getProductName(), request.getQuantity())
                        .orElseThrow(() -> new IllegalArgumentException(ERROR_INSUFFICIENT_STOCK));
                normalAmount = normalQuantity * product.getPrice();
            }
        }

        // 각 요청 처리
        for (PurchaseRequest request : requests) {
            if (usePromotion) {
                productRepository.findPromotionProduct(request.getProductName())
                        .filter(Product::hasValidPromotion)
                        .ifPresent(product -> {
                            promotionRepository.findByName(product.getPromotionName())
                                    .ifPresent(promotion -> promotionMap.put(request.getProductName(), promotion));
                        });
            }
            
            processRequest(request, items, freeItems, usePromotion);
        }

        // 프로모션을 실제로 사용한 상품만 표시
        items.stream()
                .filter(item -> freeItems.stream()
                        .anyMatch(free -> free.getName().equals(item.getName())))
                .forEach(ReceiptItem::markAsPromotionItem);

        return ReceiptResponse.from(Receipt.of(
                items, 
                freeItems, 
                hasMembership, 
                promotionMap, 
                normalQuantity, 
                normalAmount  
        ));
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

        Optional<Promotion> promotion = promotionRepository.findByName(promotionProduct.get().getPromotionName());
        
        // 프로모션이 없거나 유효하지 않은 경우 false 반환
        if (promotion.isEmpty() || !promotion.get().isValid()) {  // 여기서 간 체크
            return false;
        }

        // 구매 수량이 프로모션 구매 수량의 배수인지 확인
        if (quantity % promotion.get().getBuyCount() != 0) {
            return false;
        }

        // 증정품을 포함한 전체 필요 수량 계산
        int sets = quantity / promotion.get().getBuyCount();
        int totalQuantityNeeded = quantity + (sets * promotion.get().getGetCount());
        
        int promotionStock = promotionProduct.get().getQuantity();
        return promotionStock >= totalQuantityNeeded;
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

        // 가능한 세트 수 계산
        int possibleSets = promotionStock / (promotion.getBuyCount() + promotion.getGetCount());

        // 세트로 처리되는 전체 수량 (2+1이면 세트당 3개씩)
        int promotionSetQuantity = possibleSets * (promotion.getBuyCount() + promotion.getGetCount());

        // 요청 수량에서 세트로 처리되는 수량을 뺀 나머지가 일반 구매
        int normalPurchaseQuantity = quantity - promotionSetQuantity;
        
        return normalPurchaseQuantity;
    }

    @Override
    public int getPromotionFreeCount(String productName) {
        return productRepository.findPromotionProduct(productName)
                .filter(Product::hasValidPromotion)
                .flatMap(product -> promotionRepository.findByName(product.getPromotionName()))
                .filter(Promotion::isValid)  // 여기서 기간 체크
                .map(Promotion::getGetCount)
                .orElse(0);
    }

    private void processRequest(
            PurchaseRequest request,
            List<ReceiptItem> items,
            List<ReceiptItem> freeItems,
            boolean usePromotion
    ) {
        // 항상 프로모션 재고 먼저 확인
        Optional<Product> promotionProduct = productRepository.findPromotionProduct(request.getProductName())
                .filter(Product::hasValidPromotion);

        if (promotionProduct.isPresent()) {
            Promotion promotion = promotionRepository.findByName(promotionProduct.get().getPromotionName())
                    .orElseThrow(() -> new IllegalArgumentException(ERROR_INVALID_PROMOTION));

            // usePromotion이 false여도 프로모션 재고 먼저 소진
            handlePromotionPurchase(request, promotionProduct.get(), promotion, items, freeItems);
            return;
        }

        // 프로모션 재고가 없을 때만 일반 재고 사용
        processNormalPurchase(request, items);
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
        // 프로모션이 유효하지 않으면 일반 구매로 처리
        if (!promotion.isValid()) {
            processNormalPurchase(request, items);
            return;
        }

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

        // 증정 상품 추가 (프로모션이 유효한 경우에만)
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