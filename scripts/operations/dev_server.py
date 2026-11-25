#!/usr/bin/env python3
# GitHub 수동 실행에서 고정된 개발 EC2만 제어하고 앱 준비 상태와 작업 결과를 기록합니다.
import html
import json
import os
from pathlib import Path
import subprocess
import sys
import time
from datetime import datetime, timezone
import urllib.request


ACCOUNT_ID = "395931907754"
REGION = "ap-northeast-2"
INSTANCE_ID = "i-06da8518d0a416c98"
ROLE_NAME = "nearby-github-actions-dev-server-control"
DOCUMENT = "nearby-dev-server-runtime"
HEALTH_URL = "https://nearby-server.p-e.kr/readyz"
ROOT = Path(__file__).resolve().parents[2]
RECORD = ROOT / "build/dev-server-control/result.json"


class AwsError(RuntimeError):
    pass


def aws(*args):
    process = subprocess.run(
        ["aws", *args, "--region", REGION, "--output", "json", "--no-cli-pager",
         "--cli-connect-timeout", "5", "--cli-read-timeout", "10"],
        text=True, capture_output=True, timeout=45)
    if process.returncode:
        if "InvocationDoesNotExist" in process.stderr:
            raise AwsError("InvocationDoesNotExist")
        raise AwsError("AWS request failed: " + " ".join(args[:2]))
    return json.loads(process.stdout or "{}")


def guard():
    expected = {"GITHUB_REPOSITORY": "TEAM-Nearby/nearby-server",
                "GITHUB_REF": "refs/heads/develop", "GITHUB_EVENT_NAME": "workflow_dispatch"}
    if any(os.environ.get(name) != value for name, value in expected.items()):
        raise ValueError("Run this workflow manually from TEAM-Nearby/nearby-server develop")
    sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    if sha != os.environ.get("GITHUB_SHA"):
        raise ValueError("Checkout must match the workflow commit")
    action = os.environ.get("CONTROL_ACTION", "")
    if action not in ("status", "prepare", "start", "stop"):
        raise ValueError("Unsupported action")
    reason = os.environ.get("CONTROL_REASON", "").strip()
    if len(reason) > 100 or any(ord(character) < 32 or ord(character) == 127 for character in reason):
        raise ValueError("Reason must be a single line of at most 100 characters")
    if action == "stop" and (len(reason) < 2 or os.environ.get("STOP_CONFIRMATION") != "stop-dev"):
        raise ValueError("Stop requires a reason of 2-100 characters and confirmation stop-dev")
    return action, reason


def validate_identity():
    actual = aws("sts", "get-caller-identity")
    prefix = f"arn:aws:sts::{ACCOUNT_ID}:assumed-role/{ROLE_NAME}/"
    if actual.get("Account") != ACCOUNT_ID or not actual.get("Arn", "").startswith(prefix):
        raise ValueError("Unexpected AWS account or control role")


def instance():
    reservations = aws("ec2", "describe-instances", "--instance-ids", INSTANCE_ID).get("Reservations", [])
    entries = [(reservation.get("OwnerId"), item)
               for reservation in reservations for item in reservation.get("Instances", [])]
    if len(entries) != 1:
        raise ValueError("Expected exactly one development instance")
    owner, item = entries[0]
    tags = {tag["Key"]: tag["Value"] for tag in item.get("Tags", [])}
    if (owner != ACCOUNT_ID or item.get("InstanceId") != INSTANCE_ID
            or tags.get("Name") != "nearby-app" or tags.get("Environment") in ("production", "prod")):
        raise ValueError("Target is not the expected development instance")
    return item


def wait_for(check, seconds, description):
    deadline = time.monotonic() + seconds
    while True:
        result = check()
        if result:
            return result
        if time.monotonic() >= deadline:
            raise RuntimeError(description + " timed out; inspect actual state before retrying")
        time.sleep(5)


def wait_state(expected):
    def check():
        state = instance()["State"]["Name"]
        if state in ("terminated", "shutting-down"):
            raise RuntimeError("Development instance is being deleted; no recovery action was taken")
        return state == expected
    wait_for(check, 300, "EC2 " + expected)


def ec2_ready():
    items = aws("ec2", "describe-instance-status", "--instance-ids", INSTANCE_ID).get("InstanceStatuses", [])
    return bool(items) and all(items[0].get(key, {}).get("Status") == "ok"
                               for key in ("SystemStatus", "InstanceStatus"))


def ssm_online():
    items = aws("ssm", "describe-instance-information", "--filters",
                json.dumps([{"Key": "InstanceIds", "Values": [INSTANCE_ID]}])).get("InstanceInformationList", [])
    return len(items) == 1 and items[0].get("PingStatus") == "Online"


def healthy():
    try:
        with urllib.request.urlopen(HEALTH_URL, timeout=5) as response:
            body = json.load(response)
            return response.status == 200 and isinstance(body, dict) and body.get("status") == "UP"
    except (OSError, ValueError):
        return False


