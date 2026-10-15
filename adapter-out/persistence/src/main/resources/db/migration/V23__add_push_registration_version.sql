-- 토큰 갱신과 이전 발송 결과를 구분할 등록 버전을 추가한다.
alter table companion_push_endpoint
    add column registration_version bigint not null default 1;

alter table companion_push_delivery
    add column endpoint_registration_version bigint not null default 1;

create unique index uk_companion_push_endpoint_active_token
    on companion_push_endpoint (token, active);
