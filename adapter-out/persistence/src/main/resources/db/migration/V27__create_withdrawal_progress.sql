-- 공급자별 탈퇴 진행 결과를 기록해 중단된 탈퇴를 조정할 수 있게 한다.
create table withdrawal_progress (
    id varchar(100) primary key,
    user_id bigint not null references user_account (id),
    provider varchar(50) not null,
    state varchar(20) not null,
    constraint uk_withdrawal_progress_user_provider unique (user_id, provider)
);
