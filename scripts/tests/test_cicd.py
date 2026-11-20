#!/usr/bin/env python3
# 운영 자원 없이 배포 차단 조건과 부분 실패 및 백업 게시 순서를 검증한다.
from datetime import datetime, timezone
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts/deploy"))
import pipeline
import deploy
import ec2

spec = importlib.util.spec_from_file_location("backup", ROOT / "scripts/backup/backup.py")
backup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(backup)
COMMIT = "a" * 40


class GuardTests(unittest.TestCase):
    def test_environment_branch_and_sha_are_bound(self):
        for environment, branch in (("dev", "develop"), ("prod", "main")):
            with self.subTest(environment=environment), patch.dict(os.environ, {
                "DEPLOY_ENVIRONMENT": environment, "GITHUB_REF": "refs/heads/" + branch,
                "DEPLOY_SHA": COMMIT, "GITHUB_SHA": COMMIT
            }), patch.object(pipeline, "command", return_value=COMMIT):
                self.assertEqual((environment, COMMIT), pipeline.guard())
                with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/feature"}):
                    with self.assertRaises(ValueError):
                        pipeline.guard()
                with patch.dict(os.environ, {"DEPLOY_SHA": "b" * 40}):
                    with self.assertRaises(ValueError):
                        pipeline.guard()

    def test_invalid_environment_cannot_reach_a_database(self):
        with patch.dict(os.environ, {"DEPLOY_ENVIRONMENT": "staging"}):
            with self.assertRaises(ValueError):
                pipeline.guard()

    def test_release_versions(self):
        for value in ("01.2.3", "1.02.3", "1.2.03", "v1.2.3", "1.2.3-rc1", "1.2.3+build", "1.2"):
            with self.subTest(value=value), patch.dict(os.environ, {"RELEASE_VERSION": value}):
                with self.assertRaises(ValueError):
                    pipeline.release_tag()
        with patch.dict(os.environ, {"RELEASE_VERSION": "0.2.10"}):
            self.assertEqual("be-v0.2.10", pipeline.release_tag())

    def test_existing_tag_blocks_before_migration(self):
        with patch.object(pipeline, "guard", return_value=("prod", COMMIT)), \
                patch.object(pipeline, "configuration"), patch.object(pipeline, "required", return_value="present"), \
                patch.object(pipeline, "release_tag", return_value="be-v1.2.3"), \
                patch.object(subprocess, "run", return_value=subprocess.CompletedProcess([], 0)):
            with self.assertRaisesRegex(ValueError, "already exists"):
                pipeline.preflight()

    def test_dirty_sources_cannot_be_labeled_as_a_commit_artifact(self):
        with patch.dict(os.environ, {"DEPLOY_SHA": COMMIT}), \
                patch.object(pipeline, "command", side_effect=[COMMIT, " M application/Changed.java"]):
            with self.assertRaisesRegex(ValueError, "clean checkout"):
                pipeline.package()

    def test_failed_deployment_cannot_publish_a_release(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "build/deployment").mkdir(parents=True)
            (root / "build/deployment/metadata.json").write_text('{"status":"failure"}')
            with patch.object(pipeline, "ROOT", root), patch.object(pipeline, "guard", return_value=("prod", COMMIT)), \
                    patch.object(pipeline, "release_tag", return_value="be-v1.2.3"), patch.object(pipeline, "api") as api:
                with self.assertRaises(ValueError):
                    pipeline.publish_release()
                api.assert_not_called()