def runtime(mode, record):
    document = aws("ssm", "describe-document", "--name", DOCUMENT,
                   "--document-version", "$DEFAULT")["Document"]
    if document.get("Name") != DOCUMENT or document.get("HashType") != "Sha256":
        raise ValueError("Unexpected runtime document")
    record["document_version"] = document["DocumentVersion"]
    command_id = aws(
        "ssm", "send-command", "--instance-ids", INSTANCE_ID, "--document-name", DOCUMENT,
        "--document-version", document["DocumentVersion"], "--document-hash", document["Hash"],
        "--document-hash-type", "Sha256", "--parameters", json.dumps({"Mode": [mode]}),
        "--timeout-seconds", "60", "--comment", "Nearby dev control run " + os.environ.get("GITHUB_RUN_ID", "unknown")
    )["Command"]["CommandId"]
    record["ssm_command_id"] = command_id
    save(record)

    def complete():
        try:
            response = aws("ssm", "get-command-invocation", "--command-id", command_id,
                           "--instance-id", INSTANCE_ID)
        except AwsError as error:
            if str(error) == "InvocationDoesNotExist":
                return False
            raise
        status = response.get("Status")
        if status in ("Pending", "InProgress", "Delayed"):
            return False
        if status != "Success" or response.get("ResponseCode") != 0:
            raise RuntimeError("Runtime " + mode + " failed: " + str(status) + "; inspect the SSM command")
        lines = [line.removeprefix("NEARBY_RUNTIME_RESULT=")
                 for line in response.get("StandardOutputContent", "").splitlines()
                 if line.startswith("NEARBY_RUNTIME_RESULT=")]
        if len(lines) != 1:
            raise RuntimeError("Runtime command did not return verification evidence")
        evidence = json.loads(lines[0])
        if (evidence.get("instance_id") != INSTANCE_ID or evidence.get("mode") != mode
                or not all(evidence.get(key) is True for key in ("enabled", "active", "ready"))):
            raise RuntimeError("Runtime verification evidence does not match the development server")
        return evidence

    record["runtime"] = wait_for(complete, 310, "SSM runtime check")


def control(action, record):
    validate_identity()
    state = instance()["State"]["Name"]
    record.update(before=state, phase="target_verified")
    save(record)
    if action == "status":
        record.update(after=state, ssm_online=ssm_online(),
                      app_ready=healthy() if state == "running" else False)
        return
    if action == "prepare":
        if state != "running":
            raise ValueError("Prepare requires a running development server with an already deployed app")
        wait_for(ssm_online, 180, "SSM online")
        runtime("prepare", record)
    elif action == "start":
        if state == "stopping":
            wait_state("stopped")
            state = "stopped"
        if state == "stopped":
            record["phase"] = "start_requested"
            save(record)
            aws("ec2", "start-instances", "--instance-ids", INSTANCE_ID)
        elif state not in ("pending", "running"):
            raise ValueError("Cannot start an instance in state " + state)
        wait_state("running")
        record["phase"] = "ec2_running"
        save(record)
        wait_for(ec2_ready, 180, "EC2 status checks")
        wait_for(ssm_online, 180, "SSM online")
        runtime("verify", record)
        wait_for(healthy, 60, "HTTPS readiness")
    elif action == "stop":
        if state == "stopped":
            record.update(after="stopped", phase="already_stopped")
            return
        if state == "stopping":
            wait_state("stopped")
        else:
            if state == "pending":
                wait_state("running")
            elif state != "running":
                raise ValueError("Cannot stop an instance in state " + state)
            wait_for(ssm_online, 180, "SSM online")
            # 자동 시작을 확인하지 못한 서버는 중지하지 않습니다.
            runtime("verify", record)
            record["phase"] = "stop_requested"
            save(record)
            aws("ec2", "stop-instances", "--instance-ids", INSTANCE_ID)
            wait_state("stopped")
        record.update(after="stopped", phase="stopped_verified")
        return
    else:
        raise ValueError("Unsupported action")
    record.update(after="running", phase="application_verified")


def save(record):
    RECORD.parent.mkdir(parents=True, exist_ok=True)
    RECORD.write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n")


def main():
    preflight = sys.argv[1:] == ["preflight"]
    record = {"account_id": ACCOUNT_ID, "region": REGION, "instance_id": INSTANCE_ID,
              "actor": os.environ.get("GITHUB_ACTOR", ""), "run_id": os.environ.get("GITHUB_RUN_ID", ""),
              "run_attempt": os.environ.get("GITHUB_RUN_ATTEMPT", ""), "commit": os.environ.get("GITHUB_SHA", ""),
              "started_at": datetime.now(timezone.utc).isoformat(), "result": "failure", "phase": "preflight"}
    try:
        action, reason = guard()
        record.update(action=action, reason=reason)
        if preflight:
            record["result"] = "preflight_passed"
            return 0
        control(action, record)
        record["result"] = "success"
    except Exception as error:
        record["error"] = str(error) if isinstance(error, (ValueError, RuntimeError)) else type(error).__name__
        print("Development server control failed; see the result artifact.", file=sys.stderr)
    finally:
        record["finished_at"] = datetime.now(timezone.utc).isoformat()
        save(record)
        summary = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary and (not preflight or record["result"] == "failure"):
            with open(summary, "a") as stream:
                stream.write("### Development server control\n<pre>" +
                             html.escape(json.dumps(record, ensure_ascii=False, indent=2)) + "</pre>\n")
    return 0 if record["result"] == "success" else 1


if __name__ == "__main__":
    sys.exit(main())
