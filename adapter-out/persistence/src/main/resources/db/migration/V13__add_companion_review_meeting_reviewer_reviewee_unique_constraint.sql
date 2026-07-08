-- 동행 후기 반복 등록을 방지하는 유니크 제약을 추가한다.
alter table companion_review
    add constraint uk_companion_review_meeting_reviewer_reviewee
    unique (meeting_id, reviewer_user_id, reviewee_user_id);