class RemoteTests(unittest.TestCase):
    def test_production_keeps_profile_image_preflight(self):
        with patch.dict(os.environ, {"DEPLOY_ENVIRONMENT": "prod"}):
            command = deploy.remote_command({"test": True})
        import base64
        import shlex
        payload = json.loads(base64.b64decode(shlex.split(command)[-1]))
        self.assertIn("profile_image_check", payload)
        script = base64.b64decode(payload["profile_image_check"]).decode()
        self.assertIn("Production profile image upload and download verified", script)

    def parameters(self):
        return [{"Name": "/nearby/" + key, "Value": value} for key, value in {
            "SPRING_PROFILES_ACTIVE": "supabase,monitoring",
            "SPRING_DATASOURCE_URL": "jdbc:postgresql://db/test",
            "SPRING_DATASOURCE_USERNAME": "app",
            "SPRING_DATASOURCE_PASSWORD": 'special "$dollar\\slash',
            "SPRING_FLYWAY_ENABLED": "true"
        }.items()]

    def test_runtime_cannot_repeat_migration_and_quotes_are_preserved(self):
        content = ec2.environment_file(self.parameters())
        self.assertIn('SPRING_FLYWAY_ENABLED="false"\n', content)
        self.assertIn('SPRING_JPA_HIBERNATE_DDL_AUTO="validate"\n', content)
        self.assertIn('special \\"$dollar\\\\slash', content)

    def test_empty_duplicate_or_multiline_parameters_fail_before_switch(self):
        for parameters in ([], self.parameters() * 2,
                           self.parameters() + [{"Name": "/nearby/BAD", "Value": "a\nb"}]):
            with self.subTest(parameters=len(parameters)), self.assertRaises(ValueError):
                ec2.environment_file(parameters)

    def test_failed_copy_does_not_corrupt_current_jar(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "current.jar"
            target.write_bytes(b"old")
            with patch.object(ec2.shutil, "copyfile", side_effect=OSError("disk full")):
                with self.assertRaises(OSError):
                    ec2.atomic_copy("new.jar", target, 0o644)
            self.assertEqual(b"old", target.read_bytes())
            self.assertEqual([target], list(Path(directory).iterdir()))

    def test_generated_ssm_command_is_valid_shell_and_python(self):
        config = {"name": "quoted ' value $not_executed"}
        command = deploy.remote_command(config)
        subprocess.run(["bash", "-n"], input=command, text=True, check=True)
        compile((ROOT / "scripts/deploy/ec2.py").read_text(), "ec2.py", "exec")
        self.assertNotIn("$not_executed", command)

    def test_ssm_success_requires_application_and_identity_evidence(self):
        for remote in ({}, {"status": "success", "commit_sha": "b" * 40, "sha256": "checksum"}):
            invocation = {"Status": "Success", "ResponseCode": 0,
                          "StandardOutputContent": "NEARBY_DEPLOYMENT_RESULT=" + json.dumps(remote)}
            with self.subTest(remote=remote), patch.object(deploy, "aws", return_value=invocation):
                with self.assertRaises(RuntimeError):
                    deploy.wait_command("command", "instance", {"commit_sha": COMMIT, "sha256": "checksum"})

    def test_remote_failure_preserves_switch_phase(self):
        for phase in ("before_switch", "switch_started", "service_started"):
            result = {"Status": "Failed", "ResponseCode": 1,
                      "StandardOutputContent": 'NEARBY_DEPLOYMENT_RESULT=' + json.dumps({"phase": phase})}
            metadata = {}
            with self.subTest(phase=phase), patch.object(deploy, "aws", return_value=result):
                with self.assertRaises(RuntimeError):
                    deploy.wait_command("command", "instance", metadata)
                self.assertEqual(phase, metadata["remote"]["phase"])

    def test_timeout_does_not_report_no_change_or_rollback(self):
        metadata = {}
        with self.assertRaises(RuntimeError):
            deploy.wait_command("command", "instance", metadata, timeout=0)
        self.assertEqual("unknown_timeout", metadata["ssm_status"])

    def test_external_html_200_is_not_a_success(self):
        with patch.object(deploy.time, "monotonic", side_effect=[0, 0, 100]), \
                patch.object(deploy.time, "sleep"), patch.object(deploy.urllib.request, "urlopen") as request:
            request.return_value.__enter__.return_value.status = 200
            with patch.object(deploy.json, "load", side_effect=ValueError("HTML")):
                with self.assertRaisesRegex(RuntimeError, "External health"):
                    deploy.external_health("https://example.invalid/readyz")


class BackupTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.env = patch.dict(os.environ, {
            "BACKUP_S3_BUCKET": "backup-bucket", "SUPABASE_DB_URL": "postgres://test.invalid/db",
            "BACKUP_ENCRYPTION_PASSPHRASE": "test-passphrase", "RUNNER_TEMP": self.directory.name,
            "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "1"
        })
        self.env.start()
        self.addCleanup(self.env.stop)
        # main에서 변경하는 umask는 테스트 프로세스 밖으로 전파하지 않는다.
        self.umask = os.umask(0o077)
        self.addCleanup(os.umask, self.umask)

    def fake_command(self, args):
        if args[0] == "supabase":
            Path(args[args.index("-f") + 1]).write_text("-- SQL test fixture\nSELECT 1;\n")
        elif args[0] == "gpg":
            Path(args[args.index("--output") + 1]).write_bytes(b"encrypted fixture")
        else:
            self.fail("Unexpected command " + args[0])
        return ""

    def test_monthly_uses_korean_date_and_latest_is_last(self):
        uploads = []
        def record(path, bucket, key):
            uploads.append((key, path.read_bytes()))
        with patch.object(backup, "run", side_effect=self.fake_command), patch.object(backup, "upload", side_effect=record):
            backup.main(datetime(2026, 10, 31, 20, 0, tzinfo=timezone.utc))
        keys = [key for key, _ in uploads]
        self.assertTrue(any("/monthly/2026/11/" in key for key in keys))
        self.assertEqual("nearby-db/latest/manifest.json", keys[-1])
        latest = json.loads(uploads[-1][1])
        self.assertEqual(3, len(latest["files"]))
        self.assertTrue(all("/daily/2026/11/01/" in item["key"] for item in latest["files"]))
        self.assertEqual([], list(Path(self.directory.name).iterdir()))

    def test_partial_upload_failure_preserves_latest_and_cleans_plaintext(self):
        keys = []
        def fail_on_second(path, bucket, key):
            keys.append(key)
            if len(keys) == 2:
                raise RuntimeError("Simulated upload failure")
        with patch.object(backup, "run", side_effect=self.fake_command), patch.object(backup, "upload", side_effect=fail_on_second):
            with self.assertRaises(RuntimeError):
                backup.main(datetime(2026, 11, 2, tzinfo=timezone.utc))
        self.assertFalse(any("/latest/" in key for key in keys))
        self.assertEqual([], list(Path(self.directory.name).iterdir()))

    def test_empty_dump_is_rejected(self):
        with patch.object(backup, "run", return_value=""), patch.object(backup, "upload") as upload:
            with self.assertRaises(ValueError):
                backup.main()
            upload.assert_not_called()
        self.assertEqual([], list(Path(self.directory.name).iterdir()))


if __name__ == "__main__":
    unittest.main()
