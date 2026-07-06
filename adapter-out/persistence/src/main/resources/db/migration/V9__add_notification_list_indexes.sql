-- 알림 목록 조회 성능 개선을 위한 인덱스를 추가한다.
create index idx_companion_notification_recipient_created_at
    on companion_notification (recipient_user_id, created_at desc);
