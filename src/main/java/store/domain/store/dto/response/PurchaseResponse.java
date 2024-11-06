package store.domain.store.dto.response;

public class PurchaseResponse {

    private final String name;
    private final int quantity;
    private final int amount;

    private PurchaseResponse(String name, int quantity, int amount) {
        this.name = name;
        this.quantity = quantity;
        this.amount = amount;
    }

    public static PurchaseResponse of(
            final String name,
            final int quantity,
            final int amount
    ) {
        return new PurchaseResponse(name, quantity, amount);
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getAmount() {
        return amount;
    }

}
