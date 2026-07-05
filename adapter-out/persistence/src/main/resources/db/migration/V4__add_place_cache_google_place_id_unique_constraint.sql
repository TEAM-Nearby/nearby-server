-- place_cache의 google_place_id 중복 저장을 방지한다.
alter table place_cache
    add constraint uk_place_cache_google_place_id unique (google_place_id);
