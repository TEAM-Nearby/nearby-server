alter table emergency_contact
    add constraint uk_emergency_contact_user unique (user_id);
