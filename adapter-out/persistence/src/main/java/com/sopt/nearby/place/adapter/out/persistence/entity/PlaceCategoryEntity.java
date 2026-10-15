// 혼밥 맛집의 검수된 카테고리와 표시 순서를 저장하는 엔티티다.
package com.sopt.nearby.place.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "place_category", uniqueConstraints =
        @UniqueConstraint(name = "uk_place_category_order", columnNames = {"place_id", "category_order"}))
@IdClass(PlaceCategoryEntityId.class)
public class PlaceCategoryEntity {

    @Id
    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Id
    @Column(nullable = false)
    private String category;

    @Column(name = "category_order", nullable = false)
    private int categoryOrder;

    protected PlaceCategoryEntity() {
    }

    public PlaceCategoryEntity(final Long placeId, final String category, final int categoryOrder) {
        this.placeId = placeId;
        this.category = category;
        this.categoryOrder = categoryOrder;
    }
}
