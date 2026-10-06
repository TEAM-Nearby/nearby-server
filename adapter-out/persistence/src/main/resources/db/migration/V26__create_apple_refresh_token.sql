-- Apple 계정 연동 해제에 사용할 Refresh Token 저장소를 생성한다.
create table apple_refresh_token (
    user_id bigint primary key,
    refresh_token text not null,
    updated_at timestamp not null,
    constraint fk_apple_refresh_token_user foreign key (user_id) references user_account (id)
);
