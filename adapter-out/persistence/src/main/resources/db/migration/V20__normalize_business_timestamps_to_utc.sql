-- KST 벽시각으로 보정했던 서버 생성 업무 시간을 UTC로 되돌린다.
update user_account
set created_at = created_at - interval '9' hour,
    phone_verified_at = phone_verified_at - interval '9' hour;

update phone_verification
set expires_at = expires_at - interval '9' hour,
    verified_at = verified_at - interval '9' hour;

update solo_dining_favorite
set created_at = created_at - interval '9' hour;

update companion_post
set created_at = created_at - interval '9' hour,
    exposure_expires_at = exposure_expires_at - interval '9' hour;

update companion_application
set created_at = created_at - interval '9' hour;

update companion_match
set created_at = created_at - interval '9' hour;

update companion_schedule
set scheduled_at = scheduled_at - interval '9' hour
where match_id in (
    select matched.id
    from companion_match matched
    join companion_post post on post.id = matched.post_id
    where post.meeting_time_type = 'NOW'
);

update companion_meeting
set started_at = started_at - interval '9' hour
where match_id in (
    select matched.id
    from companion_match matched
    join companion_post post on post.id = matched.post_id
    where post.meeting_time_type = 'NOW'
);

update companion_meeting
set completed_at = completed_at - interval '9' hour;

update meeting_check_in
set checked_in_at = checked_in_at - interval '9' hour,
    completed_at = completed_at - interval '9' hour;

update companion_notification
set created_at = created_at - interval '9' hour,
    read_at = read_at - interval '9' hour;

update companion_review
set created_at = created_at - interval '9' hour;
