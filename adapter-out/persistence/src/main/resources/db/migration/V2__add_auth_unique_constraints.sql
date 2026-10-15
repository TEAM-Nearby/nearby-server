-- 인증 계정과 리프레시 토큰 중복 저장을 방지하는 제약 조건을 추가한다.
alter table social_account
    add constraint uk_social_account_provider_user unique (provider, provider_user_id);

alter table refresh_token
    add constraint uk_refresh_token_hash unique (token_hash);
