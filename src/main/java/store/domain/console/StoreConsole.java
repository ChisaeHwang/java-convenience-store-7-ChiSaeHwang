package store.domain.console;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import store.domain.console.util.CommandReader;
import store.domain.console.util.CommandWriter;
import store.domain.store.dto.request.PurchaseRequest;
import store.domain.store.dto.response.PurchaseResponse;
import store.domain.store.dto.response.ReceiptResponse;
import store.domain.store.presentation.StoreController;
import store.domain.store.service.StoreService;
import store.domain.store.dto.response.ProductResponse;

public class StoreConsole {
    private static final String WELCOME_MESSAGE = "안녕하세요. W편의점입니다.";
    private static final String PRODUCT_LIST_MESSAGE = "현재 보유하고 있는 상품입니다.";
    private static final String PRODUCT_FORMAT = "- %s %,d원 %d개%s";
    private static final String PURCHASE_INPUT_MESSAGE =
            "\n구매하실 상품명과 수량을 입력해 주세요. (예: [사이다-2],[감자칩-1])";
    private static final String PROMOTION_CONFIRM_MESSAGE =
            "\n현재 %s은(는) %d개를 무료로 더 받을 수 있습니다. 추가하시겠습니까? (Y/N)";
    private static final String NORMAL_PURCHASE_CONFIRM_MESSAGE =
            "\n현재 %s %d개는 프로모션 할인이 적용되지 않습니다. 그래도 구매하시겠습니까? (Y/N)";
    private static final String MEMBERSHIP_CONFIRM_MESSAGE = "\n멤버십 할인을 받으시겠습니까? (Y/N)";
    private static final String ERROR_INVALID_INPUT = "[ERROR] 입력이 올바르지 않습니다.";
    private static final String RECEIPT_HEADER = "\n===========W 편의점=============";
    private static final String RECEIPT_ITEMS_HEADER = "상품명\t\t수량\t금액";
    private static final String RECEIPT_FREE_HEADER = "===========증\t정=============";
    private static final String RECEIPT_FOOTER = "==============================";
    private static final String RECEIPT_ITEM_FORMAT = "%s\t\t%d\t%,d";
    private static final String RECEIPT_FREE_FORMAT = "%s\t\t%d";
    private static final String RECEIPT_TOTAL_FORMAT = "총구매액\t\t%d\t%,d";
    private static final String RECEIPT_DISCOUNT_FORMAT = "%s\t\t\t-%,d";
    private static final String RECEIPT_FINAL_FORMAT = "내실돈\t\t\t %,d";
    private static final String CONTINUE_SHOPPING_MESSAGE = "\n감사합니다. 구매하고 싶은 다른 상품이 있나요? (Y/N)";

    private final StoreController controller = StoreController.getInstance(StoreService.getInstance());

    public void run() {
        try {
            boolean continueShopping = true;
            while (continueShopping) {
                processPurchase();
                CommandWriter.write(CONTINUE_SHOPPING_MESSAGE);
                continueShopping = readYesNo();
                CommandWriter.write("");
            }
        } catch (IllegalArgumentException e) {
            CommandWriter.write(e.getMessage());
        }
    }

    private void processPurchase() {
        CommandWriter.write(WELCOME_MESSAGE);
        printProductList();

        List<PurchaseRequest> requests = inputPurchaseRequests();
        boolean usePromotion = confirmPromotionUse(requests);
        boolean hasMembership = confirmMembership();

        ReceiptResponse receipt = controller.purchase(requests, usePromotion, hasMembership);
        printReceipt(receipt);
    }

    private void printProductList() {
        CommandWriter.write(PRODUCT_LIST_MESSAGE);
        CommandWriter.write("");
        List<ProductResponse> products = controller.getProducts();
        
        for (ProductResponse product : products) {
            String promotionMark = product.hasPromotion() ? " " + product.getPromotionName() : "";
            CommandWriter.writeFormat(PRODUCT_FORMAT, 
                    product.getName(),
                    product.getPrice(),
                    product.getQuantity(),
                    promotionMark);
        }
    }

