#!/usr/bin/env python3
# 배포 입력 검증, 검증된 JAR 포장과 배포 완료 후 릴리즈 게시를 담당한다.
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile


ROOT = Path(__file__).resolve().parents[2]
SHA = re.compile(r"[0-9a-f]{40}")
VERSION = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")


def required(name):
    value = os.environ.get(name, "")
    if not value or "\n" in value or "\r" in value:
        raise ValueError("Missing or invalid setting: " + name)
    return value


def command(args, **kwargs):
    result = subprocess.run(args, text=True, capture_output=True, **kwargs)
    if result.returncode:
        # 명령 인자와 stderr에는 DB 자격 증명이 포함될 수 있다.
        raise RuntimeError("Command failed: " + Path(args[0]).name)
    return result.stdout.strip()


def digest(path):
    value = hashlib.sha256()
    with open(path, "rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def guard():
    environment = required("DEPLOY_ENVIRONMENT")
    branches = {"dev": "develop", "prod": "main"}
    if environment not in branches:
        raise ValueError("Only dev/prod environments are allowed")
    if required("GITHUB_REF") != "refs/heads/" + branches[environment]:
        raise ValueError("Invalid deployment branch for " + environment)
    sha = required("DEPLOY_SHA")
    if not SHA.fullmatch(sha) or sha != required("GITHUB_SHA"):
        raise ValueError("Deployment must use the workflow's exact commit SHA")
    if command(["git", "rev-parse", "HEAD"], cwd=ROOT) != sha:
        raise ValueError("Checkout does not match deployment SHA")
    return environment, sha


def configuration():
    names = ("AWS_REGION", "AWS_ACCOUNT_ID", "EC2_INSTANCE_ID", "DEPLOY_S3_BUCKET",
             "APP_DIR", "APP_JAR_PATH", "APP_SERVICE_NAME", "SSM_ENV_PATH",
             "INTERNAL_HEALTHCHECK_URL", "SERVICE_URL", "HEALTHCHECK_PATH")
    config = {key: required(key) for key in names}
    if not re.fullmatch(r"\d{12}", config["AWS_ACCOUNT_ID"]):
        raise ValueError("AWS_ACCOUNT_ID must have 12 digits")
    if not re.fullmatch(r"i-[a-f0-9]{8,17}", config["EC2_INSTANCE_ID"]):
        raise ValueError("Invalid EC2_INSTANCE_ID")
    if not re.fullmatch(r"[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]", config["DEPLOY_S3_BUCKET"]):
        raise ValueError("Invalid deployment bucket")
    if not re.fullmatch(r"[a-zA-Z0-9_-]+(?:\.service)?", config["APP_SERVICE_NAME"]):
        raise ValueError("Invalid systemd service name")
    for key in ("APP_DIR", "APP_JAR_PATH", "SSM_ENV_PATH"):
        value = config[key]
        if not re.fullmatch(r"/[a-zA-Z0-9_./-]+", value) or ".." in Path(value).parts:
            raise ValueError("Invalid absolute path: " + key)
    app_dir = Path(config["APP_DIR"])
    if app_dir == Path("/") or not Path(config["APP_JAR_PATH"]).is_relative_to(app_dir):
        raise ValueError("APP_JAR_PATH must be inside APP_DIR")
    internal = urllib.parse.urlsplit(config["INTERNAL_HEALTHCHECK_URL"])
    external = urllib.parse.urlsplit(config["SERVICE_URL"])
    if (internal.scheme != "http" or internal.hostname not in ("127.0.0.1", "localhost")
            or internal.path != "/actuator/health" or internal.username or internal.query):
        raise ValueError("Internal health URL must use loopback HTTP /actuator/health")
    if external.scheme != "https" or not external.hostname or external.username:
        raise ValueError("SERVICE_URL must use HTTPS without credentials")
    if not config["HEALTHCHECK_PATH"].startswith("/") or config["HEALTHCHECK_PATH"].startswith("//"):
        raise ValueError("Invalid external health path")
    return config


def release_tag():
    version = required("RELEASE_VERSION")
    if not VERSION.fullmatch(version):
        raise ValueError("Version must be final MAJOR.MINOR.PATCH without leading zeroes")
    return "be-v" + version


def preflight():
    environment, _ = guard()
    configuration()
    for name in ("AWS_ROLE_TO_ASSUME", "FLYWAY_DB_URL", "FLYWAY_DB_USER", "FLYWAY_DB_PASSWORD"):
        required(name)
    if environment == "prod":
        tag = release_tag()
        found = subprocess.run(["git", "show-ref", "--verify", "--quiet", "refs/tags/" + tag])
        if found.returncode == 0:
            raise ValueError("Release tag already exists: " + tag)
        if found.returncode != 1:
            raise RuntimeError("Cannot inspect release tags")


def package():
    sha = required("DEPLOY_SHA")
    if not SHA.fullmatch(sha) or command(["git", "rev-parse", "HEAD"], cwd=ROOT) != sha:
        raise ValueError("Artifact SHA does not match checkout")
    if command(["git", "status", "--porcelain", "--untracked-files=normal"], cwd=ROOT):
        raise ValueError("Release artifact requires a clean checkout")
    jars = list((ROOT / "bootstrap/build/libs").glob("*.jar"))
    boot_jars = []
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            manifest = archive.read("META-INF/MANIFEST.MF").decode()
            if ("Main-Class: org.springframework.boot.loader.launch.JarLauncher" in manifest
                    and "Start-Class: com.sopt.nearby.NearbyApplication" in manifest):
                boot_jars.append(jar)
    if len(boot_jars) != 1:
        raise ValueError("Expected exactly one Nearby boot jar")
    destination = ROOT / "build/deployment/package"
    destination.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(boot_jars[0], destination / "app.jar")
    (destination / "manifest.json").write_text(json.dumps({
        "commit_sha": sha, "sha256": digest(destination / "app.jar")
    }, indent=2) + "\n")


def initialize_metadata():
    destination = ROOT / "build/deployment/metadata.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps({
        "status": "not_started", "phase": "before_deploy",
        "commit_sha": required("DEPLOY_SHA"), "environment": required("DEPLOY_ENVIRONMENT"),
        "release_version": os.environ.get("RELEASE_VERSION", "")
    }, indent=2) + "\n")


