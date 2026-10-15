-- 휴대폰 인증 코드 해시를 저장할 컬럼을 추가한다.
alter table phone_verification
    add column verification_code_hash varchar(255);
