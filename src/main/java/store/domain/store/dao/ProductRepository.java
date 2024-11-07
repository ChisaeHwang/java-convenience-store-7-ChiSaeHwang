package store.domain.store.dao;

import store.domain.store.domain.Product;
import store.domain.store.util.ResourceLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.LinkedList;

/**
 * 상품 정보를 저장하고 관리하는 저장소.
 * 싱글톤 패턴을 사용하여 하나의 인스턴스만 유지한다.
 */
public class ProductRepository {
    private final List<Product> products;

    private ProductRepository() {
        this.products = new LinkedList<>();
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
     * product.md 파일 순서대로 모든 상품을 조회한다.
     */
    public List<Product> findAll() {
        List<Product> allProducts = new ArrayList<>();
        
        for (Product product : products) {
            allProducts.add(product);
            
            if (product.hasValidPromotion()) {
                Product normalProduct = Product.of(
                    product.getName(),
                    product.getPrice(),
                    0,
                    null
                );
                allProducts.add(normalProduct);
            }
        }
        
        return allProducts;
    }


    /**
     * 프로모션이 적용된 상품을 조회한다.
     */
    public Optional<Product> findPromotionProduct(String name) {
        return products.stream()
                .filter(product -> product.getName().equals(name))
                .filter(Product::hasValidPromotion)
                .findFirst();
    }

    /**
     * 프로모션이 적용되지 않은 일반 상품을 조회한다.
     */
    public Optional<Product> findNormalProduct(String name) {
        return products.stream()
                .filter(product -> product.getName().equals(name))
                .filter(product -> !product.hasValidPromotion())
                .findFirst();
    }

    /**
     * 상품을 저장하거나 업데이트한다.
     * 동일한 상품명과 프로모션을 가진 상품이 있다면 교체한다.
     *
     * @param product 저장할 상품
     * @return 저장된 상품
     */
    public Product save(Product product) {
        int index = findProductIndex(product);
        if (index >= 0) {
            products.set(index, product);
        }
        return product;
    }

    private int findProductIndex(Product product) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getName().equals(product.getName()) &&
                products.get(i).getPromotionName() == product.getPromotionName()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 여러 상품을 한번에 저장한다.
     *
     * @param products 저장할 상품 목록
     * @return 저장된 상품 목록
     */
    public List<Product> saveAll(List<Product> products) {
        products.forEach(this::save);
        return products;
    }
}