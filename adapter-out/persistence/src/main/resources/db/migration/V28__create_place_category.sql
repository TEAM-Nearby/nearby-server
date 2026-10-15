-- 혼밥 맛집의 복수 카테고리와 대표 카테고리 순서를 저장한다.
create table place_category (
    place_id bigint not null,
    category varchar(255) not null,
    category_order integer not null,
    primary key (place_id, category),
    constraint fk_place_category_place foreign key (place_id) references place_cache (id) on delete cascade,
    constraint uk_place_category_order unique (place_id, category_order),
    constraint ck_place_category_value check (category in ('RESTAURANT', 'CAFE', 'PUB', 'DESSERT', 'OTHER')),
    constraint ck_place_category_order check (category_order >= 0)
);

create index idx_place_category_category_place on place_category (category, place_id);

insert into place_category (place_id, category, category_order)
select id, upper(category), 0
from place_cache
where upper(category) in ('RESTAURANT', 'CAFE', 'PUB', 'DESSERT');
