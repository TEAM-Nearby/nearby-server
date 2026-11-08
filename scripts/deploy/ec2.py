#!/usr/bin/env python3
# SSM에서 JAR와 환경 파일을 안전하게 교체하고 systemd 및 내부 상태를 검증한다.
import base64
import fcntl
import hashlib
import json
import os
from pathlib import Path
import pwd
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request


def run(args, input_text=None):
    result = subprocess.run(args, input=input_text, text=True, capture_output=True, timeout=120)
    if result.returncode:
        raise RuntimeError("EC2 command failed: " + Path(args[0]).name)
    return result.stdout.strip()


def sha256(path):
    value = hashlib.sha256()
    with open(path, "rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def environment_file(parameters):
    values = {}
    for parameter in parameters:
        name = parameter["Name"].rsplit("/", 1)[-1]
        value = parameter["Value"]
        if not re.fullmatch(r"[A-Z_][A-Z0-9_]*", name) or name in values:
            raise ValueError("Invalid or duplicate environment parameter name")
        if any(character in value for character in ("\0", "\n", "\r")):
            raise ValueError("Multiline runtime parameters are not supported")
        values[name] = value
    for name in ("SPRING_PROFILES_ACTIVE", "SPRING_DATASOURCE_URL",
                 "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD"):
        if not values.get(name):
            raise ValueError("Missing runtime setting: " + name)
    if "supabase" not in values["SPRING_PROFILES_ACTIVE"].split(","):
        raise ValueError("Runtime must activate the supabase profile")
    # Parameter Store의 기존 값과 관계없이 배포된 앱은 migration을 실행하지 않는다.
    values["SPRING_FLYWAY_ENABLED"] = "false"
    values["SPRING_JPA_HIBERNATE_DDL_AUTO"] = "validate"
    return "".join(name + '="' + value.replace("\\", "\\\\").replace('"', '\\"') + '"\n'
                   for name, value in sorted(values.items()))


def healthy(url):
    try:
        with urllib.request.urlopen(url, timeout=5) as response:
            return response.status == 200 and json.load(response).get("status") == "UP"
    except (OSError, ValueError):
        return False


def atomic_copy(source, destination, mode):
    destination = Path(destination)
    with tempfile.NamedTemporaryFile(dir=destination.parent, delete=False) as stream:
        temporary = Path(stream.name)
    try:
        shutil.copyfile(source, temporary)
        temporary.chmod(mode)
        os.replace(temporary, destination)
    finally:
        temporary.unlink(missing_ok=True)


def deploy(config, result):
    if os.geteuid() != 0:
        raise ValueError("SSM deployment must run as root")
    app_dir = Path(config["APP_DIR"])
    jar = Path(config["APP_JAR_PATH"])
    if not app_dir.is_dir() or not jar.parent.is_dir() or not jar.is_relative_to(app_dir):
        raise ValueError("Application directories must already exist")
    # 심볼릭 링크로 app 디렉터리 밖 파일을 변경하지 않는다.
    if (jar.is_symlink() or app_dir.resolve() != app_dir
            or not jar.parent.resolve().is_relative_to(app_dir)):
        raise ValueError("Application paths must not be symbolic links")
    if run(["findmnt", "-n", "-o", "FSTYPE", "--target", str(app_dir)]) in ("tmpfs", "ramfs"):
        raise ValueError("Deployment requires disk-backed staging")
    user = pwd.getpwnam("nearby")
    service = config["APP_SERVICE_NAME"]
    if not service.endswith(".service"):
        service += ".service"
    exec_start = run(["systemctl", "show", service, "--property=ExecStart", "--value"])
    if str(jar) not in exec_start or any(value in exec_start for value in
                                        ("spring.flyway.enabled", "spring.jpa.hibernate.ddl-auto")):
        raise ValueError("Service must use APP_JAR_PATH and environment-controlled DB settings")
    with open(app_dir / ".deploy.lock", "a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        if config.get("profile_image_check"):
            run(["bash", "-s", "--", config["SSM_ENV_PATH"], config["AWS_REGION"]],
                base64.b64decode(config["profile_image_check"]).decode())
        with tempfile.TemporaryDirectory(prefix=".deploy.", dir=app_dir) as directory:
            stage = Path(directory)
            run(["aws", "s3", "cp", config["s3_uri"], str(stage / "app.jar"), "--no-progress"])
            if sha256(stage / "app.jar") != config["sha256"]:
                raise ValueError("Downloaded JAR checksum mismatch")
            parameters = json.loads(run(["aws", "ssm", "get-parameters-by-path",
                                         "--path", config["SSM_ENV_PATH"], "--recursive",
                                         "--with-decryption", "--output", "json"]))["Parameters"]
            (stage / "app.env").write_text(environment_file(parameters))
            (stage / "app.env").chmod(0o600)
            state = app_dir / ".deployment-state" / config["deployment_id"]
            state.mkdir(parents=True, exist_ok=False)
            state.parent.chmod(0o700)
            state.chmod(0o700)
            current = app_dir / "deployment.json"
            if current.exists():
                result["previous_commit"] = json.loads(current.read_text()).get("commit_sha")
                shutil.copyfile(current, state / "previous-deployment.json")
            if jar.exists():
                result["previous_sha256"] = sha256(jar)
                shutil.copyfile(jar, state / "previous.jar")
            env_path = app_dir / "app.env"
            if env_path.exists():
                shutil.copyfile(env_path, state / "previous.env")
            result["recovery_directory"] = str(state)
            # 각 실행별 기록은 재시도에서도 덮어쓰지 않는다.
            archived = app_dir / "releases" / config["commit_sha"] / config["sha256"]
            archived.mkdir(parents=True, exist_ok=True)
            if (archived / "app.jar").exists() and sha256(archived / "app.jar") != config["sha256"]:
                raise ValueError("Existing release artifact was modified")
            atomic_copy(stage / "app.jar", archived / "app.jar", 0o644)
            old_pid = run(["systemctl", "show", service, "--property=MainPID", "--value"])
            dropin = Path("/etc/systemd/system") / (service + ".d") / "90-nearby-deployment.conf"
            dropin.parent.mkdir(parents=True, exist_ok=True)
            if dropin.exists():
                shutil.copyfile(dropin, state / "previous-dropin.conf")
            (stage / "dropin.conf").write_text("[Service]\nEnvironmentFile=" + str(env_path) + "\n")
            result["phase"] = "switch_started"
            (state / "result.json").write_text(json.dumps(result))
            # 먼저 정지하므로 이전 JVM이 새 JAR의 일부 클래스를 읽는 상황을 피한다.
            run(["systemctl", "stop", service])
            atomic_copy(stage / "app.env", env_path, 0o600)
            atomic_copy(stage / "app.jar", jar, 0o644)
            os.chown(jar, user.pw_uid, user.pw_gid)
            atomic_copy(stage / "dropin.conf", dropin, 0o644)
            run(["systemctl", "daemon-reload"])
            run(["systemctl", "start", service])
            result["phase"] = "service_started"
            deadline = time.monotonic() + 120
            while time.monotonic() < deadline:
                if healthy(config["INTERNAL_HEALTHCHECK_URL"]):
                    break
                time.sleep(2)
            else:
                raise RuntimeError("Internal health check timed out")
            run(["systemctl", "is-active", "--quiet", service])
            pid = run(["systemctl", "show", service, "--property=MainPID", "--value"])
            if pid == "0" or pid == old_pid:
                raise RuntimeError("Expected a new running application process")
            commandline = Path("/proc") / pid / "cmdline"
            if str(jar).encode() not in commandline.read_bytes().split(b"\0"):
                raise RuntimeError("Running process does not use expected JAR")
            if sha256(jar) != config["sha256"]:
                raise RuntimeError("Installed artifact changed")
            result.update(phase="internal_verified", status="success", pid=pid, commit_sha=config["commit_sha"],
                          sha256=config["sha256"])
            (stage / "deployment.json").write_text(json.dumps(result, indent=2) + "\n")
            atomic_copy(stage / "deployment.json", current, 0o644)
            (state / "result.json").write_text(json.dumps(result, indent=2) + "\n")


def main():
    os.umask(0o077)
    config = json.loads(base64.b64decode(sys.argv[1]))
    os.environ["AWS_DEFAULT_REGION"] = config["AWS_REGION"]
    result = {"phase": "before_switch", "status": "failure"}
    try:
        deploy(config, result)
        result["status"] = "success"
    except Exception as error:
        result["error"] = str(error) if isinstance(error, (ValueError, RuntimeError)) else type(error).__name__
    finally:
        print("NEARBY_DEPLOYMENT_RESULT=" + json.dumps(result), flush=True)
    return 0 if result["status"] == "success" else 1


if __name__ == "__main__":
    sys.exit(main())
