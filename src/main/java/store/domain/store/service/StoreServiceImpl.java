package store.domain.store.service;

import camp.nextstep.edu.missionutils.DateTimes;
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
import store.domain.store.domain.Receipt.NormalPurchaseInfo;
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
        validateRequests(requests);
        validateStockForAllRequests(requests);

        ArrayList<ReceiptItem> items = new ArrayList<>();
        ArrayList<ReceiptItem> freeItems = new ArrayList<>();
        Map<String, Promotion> promotionMap = createPromotionMap(requests, usePromotion);
        Map<String, NormalPurchaseInfo> normalPurchaseMap = createNormalPurchaseMap(requests);

        processAllRequests(requests, items, freeItems, usePromotion);
        markPromotionItems(items, freeItems);

        return createReceiptResponse(items, freeItems, hasMembership, promotionMap, normalPurchaseMap);
    }

    private void validateRequests(List<PurchaseRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException(ERROR_NO_ITEMS);
        }
    }

    private void validateStockForAllRequests(List<PurchaseRequest> requests) {
        requests.forEach(this::validateTotalStock);
    }

    private Map<String, Promotion> createPromotionMap(List<PurchaseRequest> requests, boolean usePromotion) {
        Map<String, Promotion> promotionMap = new HashMap<>();
        if (usePromotion) {
            requests.forEach(request -> addToPromotionMap(request, promotionMap));
        }
        return promotionMap;
    }

    private void addToPromotionMap(PurchaseRequest request, Map<String, Promotion> promotionMap) {
        productRepository.findPromotionProduct(request.getProductName())
                .filter(Product::hasValidPromotion)
                .ifPresent(product -> {
                    promotionRepository.findByName(product.getPromotionName())
                            .ifPresent(promotion -> promotionMap.put(request.getProductName(), promotion));
                });
    }

    private Map<String, NormalPurchaseInfo> createNormalPurchaseMap(List<PurchaseRequest> requests) {
        Map<String, NormalPurchaseInfo> normalPurchaseMap = new HashMap<>();
        requests.forEach(request -> addToNormalPurchaseMap(request, normalPurchaseMap));
        return normalPurchaseMap;
    }

    private void addToNormalPurchaseMap(PurchaseRequest request, Map<String, NormalPurchaseInfo> normalPurchaseMap) {
        int normalQuantity = getNormalPurchaseQuantity(request.getProductName(), request.getQuantity());
        if (normalQuantity > 0) {
            Product product = findProductForNormalPurchase(request);
            int normalAmount = normalQuantity * product.getPrice();
            normalPurchaseMap.put(request.getProductName(), 
                new Receipt.NormalPurchaseInfo(normalQuantity, normalAmount));
        }
    }

    private Product findProductForNormalPurchase(PurchaseRequest request) {
        return productRepository.findByNameAndQuantityGreaterThanEqual(
                request.getProductName(), request.getQuantity())
                .orElseThrow(() -> new IllegalArgumentException(ERROR_INSUFFICIENT_STOCK));
    }

    private void processAllRequests(
            List<PurchaseRequest> requests,
            List<ReceiptItem> items,
            List<ReceiptItem> freeItems,
            boolean usePromotion
    ) {
        requests.forEach(request -> processRequest(request, items, freeItems, usePromotion));
    }

    private void markPromotionItems(List<ReceiptItem> items, List<ReceiptItem> freeItems) {
        items.stream()
                .filter(item -> hasMatchingFreeItem(item, freeItems))
                .forEach(ReceiptItem::markAsPromotionItem);
    }

    private boolean hasMatchingFreeItem(ReceiptItem item, List<ReceiptItem> freeItems) {
        return freeItems.stream()
                .anyMatch(free -> free.getName().equals(item.getName()));
    }

    private ReceiptResponse createReceiptResponse(
            List<ReceiptItem> items,
            List<ReceiptItem> freeItems,
            boolean hasMembership,
            Map<String, Promotion> promotionMap,
            Map<String, NormalPurchaseInfo> normalPurchaseMap
    ) {
        return ReceiptResponse.from(Receipt.of(
                items, 
                freeItems, 
                hasMembership, 
                promotionMap, 
                normalPurchaseMap
        ));
    }

    @Override
    public List<ProductResponse> getProducts() {
        List<Product> products = productRepository.findAll();
        List<ProductResponse> responses = new ArrayList<>();
        
        // 프로모션 상품 처리
        products.stream()
                .filter(Product::hasValidPromotion)
                .forEach(product -> {
                    responses.add(ProductResponse.from(product));  // 재고 있는 버전
                    
                    // 같은 상품의 재고 없는 일반 버전 추가
                    Product outOfStockProduct = Product.of(
                        product.getName(),
                        product.getPrice(),
                        0,
                        null
                    );
                    responses.add(ProductResponse.createOutOfStock(outOfStockProduct));
                });
        
        // 일반 상품 처리
        products.stream()
                .filter(product -> !product.hasValidPromotion())
                .forEach(product -> responses.add(ProductResponse.from(product)));
        
        return responses;
    }

    @Override
    public boolean canAddPromotionPurchase(String productName, int quantity) {
        Optional<Promotion> promotion = findValidPromotion(productName);
        
        // 프로모션이 없거나 유효하지 않은 경우
        if (promotion.isEmpty() || !promotion.get().isValid(DateTimes.now())) {
            return false;
        }
        
        if (!isValidPromotionQuantity(quantity, promotion.get())) {
            return false;
        }
        
        return hasEnoughPromotionStock(productName, quantity, promotion.get());
    }

    private Optional<Promotion> findValidPromotion(String productName) {
        return productRepository.findPromotionProduct(productName)
                .filter(Product::hasValidPromotion)
                .flatMap(product -> promotionRepository.findByName(product.getPromotionName()))
                .filter(Promotion::isValid);
    }

    private boolean isValidPromotionQuantity(int quantity, Promotion promotion) {
        return quantity % promotion.getBuyCount() == 0;
    }

    private boolean hasEnoughPromotionStock(String productName, int quantity, Promotion promotion) {
        int sets = quantity / promotion.getBuyCount();
        int totalNeeded = quantity + (sets * promotion.getGetCount());
        return getPromotionStock(productName) >= totalNeeded;
    }

    private int getPromotionStock(String productName) {
        return productRepository.findPromotionProduct(productName)
                .map(Product::getQuantity)
                .orElse(0);
    }

    @Override
    public int getNormalPurchaseQuantity(String productName, int quantity) {
        Optional<Promotion> promotion = findValidPromotion(productName);
        if (promotion.isEmpty()) {
            return 0;
        }
        
        return calculateNormalPurchaseQuantity(productName, quantity, promotion.get());
    }

    private int calculateNormalPurchaseQuantity(String productName, int quantity, Promotion promotion) {
        int promotionStock = getPromotionStock(productName);
        int possibleSets = calculatePossibleSets(promotionStock, promotion);
        int promotionSetQuantity = calculatePromotionSetQuantity(possibleSets, promotion);
        return quantity - promotionSetQuantity;
    }

    private int calculatePossibleSets(int stock, Promotion promotion) {
        return stock / (promotion.getBuyCount() + promotion.getGetCount());
    }

    private int calculatePromotionSetQuantity(int sets, Promotion promotion) {
        return sets * (promotion.getBuyCount() + promotion.getGetCount());
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

            // usePromotion이 false여도 프로모션 재고 먼저 진
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
        if (!promotion.isValid()) {
            processNormalPurchase(request, items);
            return;
        }
        
        addPromotionPurchaseItems(request, promotionProduct, promotion, items, freeItems);
        updatePromotionStock(promotionProduct, request.getQuantity());
    }

    private void addPromotionPurchaseItems(
            PurchaseRequest request,
            Product product,
            Promotion promotion,
            List<ReceiptItem> items,
            List<ReceiptItem> freeItems
    ) {
        int quantity = Math.min(request.getQuantity(), product.getQuantity());
        items.add(createPurchaseItem(request.getProductName(), quantity, product.getPrice()));
        addFreeItemsIfApplicable(request.getProductName(), quantity, promotion, freeItems);
    }

    private ReceiptItem createPurchaseItem(String name, int quantity, int price) {
        return ReceiptItem.of(name, quantity, price);
    }

    private void addFreeItemsIfApplicable(
            String productName,
            int quantity,
            Promotion promotion,
            List<ReceiptItem> freeItems
    ) {
        int freeQuantity = calculateFreeQuantity(productName, quantity);
        if (freeQuantity > 0) {
            freeItems.add(ReceiptItem.createFreeItem(productName, freeQuantity));
        }
    }

    private int calculateFreeQuantity(String productName, int quantity) {
        return findValidPromotion(productName)
                .map(promotion -> promotion.calculateFreeQuantity(quantity, DateTimes.now()))
                .orElse(0);
    }

    private void updatePromotionStock(Product product, int quantity) {
        productRepository.save(product.removeStock(quantity));
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

    private void validateTotalStock(PurchaseRequest request) {
        Optional<Product> promotionProduct = productRepository.findPromotionProduct(request.getProductName())
                .filter(Product::hasValidPromotion);
        
        int totalAvailableStock = 0;
        
        // 프로모션 재고 확인
        if (promotionProduct.isPresent()) {
            totalAvailableStock += promotionProduct.get().getQuantity();
        }
        
        // 일반 재고 확인
        Optional<Product> normalProduct = productRepository.findNormalProduct(request.getProductName());
        if (normalProduct.isPresent()) {
            totalAvailableStock += normalProduct.get().getQuantity();
        }
        
        // 총 재고가 요청 수량보다 적으면 예외 발생
        if (totalAvailableStock < request.getQuantity()) {
            throw new IllegalArgumentException(ERROR_INSUFFICIENT_STOCK);
        }
    }
}
