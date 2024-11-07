package store.domain.console;

import java.util.Arrays;
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
    private static final String PRODUCT_LIST_MESSAGE = "현재 보유하고 있는 상품입니다.\n";
    private static final String PRODUCT_FORMAT = "- %s %,d원 %d개%s";
    private static final String PURCHASE_INPUT_MESSAGE =
            "\n구매하실 상품명과 수량을 입력해 주세요. (예: [사이다-2],[감자칩-1])";
    private static final String PROMOTION_CONFIRM_MESSAGE =
            "\n현재 %s은(는) %d개를 무료로 더 받을 수 있습니다. 추가하시겠습니까? (Y/N)";
    private static final String NORMAL_PURCHASE_CONFIRM_MESSAGE =
            "\n현재 %s %d개는 프로모션 할인이 적용되지 않습니다. 그래도 구매하시겠습니까? (Y/N)";
    private static final String MEMBERSHIP_CONFIRM_MESSAGE = "\n멤버십을 적용하시겠습니까? (Y/N)";
    private static final String ERROR_INVALID_INPUT = "[ERROR] 입력이 올바르지 않습니다.";

    private final StoreController controller = StoreController.getInstance(StoreService.getInstance());

    public void run() {
        try {
            processPurchase();
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
        List<ProductResponse> products = controller.getProducts();
        
        for (ProductResponse product : products) {
            String promotionMark = product.hasPromotion() ? " " + product.getPromotionName() : "";
            CommandWriter.writeFormat(PRODUCT_FORMAT, 
                    product.getName(),
                    product.getPrice(),
                    product.getQuantity(),
                    promotionMark);
        }
        CommandWriter.write("");
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
                return readYesNo();
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
        CommandWriter.write("\n영수증");
        CommandWriter.write("=".repeat(40));

        CommandWriter.write("구매 상품");
        receipt.getItems().forEach(this::printPurchaseItem);

        if (!receipt.getFreeItems().isEmpty()) {
            CommandWriter.write("\n증정 상품");
            receipt.getFreeItems().forEach(this::printFreeItem);
        }

        CommandWriter.write("\n금액 정보");
        printAmountInfo(receipt);
        CommandWriter.write("=".repeat(40));
    }

    private void printPurchaseItem(PurchaseResponse item) {
        CommandWriter.writeFormat("%s - %d개 : %,d원",
                item.getName(), item.getQuantity(), item.getAmount());
    }

    private void printFreeItem(PurchaseResponse item) {
        CommandWriter.writeFormat("%s - %d개", item.getName(), item.getQuantity());
    }

    private void printAmountInfo(ReceiptResponse receipt) {
        CommandWriter.writeFormat("총 구매액: %,d원", receipt.getTotalAmount());
        if (receipt.getPromotionDiscountAmount() > 0) {
            CommandWriter.writeFormat("행사 할인: -%,d원", receipt.getPromotionDiscountAmount());
        }
        if (receipt.getMembershipDiscountAmount() > 0) {
            CommandWriter.writeFormat("멤버십 할인: -%,d원", receipt.getMembershipDiscountAmount());
        }
        CommandWriter.writeFormat("최종 결제 금액: %,d원", receipt.getFinalAmount());
    }
}