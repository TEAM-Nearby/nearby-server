#!/usr/bin/env python3
# Supabase 덤프를 분리·압축·암호화하고 완성된 백업만 S3 최신 포인터로 게시한다.
from datetime import datetime, timezone
import gzip
import hashlib
import json
import os
from pathlib import Path
import subprocess
import shutil
import sys
import tempfile
from zoneinfo import ZoneInfo


def run(args):
    result = subprocess.run(args, capture_output=True, text=True, timeout=1800)
    if result.returncode:
        raise RuntimeError("Backup command failed: " + Path(args[0]).name)
    return result.stdout.strip()


def require_nonempty(path):
    if not path.is_file() or path.stat().st_size == 0:
        raise ValueError("Missing or empty backup: " + path.name)


def upload(path, bucket, key):
    run(["aws", "s3", "cp", str(path), "s3://" + bucket + "/" + key, "--only-show-errors"])
    remote = json.loads(run(["aws", "s3api", "head-object", "--bucket", bucket,
                             "--key", key, "--output", "json"]))
    if remote["ContentLength"] != path.stat().st_size:
        raise RuntimeError("Backup upload size mismatch: " + key)


def main(now=None):
    settings = {name: os.environ.get(name, "") for name in
                ("BACKUP_S3_BUCKET", "SUPABASE_DB_URL", "BACKUP_ENCRYPTION_PASSPHRASE")}
    for key, value in settings.items():
        if not value:
            raise ValueError("Missing backup setting: " + key)
    now = now or datetime.now(timezone.utc)
    korea = now.astimezone(ZoneInfo("Asia/Seoul"))
    bucket = settings["BACKUP_S3_BUCKET"]
    run_id = os.environ.get("GITHUB_RUN_ID", "local") + "-" + os.environ.get("GITHUB_RUN_ATTEMPT", "1")
    timestamp = now.strftime("%Y%m%dT%H%M%SZ") + "-" + run_id
    prefix = "nearby-db/daily/" + korea.strftime("%Y/%m/%d/") + timestamp
    os.umask(0o077)
    with tempfile.TemporaryDirectory(prefix="nearby-backup.", dir=os.environ.get("RUNNER_TEMP")) as directory:
        work = Path(directory)
        passphrase = work / "passphrase"
        passphrase.write_text(settings["BACKUP_ENCRYPTION_PASSPHRASE"])
        manifest = {"timestamp": now.isoformat(), "prefix": prefix, "files": []}
        # 역할과 스키마는 분리하고 COPY 형식 데이터 덤프를 생성한다.
        for name, options in (("roles", ["--role-only"]), ("schema", []),
                              ("data", ["--data-only", "--use-copy"])):
            plain = work / (name + ".sql")
            run(["supabase", "db", "dump", "--db-url", settings["SUPABASE_DB_URL"],
                 "-f", str(plain), *options])
            require_nonempty(plain)
            compressed = work / (name + ".sql.gz")
            with plain.open("rb") as source, gzip.open(compressed, "wb") as target:
                shutil.copyfileobj(source, target)
            encrypted = work / (name + ".sql.gz.gpg")
            run(["gpg", "--batch", "--yes", "--no-tty", "--pinentry-mode", "loopback",
                 "--passphrase-file", str(passphrase), "--symmetric", "--cipher-algo", "AES256",
                 "--output", str(encrypted), str(compressed)])
            require_nonempty(encrypted)
            plain.unlink()
            compressed.unlink()
            key = prefix + "/" + encrypted.name
            upload(encrypted, bucket, key)
            with encrypted.open("rb") as stream:
                checksum = hashlib.file_digest(stream, "sha256").hexdigest()
            manifest["files"].append({"key": key, "bytes": encrypted.stat().st_size,
                                      "sha256": checksum})
        manifest_path = work / "manifest.json"
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        upload(manifest_path, bucket, prefix + "/manifest.json")
        # monthly 사본을 모두 검증한 뒤 manifest를 게시한다.
        if korea.day == 1:
            monthly = "nearby-db/monthly/" + korea.strftime("%Y/%m/") + timestamp
            monthly_manifest = dict(manifest, prefix=monthly, files=[])
            for item in manifest["files"]:
                name = item["key"].rsplit("/", 1)[-1]
                upload(work / name, bucket, monthly + "/" + name)
                monthly_manifest["files"].append(dict(item, key=monthly + "/" + name))
            monthly_path = work / "monthly-manifest.json"
            monthly_path.write_text(json.dumps(monthly_manifest, indent=2) + "\n")
            upload(monthly_path, bucket, monthly + "/manifest.json")
        # 여러 파일을 latest 아래 순차 덮어쓰면 부분 실패 때 서로 다른 백업이 섞인다.
        upload(manifest_path, bucket, "nearby-db/latest/manifest.json")
        if os.environ.get("GITHUB_STEP_SUMMARY"):
            with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as stream:
                stream.write("\nBackup verified: s3://" + bucket + "/" + prefix + "/manifest.json\n")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        sys.exit(str(error) if isinstance(error, (ValueError, RuntimeError)) else type(error).__name__)
