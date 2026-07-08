-- companion_application의 모집글별 신청자 중복 신청을 방지한다.
create temporary table companion_application_dedup as
select
    target_app.id,
    keep_app.id as keep_id
from (
    select
        id,
        post_id,
        applicant_user_id,
        row_number() over (
            partition by post_id, applicant_user_id
            order by created_at desc, id desc
        ) as rn
    from companion_application
) target_app
join (
    select
        id,
        post_id,
        applicant_user_id,
        row_number() over (
            partition by post_id, applicant_user_id
            order by created_at desc, id desc
        ) as rn
    from companion_application
) keep_app
    on keep_app.post_id = target_app.post_id
    and keep_app.applicant_user_id = target_app.applicant_user_id
    and keep_app.rn = 1;

delete from companion_application_read_status
where id in (
    select id
    from (
        select
            read_status.id,
            row_number() over (
                partition by dedup.keep_id, read_status.user_id
                order by read_status.read_at desc, read_status.id desc
            ) as rn
        from companion_application_read_status read_status
        join companion_application_dedup dedup
            on dedup.id = read_status.application_id
    ) ranked_read_statuses
    where ranked_read_statuses.rn > 1
);

update companion_application_read_status
set application_id = (
    select keep_id
    from companion_application_dedup dedup
    where dedup.id = companion_application_read_status.application_id
)
where application_id in (
    select id
    from companion_application_dedup
    where id <> keep_id
);

delete from companion_notification
where id in (
    select id
    from (
        select
            notification.id,
            row_number() over (
                partition by notification.notification_type,
                    notification.target_type,
                    dedup.keep_id,
                    notification.recipient_user_id
                order by notification.created_at desc, notification.id desc
            ) as rn
        from companion_notification notification
        join companion_application_dedup dedup
            on dedup.id = notification.target_id
        where notification.target_type = 'COMPANION_APPLICATION'
    ) ranked_notifications
    where ranked_notifications.rn > 1
);

update companion_notification
set target_id = (
    select keep_id
    from companion_application_dedup dedup
    where dedup.id = companion_notification.target_id
)
where target_type = 'COMPANION_APPLICATION'
    and target_id in (
        select id
        from companion_application_dedup
        where id <> keep_id
    );

update companion_match_participant
set accepted_application_id = (
    select keep_id
    from companion_application_dedup dedup
    where dedup.id = companion_match_participant.accepted_application_id
)
where accepted_application_id in (
    select id
    from companion_application_dedup
    where id <> keep_id
);

delete from companion_application
where id in (
    select id
    from companion_application_dedup
    where id <> keep_id
);

drop table companion_application_dedup;

alter table companion_application
    add constraint uk_companion_application_post_applicant unique (post_id, applicant_user_id);
