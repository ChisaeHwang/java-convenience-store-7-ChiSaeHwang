package store.domain.store.dao;

import store.domain.store.domain.Product;
import store.domain.store.util.ResourceLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 상품 정보를 저장하고 관리하는 저장소.
 * 싱글톤 패턴을 사용하여 하나의 인스턴스만 유지한다.
 */
public class ProductRepository {
    private final List<Product> products;

    private ProductRepository() {
        this.products = new ArrayList<>();
        initializeProducts();
    }

    private static class LazyHolder {
        private static final ProductRepository INSTANCE = new ProductRepository();
    }

    public static ProductRepository getInstance() {
        return LazyHolder.INSTANCE;
    }

    private void initializeProducts() {
        products.addAll(ResourceLoader.loadProducts());
    }

    /**
     * 상품명과 수량 조건으로 구매 가능한 상품을 찾는다.
     *
     * @param name 상품명
     * @param quantity 필요한 수량
     * @return 조건을 만족하는 상품
     */
    public Optional<Product> findByNameAndQuantityGreaterThanEqual(String name, int quantity) {
        return products.stream()
                .filter(product -> product.getName().equals(name))
                .filter(product -> product.hasEnoughStock(quantity))
                .findFirst();
    }

    /**
     * 상품명으로 첫 번째 상품을 조회한다.
     *
     * @param name 상품명
     * @return 해당 상품명을 가진 첫 번째 상품
     */
    public Optional<Product> findFirstByName(String name) {
        return products.stream()
                .filter(product -> product.getName().equals(name))
                .findFirst();
    }
}