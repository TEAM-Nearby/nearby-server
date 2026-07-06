-- 동행 모집 글 작성 API에 필요한 시간 유형과 노출 만료 필드를 추가한다.
alter table companion_post
    add column meeting_time_type varchar(255) not null default 'SCHEDULED';

alter table companion_post
    alter column meeting_at drop not null;

alter table companion_post
    add column exposure_expires_at timestamp;

alter table companion_post
    add column depart_even_if_not_full boolean not null default true;
