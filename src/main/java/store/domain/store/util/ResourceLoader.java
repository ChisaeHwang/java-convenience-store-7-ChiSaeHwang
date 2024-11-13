package store.domain.store.util;

import store.domain.store.domain.Product;
import store.domain.store.domain.Promotion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 리소스 파일에서 데이터를 읽어오는 유틸리티 클래스.
 */
public final class ResourceLoader {
  private static final String PRODUCTS_FILE = "products.md";
  private static final String PROMOTIONS_FILE = "promotions.md";
  private static final String DELIMITER = ",";
  
  // Products 관련 상수
  private static final int PRODUCT_EXPECTED_COLUMNS = 4;
  private static final int PRODUCT_NAME_INDEX = 0;
  private static final int PRODUCT_PRICE_INDEX = 1;
  private static final int PRODUCT_QUANTITY_INDEX = 2;
  private static final int PRODUCT_PROMOTION_INDEX = 3;
  
  // Promotions 관련 상수
  private static final int PROMOTION_EXPECTED_COLUMNS = 5;
  private static final int PROMOTION_NAME_INDEX = 0;
  private static final int PROMOTION_BUY_INDEX = 1;
  private static final int PROMOTION_GET_INDEX = 2;
  private static final int PROMOTION_START_DATE_INDEX = 3;
  private static final int PROMOTION_END_DATE_INDEX = 4;

  private ResourceLoader() {
  }

  /**
   * products.md 파일에서 상품 정보를 읽어 Product 객체 리스트로 반환한다.
   *
   * @return 상품 목록
   * @throws IllegalStateException 파일을 찾을 수 없거나 읽기에 실패한 경우
   */
  public static List<Product> loadProducts() {
    List<Product> products = new ArrayList<>();
    
    try (InputStream inputStream = getResourceFileStream(PRODUCTS_FILE)) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      skipHeader(reader);
      
      String line;
      while ((line = reader.readLine()) != null) {
        String[] values = line.split(DELIMITER);
        validateProductValues(values);
        
        products.add(Product.of(
            values[PRODUCT_NAME_INDEX].trim(),
            Integer.parseInt(values[PRODUCT_PRICE_INDEX].trim()),
            Integer.parseInt(values[PRODUCT_QUANTITY_INDEX].trim()),
            values[PRODUCT_PROMOTION_INDEX].trim()
        ));
      }
      
      return products;
    } catch (IOException e) {
      throw new IllegalStateException("[ERROR] 상품 정보를 불러오는데 실패했습니다.", e);
    }
  }

  /**
   * promotions.md 파일에서 프로모션 정보를 읽어 Promotion 객체 리스트로 반환한다.
   *
   * @return 프로모션 목록
   * @throws IllegalStateException 파일을 찾을 수 없거나 읽기에 실패한 경우
   */
  public static List<Promotion> loadPromotions() {
    List<Promotion> promotions = new ArrayList<>();
    
    try (InputStream inputStream = getResourceFileStream(PROMOTIONS_FILE)) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      skipHeader(reader);
      
      String line;
      while ((line = reader.readLine()) != null) {
        String[] values = line.split(DELIMITER);
        validatePromotionValues(values);
        
        try {
          promotions.add(Promotion.of(
              values[PROMOTION_NAME_INDEX].trim(),
              Integer.parseInt(values[PROMOTION_BUY_INDEX].trim()),
              Integer.parseInt(values[PROMOTION_GET_INDEX].trim()),
              LocalDate.parse(values[PROMOTION_START_DATE_INDEX].trim()),
              LocalDate.parse(values[PROMOTION_END_DATE_INDEX].trim())
          ));
        } catch (DateTimeParseException e) {
          throw new IllegalStateException("[ERROR] 프로모션 날짜 형식이 올바르지 않습니다.", e);
        }
      }
      
      return promotions;
    } catch (IOException e) {
      throw new IllegalStateException("[ERROR] 프로모션 정보를 불러오는데 실패했습니다.", e);
    }
  }

  private static InputStream getResourceFileStream(String fileName) {
    InputStream inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream(fileName);
    if (inputStream == null) {
      throw new IllegalStateException(String.format("[ERROR] %s 파일을 찾을 수 없습니다.", fileName));
    }
    return inputStream;
  }

  private static void skipHeader(BufferedReader reader) throws IOException {
    reader.readLine();
  }


  /**
   * @throws IllegalStateException 형식이 올바르지 않은 경우
   */

  private static void validateProductValues(String[] values) {
    if (values.length != PRODUCT_EXPECTED_COLUMNS) {
      throw new IllegalStateException("[ERROR] 상품 정보 형식이 올바르지 않습니다.");
    }
  }

  private static void validatePromotionValues(String[] values) {
    if (values.length != PROMOTION_EXPECTED_COLUMNS) {
      throw new IllegalStateException("[ERROR] 프로모션 정보 형식이 올바르지 않습니다.");
    }
  }
}
