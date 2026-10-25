-- 반복 가능한 일정 수정 알림을 지원한다.
alter table companion_notification
    add column deduplication_key bigint;

update companion_notification
set deduplication_key = 0;

alter table companion_notification
    drop constraint uk_companion_notification_event_recipient;

alter table companion_notification
    add constraint uk_companion_notification_event_recipient
        unique (notification_type, target_type, target_id, recipient_user_id, deduplication_key);
