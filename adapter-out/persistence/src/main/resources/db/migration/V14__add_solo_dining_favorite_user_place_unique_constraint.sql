-- solo_dining_favorite의 사용자별 장소 중복 즐겨찾기를 방지한다.
delete from solo_dining_favorite
where id not in (
    select min(id)
    from solo_dining_favorite
    group by user_id, place_id
);

alter table solo_dining_favorite
    add constraint uk_solo_dining_favorite_user_place unique (user_id, place_id);
