-- 동행 프로필의 사용자와 닉네임 중복을 방지하는 제약을 추가한다
alter table companion_profile
    add constraint uk_companion_profile_user unique (user_id);

alter table companion_profile
    add constraint uk_companion_profile_nickname unique (nickname);