def api(path, data=None):
    base = os.environ.get("GITHUB_API_URL", "https://api.github.com")
    request = urllib.request.Request(base + "/repos/" + required("GITHUB_REPOSITORY") + "/" + path,
                                     data=None if data is None else json.dumps(data).encode(),
                                     headers={"Authorization": "Bearer " + required("GH_TOKEN"),
                                              "Accept": "application/vnd.github+json",
                                              "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if error.code == 404 and data is None:
            return None
        raise RuntimeError("GitHub API failed with status " + str(error.code)) from None


def publish_release():
    environment, sha = guard()
    tag = release_tag()
    metadata = json.loads((ROOT / "build/deployment/metadata.json").read_text())
    if (environment != "prod" or metadata.get("status") != "success"
            or metadata.get("commit_sha") != sha or metadata.get("release_version") != required("RELEASE_VERSION")):
        raise ValueError("Missing successful deployment evidence for this release")
    existing = api("git/ref/tags/" + tag)
    if existing:
        obj = existing["object"]
        if obj["type"] != "tag":
            raise ValueError("Existing tag must be annotated")
        annotated = api("git/tags/" + obj["sha"])
        if annotated["object"]["sha"] != sha or annotated["object"]["type"] != "commit":
            raise ValueError("Existing tag points to a different commit")
    else:
        annotated = api("git/tags", {"tag": tag, "message": tag + " Nearby production deployment",
                                       "object": sha, "type": "commit"})
        api("git/refs", {"ref": "refs/tags/" + tag, "sha": annotated["sha"]})
    if not api("releases/tags/" + tag):
        notes = ROOT / "build/deployment/release-notes.md"
        notes.write_text("- Commit: " + sha + "\n- Artifact: " + metadata["s3_uri"]
                         + "\n- SHA256: " + metadata["sha256"] + "\n- SSM command: "
                         + metadata["command_id"] + "\n")
        command(["gh", "release", "create", tag, "--verify-tag", "--target", sha,
                 "--title", tag, "--generate-notes", "--notes-file", str(notes),
                 str(ROOT / "build/deployment/metadata.json")])
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a") as stream:
            stream.write("\nPublished release: " + tag + " (" + sha + ")\n")


if __name__ == "__main__":
    try:
        {"guard": guard, "preflight": preflight, "package": package,
         "release": publish_release, "initialize": initialize_metadata}[sys.argv[1]]()
    except (ValueError, RuntimeError) as error:
        sys.exit(str(error))
