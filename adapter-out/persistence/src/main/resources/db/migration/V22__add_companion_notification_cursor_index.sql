-- 커서 페이지 조회의 정렬 조건을 지원하는 복합 인덱스를 추가한다.
create index idx_companion_notification_recipient_created_id
    on companion_notification (recipient_user_id, created_at desc, id desc);
