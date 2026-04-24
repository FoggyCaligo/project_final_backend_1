package com.today.fridge.ingredient.repository;

import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.entity.UserIngredient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserIngredientRepositoryImpl implements UserIngredientRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<UserIngredient> searchFridgePage(
            Long userId,
            LocalDate today,
            LocalDate soonEnd,
            String freshnessStatus,
            String storageType,
            String keyword,
            String sort,
            Pageable pageable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserIngredient> cq = cb.createQuery(UserIngredient.class);
        Root<UserIngredient> root = cq.from(UserIngredient.class);

        Fetch<UserIngredient, IngredientMaster> masterFetch =
                root.fetch("ingredientMaster", JoinType.LEFT);
        masterFetch.fetch("category", JoinType.LEFT);
        root.fetch("category", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(cb, root, userId, freshnessStatus, storageType, keyword);
        cq.where(predicates.toArray(Predicate[]::new));
        cq.distinct(true);
        applyOrder(cb, cq, root, today, soonEnd, sort);

        TypedQuery<UserIngredient> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<UserIngredient> content = query.getResultList();

        long total = countMatching(cb, userId, freshnessStatus, storageType, keyword);
        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<UserIngredient> root,
            Long userId,
            String freshnessStatus,
            String storageType,
            String keyword) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("user").get("userId"), userId));
        if (StringUtils.hasText(freshnessStatus)) {
            predicates.add(cb.equal(root.get("freshnessStatus"), freshnessStatus));
        }
        if (StringUtils.hasText(storageType)) {
            predicates.add(cb.equal(root.get("storageType"), storageType));
        }
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("rawName")), pattern),
                    cb.like(cb.lower(root.get("normalizedNameSnapshot")), pattern)));
        }
        return predicates;
    }

    private void applyOrder(
            CriteriaBuilder cb,
            CriteriaQuery<UserIngredient> cq,
            Root<UserIngredient> root,
            LocalDate today,
            LocalDate soonEnd,
            String sortParam) {
        if (StringUtils.hasText(sortParam)) {
            String[] parts = sortParam.split(",");
            String field = parts[0].trim();
            boolean asc = parts.length < 2 || parts[1].trim().equalsIgnoreCase("asc");
            Order primary;
            switch (field) {
                case "expiresAt" ->
                        primary = asc ? cb.asc(root.get("expiresAt")) : cb.desc(root.get("expiresAt"));
                case "updatedAt" ->
                        primary = asc ? cb.asc(root.get("updatedAt")) : cb.desc(root.get("updatedAt"));
                case "createdAt" ->
                        primary = asc ? cb.asc(root.get("createdAt")) : cb.desc(root.get("createdAt"));
                default -> {
                    applyDefaultFridgeOrder(cb, cq, root, today, soonEnd);
                    return;
                }
            }
            cq.orderBy(primary, cb.desc(root.get("updatedAt")));
            return;
        }
        applyDefaultFridgeOrder(cb, cq, root, today, soonEnd);
    }

    private void applyDefaultFridgeOrder(
            CriteriaBuilder cb,
            CriteriaQuery<UserIngredient> cq,
            Root<UserIngredient> root,
            LocalDate today,
            LocalDate soonEnd) {
        Expression<Integer> tier = cb.<Integer>selectCase()
                .when(cb.isNull(root.get("expiresAt")), 3)
                .when(cb.lessThan(root.get("expiresAt"), cb.literal(today)), 0)
                .when(cb.lessThanOrEqualTo(root.get("expiresAt"), cb.literal(soonEnd)), 1)
                .otherwise(2);
        cq.orderBy(
                cb.asc(tier),
                cb.asc(root.get("expiresAt")),
                cb.desc(root.get("updatedAt")));
    }

    private long countMatching(
            CriteriaBuilder cb,
            Long userId,
            String freshnessStatus,
            String storageType,
            String keyword) {
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<UserIngredient> root = cq.from(UserIngredient.class);
        List<Predicate> predicates = buildPredicates(cb, root, userId, freshnessStatus, storageType, keyword);
        cq.select(cb.count(root));
        cq.where(predicates.toArray(Predicate[]::new));
        return em.createQuery(cq).getSingleResult();
    }
}
