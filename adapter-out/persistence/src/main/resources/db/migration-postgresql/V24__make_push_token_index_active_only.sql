-- 비활성 이력은 토큰을 재사용할 수 있도록 활성 토큰만 유일하게 보장한다.
drop index if exists uk_companion_push_endpoint_active_token;

create unique index uk_companion_push_endpoint_active_token
    on companion_push_endpoint (token)
    where active = true;
