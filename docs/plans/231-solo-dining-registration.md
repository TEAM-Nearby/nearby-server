# 동행 장소와 혼밥 지도 등록 분리

동행 글에서 새 장소를 등록하면 공용 `place_cache`에 저장한다. 혼밥 지도 목록은 `solo_dining_place`에 등록된 장소만 조회한다. 동행 글에서 장소를 새로 저장하거나 갱신해도 지도 등록 여부는 변하지 않는다.

## 변경 범위

- `V29__create_solo_dining_place.sql`은 `place_id` 기본키와 `place_cache.id` 외래키를 가진 등록 테이블을 생성한다.
- 사용자가 전체 원본으로 확인한 파리 101곳·런던 98곳의 Google Place ID 중 DB에 이미 존재하는 장소만 이관한다. 이름·카테고리·평점·동행 사용 여부로 추정하지 않는다.
- 이관은 공용 장소 ID, 장소 정보, 동행 글과 즐겨찾기 참조를 변경하지 않는다. 원본 목록 밖의 장소는 기존 등록 시점과 관계없이 지도 목록에서 제외된다.
- 지도 목록의 반경 1km, 복수 카테고리 필터, 정렬, 즐겨찾기 표시는 유지한다. 상세 조회와 즐겨찾기 접근 정책은 이번 변경 대상이 아니다.
- 파리·런던 등록 SQL은 장소 저장 후 혼밥 등록 목록도 갱신한다. 같은 SQL을 반복 실행해도 기존 ID를 유지하고 등록 행은 중복되지 않는다.

## 배포 및 데이터 적용

1. 배포 대상 DB에 기존 파리·런던 장소가 들어 있는지 원본 SQL의 Google Place ID와 대조한다. 두 목록 전체가 저장되어 있으면 이관 대상은 199곳이다.
2. 서버 시작 시 Flyway가 V29의 테이블 생성과 기존 데이터 이관을 한 트랜잭션으로 완료한 뒤 새 조회 조건을 사용한다. 이전 마이그레이션 파일을 수정하거나 V29를 수동으로 재실행하지 않는다.
3. 신규 DB 또는 원본 장소가 누락된 DB는 V29 적용 후 [파리 등록 SQL](../sql/seed_paris_solo_dining.sql)과 [런던 등록 SQL](../sql/seed_london_solo_dining.sql)을 실행한다. V29는 누락 장소를 새로 만들지 않는다.
4. 기존 혼밥 장소가 계속 보이는지 확인하고, 원본에 없는 장소로 동행 글을 만든 뒤 같은 위치에서 혼밥 지도에 나오지 않는지 확인한다.

등록 수와 제외된 공용 장소는 다음 읽기 쿼리로 확인한다. 제외된 장소를 삭제하지 않는다.

```sql
select count(*) as registered_places from solo_dining_place;

select place.id, place.google_place_id, place.name
from place_cache place
left join solo_dining_place registered on registered.place_id = place.id
where registered.place_id is null
order by place.id;
```

향후 혼밥 장소를 추가할 때는 공용 장소 저장과 함께 다음 등록을 명시적으로 수행한다. `:google_place_id`는 운영 도구에서 바인딩할 값이다.

```sql
insert into solo_dining_place (place_id)
select id from place_cache where google_place_id = :google_place_id
on conflict (place_id) do nothing;
```

## 검증

- `SoloDiningPlaceMigrationTest`는 원본 장소만 이관되는지, 기존 동행·즐겨찾기 참조가 유지되는지, 신규 장소가 자동 등록되지 않는지와 기본키·외래키 제약을 검증한다.
- `SoloDiningPlaceQueryAdapterTest`는 등록된 장소의 거리·카테고리 필터, 정렬과 즐겨찾기 표시를 검증한다.
- `SoloDiningPlaceRegistrationFlowTest`는 실제 동행 작성·상세·지도 HTTP 요청과 DB 저장을 연결한다. 동행 전용 장소의 작성자·다른 사용자 대상 노출 차단, 명시적 등록 후 동일 ID 노출, 기존 혼밥 장소의 복수 카테고리·즐겨찾기 유지를 검증한다. 외부 이미지 조회는 모의 처리한다.

```bash
./gradlew test :bootstrap:bootJar
```

2026-10-17 로컬 검증 결과는 다음과 같다.

- 전체 783개 테스트가 통과했고 건너뛴 테스트는 없다. 실행 JAR 빌드도 성공했다.
- 별도 임시 PostgreSQL 14.18에 기존 파리·런던 데이터와 동행 전용 장소를 넣고 V29를 적용해 혼밥 등록 199곳을 확인했다.
- 변경된 파리·런던 등록 SQL을 각각 두 번 실행한 뒤에도 등록 199곳·분류 226개가 유지됐으며 장소 ID, 동행 글·즐겨찾기 참조가 보존됐다. 기존·신규 동행 전용 장소 모두 자동 등록되지 않았다.
- 원본 두 목록과 V29의 Google Place ID 199개가 일치하고 중복이 없음을 확인했다.

로컬 검증은 운영 데이터 이관이나 실제 배포 상태를 보장하지 않는다. 배포 후 API와 지도 화면 확인은 별도로 수행한다.

## 변경 파일

- `adapter-out/persistence/src/main/java/com/sopt/nearby/place/adapter/out/persistence/entity/SoloDiningPlaceEntity.java`.
- `adapter-out/persistence/src/main/java/com/sopt/nearby/place/adapter/out/persistence/repository/SoloDiningPlaceQueryJpaRepository.java`.
- `adapter-out/persistence/src/main/resources/db/migration/V29__create_solo_dining_place.sql`.
- `adapter-in/web/src/main/java/com/sopt/nearby/place/adapter/in/web/controller/SoloDiningPlaceApi.java`.
- `adapter-out/persistence/src/test/java/com/sopt/nearby/place/adapter/out/persistence/SoloDiningPlaceMigrationTest.java`.
- `adapter-out/persistence/src/test/java/com/sopt/nearby/place/adapter/out/persistence/SoloDiningPlaceQueryAdapterTest.java`.
- `bootstrap/src/test/java/com/sopt/nearby/place/SoloDiningPlaceRegistrationFlowTest.java`.
- `docs/sql/seed_paris_solo_dining.sql`.
- `docs/sql/seed_london_solo_dining.sql`.
- `docs/plans/231-solo-dining-registration.md`.
