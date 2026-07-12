-- 만남 참여자의 개인 동행 완료 시간을 저장한다.
alter table meeting_check_in
    add column completed_at timestamp;
