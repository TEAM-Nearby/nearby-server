-- 일정 미확정 신고를 저장하고 매칭 단위로 중복 신고를 방지한다.
alter table companion_report add column match_id bigint;

update companion_report
set match_id = (
    select meeting.match_id
    from companion_meeting meeting
    where meeting.id = companion_report.meeting_id
);

alter table companion_report alter column match_id set not null;
alter table companion_report
    add constraint fk_companion_report_match foreign key (match_id) references companion_match (id);
alter table companion_report alter column meeting_id drop not null;
alter table companion_report drop constraint uk_companion_report_meeting_reporter_reported;
alter table companion_report
    add constraint uk_companion_report_match_reporter_reported
    unique (match_id, reporter_user_id, reported_user_id);
