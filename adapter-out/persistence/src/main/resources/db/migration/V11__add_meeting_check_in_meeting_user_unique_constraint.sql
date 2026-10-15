-- meeting_check_in의 만남별 사용자 중복 인증을 방지한다.
alter table meeting_check_in
    add constraint uk_meeting_check_in_meeting_user unique (meeting_id, user_id);
