// 장소와 카테고리 조합을 복합 식별자로 표현한다.
package com.sopt.nearby.place.adapter.out.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

public class PlaceCategoryEntityId implements Serializable {

    private Long placeId;
    private String category;

    public PlaceCategoryEntityId() {
    }

    public PlaceCategoryEntityId(final Long placeId, final String category) {
        this.placeId = placeId;
        this.category = category;
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof PlaceCategoryEntityId that
                && Objects.equals(placeId, that.placeId) && Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeId, category);
    }
}
