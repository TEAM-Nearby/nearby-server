-- Google 장소 사진 식별자를 길이 제한 없이 저장하도록 확장한다.
alter table place_cache
    alter column photo_reference type text;
