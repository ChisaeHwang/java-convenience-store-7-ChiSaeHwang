package store.domain.store.util;

import store.domain.store.domain.Product;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 상품 정보를 파일에서 읽어오는 유틸리티 클래스.
 */
public final class ResourceLoader {
    private static final String PRODUCTS_FILE = "products.csv";
    private static final String DELIMITER = ",";
    private static final int EXPECTED_COLUMNS = 4;
    private static final int NAME_INDEX = 0;
    private static final int PRICE_INDEX = 1;
    private static final int QUANTITY_INDEX = 2;
    private static final int PROMOTION_INDEX = 3;

    private ResourceLoader() {
    }

    /**
     * products.csv 파일에서 상품 정보를 읽어 Product 객체 리스트로 반환한다.
     *
     * @return 상품 목록
     * @throws IllegalStateException 파일을 찾을 수 없거나 읽기에 실패한 경우
     */
    public static List<Product> loadProducts() {
        List<Product> products = new ArrayList<>();

        try (InputStream inputStream = ResourceLoader.class.getClassLoader()
                .getResourceAsStream(PRODUCTS_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("[ERROR] 상품 정보 파일을 찾을 수 없습니다.");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            // 첫 줄은 헤더이므로 건너뛰기
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                products.add(createProduct(line));
            }

            return products;
        } catch (IOException e) {
            throw new IllegalStateException("[ERROR] 상품 정보를 불러오는데 실패했습니다.", e);
        }
    }

    private static Product createProduct(String line) {
        String[] values = line.split(DELIMITER);
        validateValues(values);

        return Product.of(
                values[NAME_INDEX].trim(),
                Integer.parseInt(values[PRICE_INDEX].trim()),
                Integer.parseInt(values[QUANTITY_INDEX].trim()),
                values[PROMOTION_INDEX].trim()
        );
    }

    private static void validateValues(String[] values) {
        if (values.length != EXPECTED_COLUMNS) {
            throw new IllegalStateException("[ERROR] 상품 정보 형식이 올바르지 않습니다.");
        }
    }
}