    private List<PurchaseRequest> inputPurchaseRequests() {
        CommandWriter.write(PURCHASE_INPUT_MESSAGE);
        String input = CommandReader.read();


        String pattern = "\\[([^-]+-\\d+)\\](,\\[([^-]+-\\d+)\\])*";
        if (!input.matches(pattern)) {
            throw new IllegalArgumentException(ERROR_INVALID_INPUT);
        }

        return parseRequests(input);
    }

    private List<PurchaseRequest> parseRequests(String input) {
        return Arrays.stream(input.split(","))
                .map(request -> request.replaceAll("[\\[\\]]", ""))
                .map(request -> request.split("-"))
                .map(parts -> PurchaseRequest.of(parts[0], Integer.parseInt(parts[1])))
                .collect(Collectors.toList());
    }

    private boolean confirmPromotionUse(List<PurchaseRequest> requests) {
        for (PurchaseRequest request : requests) {
            if (controller.canAddPromotionPurchase(request.getProductName(), request.getQuantity())) {
                CommandWriter.writeFormat(PROMOTION_CONFIRM_MESSAGE,
                        request.getProductName(), request.getQuantity());
                boolean usePromotion = readYesNo();
                if (usePromotion) {
                    // 프로모션 수락 시 증정품 추가
                    request.addPromotionQuantity(request.getQuantity());
                }
                return usePromotion;
            }

            int normalQuantity = controller.getNormalPurchaseQuantity(
                    request.getProductName(), request.getQuantity());
            if (normalQuantity > 0) {
                CommandWriter.writeFormat(NORMAL_PURCHASE_CONFIRM_MESSAGE,
                        request.getProductName(), normalQuantity);
                return readYesNo();
            }
        }
        return true;
    }

    private boolean confirmMembership() {
        CommandWriter.write(MEMBERSHIP_CONFIRM_MESSAGE);
        return readYesNo();
    }

    private boolean readYesNo() {
        String input = CommandReader.read().toUpperCase();
        if (!input.matches("[YN]")) {
            throw new IllegalArgumentException(ERROR_INVALID_INPUT);
        }
        return input.equals("Y");
    }

    private void printReceipt(ReceiptResponse receipt) {
        CommandWriter.write(RECEIPT_HEADER);
        CommandWriter.write(RECEIPT_ITEMS_HEADER);
        
        receipt.getItems().stream()
               .collect(Collectors.groupingBy(
                   PurchaseResponse::getName,
                   LinkedHashMap::new,  // 순서 유지를 위해 LinkedHashMap 사용
                   Collectors.collectingAndThen(
                       Collectors.toList(),
                       items -> {
                           int totalQuantity = items.stream().mapToInt(PurchaseResponse::getQuantity).sum();
                           int totalAmount = items.stream().mapToInt(PurchaseResponse::getAmount).sum();
                           return new AbstractMap.SimpleEntry<>(totalQuantity, totalAmount);
                       }
                   )
               ))
               .forEach((name, entry) -> 
                   CommandWriter.writeFormat(RECEIPT_ITEM_FORMAT,
                       name, entry.getKey(), entry.getValue())
               );

        if (!receipt.getFreeItems().isEmpty()) {
            CommandWriter.write(RECEIPT_FREE_HEADER);
            receipt.getFreeItems().forEach(this::printFreeItem);
        }

        CommandWriter.write(RECEIPT_FOOTER);
        printAmountInfo(receipt);
    }

    private void printFreeItem(PurchaseResponse item) {
        CommandWriter.writeFormat(RECEIPT_FREE_FORMAT,
                item.getName(), item.getQuantity());
    }

    private void printAmountInfo(ReceiptResponse receipt) {
        // 구매 수량만 합산 (증정품은 제외)
        int totalQuantity = receipt.getItems().stream()
                .mapToInt(PurchaseResponse::getQuantity)
                .sum();
        
        CommandWriter.writeFormat(RECEIPT_TOTAL_FORMAT, 
                totalQuantity, receipt.getTotalAmount());
        
        CommandWriter.writeFormat(RECEIPT_DISCOUNT_FORMAT, 
                "행사할인", receipt.getPromotionDiscountAmount());
        CommandWriter.writeFormat(RECEIPT_DISCOUNT_FORMAT, 
                "멤버십할인", receipt.getMembershipDiscountAmount());
        
        CommandWriter.writeFormat(RECEIPT_FINAL_FORMAT, receipt.getFinalAmount());
    }
}