#!/usr/bin/env python3
# 검증된 산출물을 S3와 SSM으로 배포하고 성공 및 실패 상태를 기록한다.
import base64
import json
import os
from pathlib import Path
import shlex
import sys
import time
import urllib.request

from pipeline import ROOT, command, configuration, digest, guard, required


def aws(*args):
    return json.loads(command(["aws", *args, "--output", "json"]))


def remote_command(config):
    if os.environ.get("DEPLOY_ENVIRONMENT") == "prod":
        config = dict(config, profile_image_check=base64.b64encode(
            (ROOT / ".github/scripts/check-production-profile-images.sh").read_bytes()).decode())
    script = base64.b64encode((ROOT / "scripts/deploy/ec2.py").read_bytes()).decode()
    encoded = base64.b64encode(json.dumps(config).encode()).decode()
    program = "import base64; exec(compile(base64.b64decode(" + repr(script) + "), 'nearby-deploy', 'exec'))"
    return shlex.join(["python3", "-c", program, encoded])


def save_metadata(metadata):
    destination = ROOT / "build/deployment/metadata.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(metadata, indent=2) + "\n")


def wait_command(command_id, instance_id, metadata, timeout=720):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            invocation = aws("ssm", "get-command-invocation", "--command-id", command_id,
                             "--instance-id", instance_id)
        except RuntimeError:
            time.sleep(5)
            continue
        for line in invocation.get("StandardOutputContent", "").splitlines():
            if line.startswith("NEARBY_DEPLOYMENT_RESULT="):
                metadata["remote"] = json.loads(line.split("=", 1)[1])
        status = invocation["Status"]
        metadata["ssm_status"] = status
        if status == "Success":
            remote = metadata.get("remote", {})
            if invocation.get("ResponseCode") != 0 or remote.get("status") != "success":
                raise RuntimeError("SSM returned success without verified application evidence")
            if remote.get("commit_sha") != metadata["commit_sha"] or remote.get("sha256") != metadata["sha256"]:
                raise RuntimeError("Remote deployment identity mismatch")
            return
        if status in ("Failed", "Cancelled", "Cancelling", "TimedOut"):
            raise RuntimeError("SSM deployment ended with " + status)
        time.sleep(5)
    metadata["ssm_status"] = "unknown_timeout"
    raise RuntimeError("SSM observation timed out; inspect the command before retrying")


def external_health(url, timeout=90):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=10) as response:
                if response.status == 200 and json.load(response).get("status") == "UP":
                    return
        except (OSError, ValueError):
            pass
        time.sleep(5)
    raise RuntimeError("External health check timed out")


def deploy(metadata):
    environment, sha = guard()
    config = configuration()
    package = ROOT / "build/deployment/package"
    manifest = json.loads((package / "manifest.json").read_text())
    checksum = digest(package / "app.jar")
    if manifest != {"commit_sha": sha, "sha256": checksum}:
        raise ValueError("Verified artifact does not match deployment commit/checksum")
    deployment_id = required("GITHUB_RUN_ID") + "-" + required("GITHUB_RUN_ATTEMPT")
    if not all(part.isdigit() for part in deployment_id.split("-")):
        raise ValueError("Invalid deployment run identifier")
    # 커밋 아래 실행별 객체를 두어 같은 SHA 재빌드도 이전 JAR를 덮어쓰지 않는다.
    key = "nearby/" + sha + "/" + deployment_id + "/app.jar"
    s3_uri = "s3://" + config["DEPLOY_S3_BUCKET"] + "/" + key
    metadata.update(environment=environment, commit_sha=sha, sha256=checksum,
                    s3_uri=s3_uri, instance_id=config["EC2_INSTANCE_ID"],
                    release_version=os.environ.get("RELEASE_VERSION", ""), phase="upload")
    save_metadata(metadata)
    aws("s3api", "put-object", "--bucket", config["DEPLOY_S3_BUCKET"], "--key", key,
        "--body", str(package / "app.jar"), "--if-none-match", "*",
        "--metadata", json.dumps({"commit-sha": sha, "sha256": checksum}))
    head = aws("s3api", "head-object", "--bucket", config["DEPLOY_S3_BUCKET"], "--key", key)
    if head["ContentLength"] != (package / "app.jar").stat().st_size or head["Metadata"]["sha256"] != checksum:
        raise RuntimeError("Uploaded artifact verification failed")
    metadata["s3_version_id"] = head.get("VersionId")
    config.update(commit_sha=sha, sha256=checksum, s3_uri=s3_uri, deployment_id=deployment_id)
    parameters = {"commands": [remote_command(config)], "executionTimeout": ["600"]}
    metadata["phase"] = "remote_state_unknown"
    save_metadata(metadata)
    response = aws("ssm", "send-command", "--instance-ids", config["EC2_INSTANCE_ID"],
                   "--document-name", "AWS-RunShellScript", "--timeout-seconds", "60",
                   "--comment", "Deploy nearby " + sha, "--parameters", json.dumps(parameters))
    metadata.update(command_id=response["Command"]["CommandId"], phase="remote_state_unknown")
    save_metadata(metadata)
    wait_command(metadata["command_id"], config["EC2_INSTANCE_ID"], metadata)
    metadata["phase"] = "external_health"
    save_metadata(metadata)
    external_health(config["SERVICE_URL"].rstrip("/") + config["HEALTHCHECK_PATH"])
    metadata.update(phase="verified", status="success")


def main():
    metadata = {"status": "failure", "phase": "before_deploy",
                "commit_sha": os.environ.get("DEPLOY_SHA"), "environment": os.environ.get("DEPLOY_ENVIRONMENT")}
    try:
        deploy(metadata)
    except Exception as error:
        metadata["error"] = str(error) if isinstance(error, (ValueError, RuntimeError)) else type(error).__name__
    finally:
        save_metadata(metadata)
        summary = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary:
            with open(summary, "a") as stream:
                stream.write("\n### EC2 deployment\n\n")
                stream.write(json.dumps(metadata, indent=2) + "\n")
        print(json.dumps(metadata, indent=2))
    return 0 if metadata["status"] == "success" else 1


if __name__ == "__main__":
    sys.exit(main())
