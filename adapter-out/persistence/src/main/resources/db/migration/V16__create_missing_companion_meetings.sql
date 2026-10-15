-- 확정 일정에 대응하는 동행 만남을 보완하고 매칭별 중복 생성을 방지한다.
insert into companion_meeting (match_id, status, started_at, completed_at)
select schedule.match_id, 'ONGOING', schedule.scheduled_at, null
from companion_schedule schedule
join companion_match matched
  on matched.id = schedule.match_id
where schedule.confirmed = true
  and matched.status = 'SCHEDULE_CONFIRMED'
  and not exists (
      select 1
      from companion_meeting meeting
      where meeting.match_id = schedule.match_id
  );

alter table companion_meeting
    add constraint uk_companion_meeting_match unique (match_id);
