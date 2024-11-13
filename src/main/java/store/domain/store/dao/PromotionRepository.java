package store.domain.store.dao;

import store.domain.store.domain.Promotion;
import store.domain.store.util.ResourceLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 프로모션 정보를 저장하고 관리하는 저장소.
 * 싱글톤 패턴을 사용하여 하나의 인스턴스만 유지한다.
 */
public class PromotionRepository {
    private final List<Promotion> promotions;

    private PromotionRepository() {
        this.promotions = new ArrayList<>();
        initializePromotions();
    }

    private static class LazyHolder {
        private static final PromotionRepository INSTANCE = new PromotionRepository();
    }

    public static PromotionRepository getInstance() {
        return LazyHolder.INSTANCE;
    }

    private void initializePromotions() {
        promotions.addAll(ResourceLoader.loadPromotions());
    }

    /**
     * 프로모션명으로 프로모션을 조회한다.
     *
     * @param name 프로모션명
     * @return 해당하는 프로모션 객체
     */
    public Optional<Promotion> findByName(String name) {
        return promotions.stream()
                .filter(promotion -> promotion.getName().equals(name))
                .findFirst();
    }

    /**
     * 현재 유효한 프로모션 목록을 반환한다.
     *
     * @return 유효한 프로모션 목록
     */
    public List<Promotion> findAllValid() {
        return promotions.stream()
                .filter(Promotion::isValid)
                .toList();
    }

    /**
     * 프로모션을 저장하거나 업데이트한다.
     * 동일한 프로모션명이 있다면 교체한다.
     *
     * @param promotion 저장할 프로모션
     * @return 저장된 프로모션
     */
    public Promotion save(Promotion promotion) {
        promotions.removeIf(p -> p.getName().equals(promotion.getName()));
        promotions.add(promotion);
        return promotion;
    }

    /**
     * 여러 프로모션을 한번에 저장한다.
     *
     * @param promotions 저장할 프로모션 목록
     * @return 저장된 프로모션 목록
     */
    public List<Promotion> saveAll(List<Promotion> promotions) {
        promotions.forEach(this::save);
        return promotions;
    }
}