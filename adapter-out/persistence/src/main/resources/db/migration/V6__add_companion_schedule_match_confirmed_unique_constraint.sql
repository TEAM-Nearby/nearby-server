-- 동행 일정은 매칭별 confirmed 상태마다 하나만 저장되도록 보장한다.
alter table companion_schedule
    add constraint uk_companion_schedule_match_confirmed unique (match_id, confirmed);
