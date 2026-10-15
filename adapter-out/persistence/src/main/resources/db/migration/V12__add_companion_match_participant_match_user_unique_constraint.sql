-- companion_match_participant의 매칭별 사용자 중복 참여를 방지한다.
alter table companion_match_participant
    add constraint uk_companion_match_participant_match_user unique (match_id, user_id);
