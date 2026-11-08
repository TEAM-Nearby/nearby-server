#!/usr/bin/env python3
# 격리된 파일과 가짜 외부 명령으로 EC2 배포의 교체·복구·실패 상태를 검증한다.
import base64
from contextlib import redirect_stdout
import io
import json
import os
from pathlib import Path
from types import SimpleNamespace
import sys
import tempfile
import unittest
from unittest.mock import patch


sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "deploy"))
import ec2


class Ec2DeploymentTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name).resolve()
        self.app = self.root / "app"
        self.app.mkdir()
        self.jar = self.app / "app.jar"
        self.jar.write_bytes(b"previous jar")
        self.env = self.app / "app.env"
        self.env.write_text('OLD_SETTING="previous"\n')
        self.current = self.app / "deployment.json"
        self.current.write_text(json.dumps({"commit_sha": "b" * 40}))
        self.systemd = self.root / "systemd"
        self.dropin = self.systemd / "nearby.service.d/90-nearby-deployment.conf"
        self.dropin.parent.mkdir(parents=True)
        self.dropin.write_text("[Service]\nEnvironmentFile=/previous/app.env\n")
        self.proc = self.root / "proc"
        (self.proc / "202").mkdir(parents=True)
        (self.proc / "202/cmdline").write_bytes(b"java\0-jar\0" + str(self.jar).encode() + b"\0")
        self.payload = b"verified new jar"
        checksum_source = self.root / "source.jar"
        checksum_source.write_bytes(self.payload)
        self.config = {
            "APP_DIR": str(self.app), "APP_JAR_PATH": str(self.jar),
            "APP_SERVICE_NAME": "nearby", "AWS_REGION": "ap-northeast-2",
            "SSM_ENV_PATH": "/nearby/test", "s3_uri": "s3://test-only/app.jar",
            "sha256": ec2.sha256(checksum_source), "commit_sha": "a" * 40,
            "deployment_id": "123-1", "INTERNAL_HEALTHCHECK_URL": "http://127.0.0.1/actuator/health"
        }
        self.parameters = [{"Name": "/nearby/test/" + key, "Value": value} for key, value in {
            "SPRING_PROFILES_ACTIVE": "supabase,monitoring",
            "SPRING_DATASOURCE_URL": "jdbc:postgresql://test.invalid/test",
            "SPRING_DATASOURCE_USERNAME": "test",
            "SPRING_DATASOURCE_PASSWORD": "private-test-password"
        }.items()]
        self.calls = []
        self.started = False
        self.fail_command = None
        self.result = {"phase": "before_switch", "status": "failure"}
        self.state = self.app / ".deployment-state/123-1"
        self.patch(ec2, "run", side_effect=self.fake_run)
        self.patch(ec2.os, "geteuid", return_value=0)
        self.patch(ec2.os, "chown")
        self.patch(ec2.pwd, "getpwnam", return_value=SimpleNamespace(pw_uid=1000, pw_gid=1000))
        self.patch(ec2, "healthy", return_value=True)
        # main()의 umask와 리전 변경을 다른 테스트로 전파하지 않는다.
        previous_umask = os.umask(0o077)
        self.addCleanup(os.umask, previous_umask)
        self.env_patch = patch.dict(os.environ)
        self.env_patch.start()
        self.addCleanup(self.env_patch.stop)

    def patch(self, target, name, **kwargs):
        replacement = patch.object(target, name, **kwargs)
        value = replacement.start()
        self.addCleanup(replacement.stop)
        return value

    def fake_run(self, args, input_text=None):
        self.calls.append(args)
        if self.fail_command and args[:2] == self.fail_command:
            raise RuntimeError("Simulated remote command failure")
        if args[0] == "findmnt":
            return "ext4"
        if args[:3] == ["aws", "s3", "cp"]:
            Path(args[4]).write_bytes(self.payload)
        elif args[:3] == ["aws", "ssm", "get-parameters-by-path"]:
            return json.dumps({"Parameters": self.parameters})
        elif args[:2] == ["systemctl", "show"]:
            if "--property=ExecStart" in args:
                return "java -jar " + str(self.jar)
            return "202" if self.started else "101"
        elif args[:2] == ["systemctl", "stop"]:
            # 정지 전까지 기존 앱과 환경이 그대로 유지되어야 한다.
            self.assertEqual(b"previous jar", self.jar.read_bytes())
            self.assertEqual('OLD_SETTING="previous"\n', self.env.read_text())
        elif args[:2] == ["systemctl", "start"]:
            self.assertEqual(self.payload, self.jar.read_bytes())
            self.assertIn('SPRING_FLYWAY_ENABLED="false"', self.env.read_text())
            self.assertIn('SPRING_JPA_HIBERNATE_DDL_AUTO="validate"', self.env.read_text())
            self.started = True
        return ""

    def execute(self):
        ec2.deploy(self.config, self.result, systemd_root=self.systemd, proc_root=self.proc)

    def assert_recovery(self):
        self.assertEqual(str(self.state), self.result["recovery_directory"])
        self.assertEqual(b"previous jar", (self.state / "previous.jar").read_bytes())
        self.assertEqual('OLD_SETTING="previous"\n', (self.state / "previous.env").read_text())
        self.assertEqual("b" * 40, json.loads((self.state / "previous-deployment.json").read_text())["commit_sha"])
        self.assertIn("/previous/app.env", (self.state / "previous-dropin.conf").read_text())
        self.assertEqual(0o700, self.state.stat().st_mode & 0o777)
        self.assertFalse([path for path in self.app.glob(".deploy.*") if path.is_dir()])

    def test_success_installs_verified_jar_and_preserves_previous_release(self):
        self.execute()
        self.assert_recovery()
        self.assertEqual(self.payload, self.jar.read_bytes())
        self.assertEqual(0o600, self.env.stat().st_mode & 0o777)
        recorded = json.loads(self.current.read_text())
        self.assertEqual("success", recorded["status"])
        self.assertEqual("internal_verified", recorded["phase"])
        self.assertEqual(self.config["commit_sha"], recorded["commit_sha"])
        self.assertEqual(self.config["sha256"], recorded["sha256"])
        self.assertEqual(recorded, json.loads((self.state / "result.json").read_text()))

    def test_bad_checksum_leaves_service_and_files_untouched(self):
        self.config["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "checksum"):
            self.execute()
        self.assertEqual("before_switch", self.result["phase"])
        self.assertEqual(b"previous jar", self.jar.read_bytes())
        self.assertFalse(self.state.exists())
        self.assertNotIn(["systemctl", "stop", "nearby.service"], self.calls)
        self.assertFalse([path for path in self.app.glob(".deploy.*") if path.is_dir()])

    def test_missing_runtime_parameters_do_not_stop_service(self):
        self.parameters = []
        with self.assertRaisesRegex(ValueError, "Missing runtime"):
            self.execute()
        self.assertEqual("before_switch", self.result["phase"])
        self.assertEqual(b"previous jar", self.jar.read_bytes())
        self.assertNotIn(["systemctl", "stop", "nearby.service"], self.calls)

    def test_jar_install_failure_preserves_recovery_without_restart(self):
        actual_copy = ec2.atomic_copy
        def fail_jar_copy(source, target, mode):
            if Path(target) == self.jar:
                raise OSError("Simulated disk full")
            actual_copy(source, target, mode)
        with patch.object(ec2, "atomic_copy", side_effect=fail_jar_copy):
            with self.assertRaises(OSError):
                self.execute()
        self.assert_recovery()
        self.assertEqual("switch_started", self.result["phase"])
        self.assertEqual(b"previous jar", self.jar.read_bytes())
        self.assertNotIn(["systemctl", "start", "nearby.service"], self.calls)

    def test_start_failure_leaves_new_jar_and_previous_recovery(self):
        self.fail_command = ["systemctl", "start"]
        with self.assertRaises(RuntimeError):
            self.execute()
        self.assert_recovery()
        self.assertEqual("switch_started", self.result["phase"])
        self.assertEqual(self.payload, self.jar.read_bytes())
        self.assertEqual("b" * 40, json.loads(self.current.read_text())["commit_sha"])
        self.assertEqual(1, self.calls.count(["systemctl", "start", "nearby.service"]))

    def test_health_timeout_records_started_phase_and_keeps_recovery(self):
        with patch.object(ec2.time, "monotonic", side_effect=[0, 121]):
            with self.assertRaisesRegex(RuntimeError, "health check timed out"):
                self.execute()
        self.assert_recovery()
        self.assertEqual("service_started", self.result["phase"])
        self.assertEqual("b" * 40, json.loads(self.current.read_text())["commit_sha"])
        self.assertEqual(self.payload, self.jar.read_bytes())

    def test_wrong_running_jar_does_not_publish_success_metadata(self):
        (self.proc / "202/cmdline").write_bytes(b"java\0-jar\0/other/app.jar\0")
        with self.assertRaisesRegex(RuntimeError, "expected JAR"):
            self.execute()
        self.assert_recovery()
        self.assertEqual("service_started", self.result["phase"])
        self.assertEqual("b" * 40, json.loads(self.current.read_text())["commit_sha"])

    def test_metadata_write_failure_exits_nonzero_and_reports_failure(self):
        actual_copy = ec2.atomic_copy
        actual_deploy = ec2.deploy
        def fail_metadata_copy(source, target, mode):
            if Path(target) == self.current:
                raise OSError("Simulated disk full")
            actual_copy(source, target, mode)
        def isolated_deploy(config, result):
            return actual_deploy(config, result, systemd_root=self.systemd, proc_root=self.proc)
        encoded = base64.b64encode(json.dumps(self.config).encode()).decode()
        output = io.StringIO()
        with patch.object(ec2, "atomic_copy", side_effect=fail_metadata_copy), \
                patch.object(ec2, "deploy", side_effect=isolated_deploy), \
                patch.object(sys, "argv", ["ec2.py", encoded]), redirect_stdout(output):
            code = ec2.main()
        self.assertEqual(1, code)
        result = json.loads(output.getvalue().split("=", 1)[1])
        self.assertEqual("failure", result["status"])
        self.assertEqual("internal_verified", result["phase"])
        self.assertNotIn("private-test-password", output.getvalue())
        self.assertTrue((self.state / "previous.jar").exists())


if __name__ == "__main__":
    unittest.main()
