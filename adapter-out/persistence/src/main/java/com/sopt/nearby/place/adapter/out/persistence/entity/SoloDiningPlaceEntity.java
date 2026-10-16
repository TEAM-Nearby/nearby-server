// 혼밥 지도에 명시적으로 등록된 장소를 매핑한다.
package com.sopt.nearby.place.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "solo_dining_place")
public class SoloDiningPlaceEntity {

    @Id
    @Column(name = "place_id", nullable = false)
    private Long placeId;

    protected SoloDiningPlaceEntity() {
    }

    public SoloDiningPlaceEntity(final Long placeId) {
        this.placeId = placeId;
    }
}
