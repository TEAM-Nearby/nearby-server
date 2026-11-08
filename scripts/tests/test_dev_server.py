#!/usr/bin/env python3
# AWS 자원 없이 개발 서버 제어의 대상 제한과 중복 실행 및 실패 경로를 검증합니다.
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch


spec = importlib.util.spec_from_file_location(
    "dev_server", Path(__file__).resolve().parents[1] / "operations/dev_server.py")
dev = importlib.util.module_from_spec(spec)
spec.loader.exec_module(dev)
SHA = "a" * 40
HEALTHY = dev.healthy


class InputTests(unittest.TestCase):
    def setUp(self):
        env = patch.dict(os.environ, {"GITHUB_REPOSITORY": "TEAM-Nearby/nearby-server",
            "GITHUB_REF": "refs/heads/develop", "GITHUB_EVENT_NAME": "workflow_dispatch",
            "GITHUB_SHA": SHA, "CONTROL_ACTION": "status", "CONTROL_REASON": "",
            "STOP_CONFIRMATION": ""})
        env.start()
        self.addCleanup(env.stop)
        checkout = patch.object(dev.subprocess, "check_output", return_value=SHA + "\n")
        checkout.start()
        self.addCleanup(checkout.stop)

    def test_wrong_repo_branch_event_and_commit_are_rejected(self):
        for key, value in (("GITHUB_REPOSITORY", "someone/fork"), ("GITHUB_REF", "refs/heads/main"),
                           ("GITHUB_EVENT_NAME", "push"), ("GITHUB_SHA", "b" * 40)):
            with self.subTest(key=key), patch.dict(os.environ, {key: value}), self.assertRaises(ValueError):
                dev.guard()

    def test_stop_requires_both_reason_and_literal_confirmation(self):
        for reason, confirmation in (("", "stop-dev"), ("x", "stop-dev"), ("개발 종료", ""),
                                     ("개발 종료", "stop-prod"), ("a\nb", "stop-dev")):
            with self.subTest(reason=reason, confirmation=confirmation), patch.dict(os.environ, {
                "CONTROL_ACTION": "stop", "CONTROL_REASON": reason, "STOP_CONFIRMATION": confirmation
            }), self.assertRaises(ValueError):
                dev.guard()

    def test_reason_is_literal_data_and_not_shell_code(self):
        reason = '$(touch /tmp/should-not-exist) `echo text`'
        with patch.dict(os.environ, {"CONTROL_ACTION": "stop", "CONTROL_REASON": reason,
                                     "STOP_CONFIRMATION": "stop-dev"}):
            self.assertEqual(("stop", reason), dev.guard())

    def test_preflight_rejects_invalid_input_before_aws_and_records_failure(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(dev, "RECORD", Path(directory) / "result.json"), \
                patch.object(dev.sys, "argv", ["dev_server.py", "preflight"]), \
                patch.dict(os.environ, {"CONTROL_ACTION": "stop"}), patch.object(dev, "aws") as aws:
            self.assertEqual(1, dev.main())
            aws.assert_not_called()
            self.assertEqual("failure", json.loads(dev.RECORD.read_text())["result"])


class ControlTests(unittest.TestCase):
    def setUp(self):
        self.calls = []
        self.state = "running"
        self.name = "nearby-app"
        self.account = dev.ACCOUNT_ID
        self.runtime_status = "Success"
        self.runtime_code = 0
        self.evidence = True
        self.mode = "verify"
        self.app_ready = True
        self.record = {}
        self.patch(dev, "aws", side_effect=self.aws)
        self.patch(dev, "save")
        self.patch(dev, "healthy", side_effect=lambda: self.app_ready)

    def patch(self, target, name, **kwargs):
        patcher = patch.object(target, name, **kwargs)
        result = patcher.start()
        self.addCleanup(patcher.stop)
        return result

    def aws(self, *args):
        self.calls.append(args)
        if "--instance-ids" in args or "--instance-id" in args:
            key = "--instance-ids" if "--instance-ids" in args else "--instance-id"
            self.assertEqual(dev.INSTANCE_ID, args[args.index(key) + 1])
        operation = args[1]
        if operation == "get-caller-identity":
            return {"Account": self.account, "Arn": f"arn:aws:sts::{self.account}:assumed-role/{dev.ROLE_NAME}/run"}
        if operation == "describe-instances":
            return {"Reservations": [{"OwnerId": dev.ACCOUNT_ID, "Instances": [{
                "InstanceId": dev.INSTANCE_ID, "State": {"Name": self.state},
                "Tags": [{"Key": "Name", "Value": self.name}]}]}]}
        if operation == "describe-instance-status":
            return {"InstanceStatuses": [{"SystemStatus": {"Status": "ok"}, "InstanceStatus": {"Status": "ok"}}]}
        if operation == "describe-instance-information":
            return {"InstanceInformationList": [{"InstanceId": dev.INSTANCE_ID, "PingStatus": "Online"}]}
        if operation == "describe-document":
            return {"Document": {"Name": dev.DOCUMENT, "HashType": "Sha256", "Hash": "f" * 64, "DocumentVersion": "1"}}
        if operation == "send-command":
            self.mode = json.loads(args[args.index("--parameters") + 1])["Mode"][0]
            return {"Command": {"CommandId": "command-1"}}
        if operation == "get-command-invocation":
            evidence = {"instance_id": dev.INSTANCE_ID, "mode": self.mode, "enabled": True, "active": True, "ready": True}
            return {"Status": self.runtime_status, "ResponseCode": self.runtime_code,
                    "StandardOutputContent": "NEARBY_RUNTIME_RESULT=" + json.dumps(evidence) if self.evidence else ""}
        if operation == "start-instances":
            self.state = "running"
            return {}
        if operation == "stop-instances":
            self.state = "stopped"
            return {}
        raise AssertionError(args)

    def mutations(self):
        return [call[1] for call in self.calls if call[1] in ("start-instances", "stop-instances", "send-command")]

    def test_status_never_changes_the_server(self):
        dev.control("status", self.record)
        self.assertEqual([], self.mutations())
        self.assertTrue(self.record["app_ready"])

    def test_wrong_account_or_production_name_blocks_all_mutations(self):
        for field, value in (("account", "340271093505"), ("name", "nearby-prod-app")):
            with self.subTest(field=field), patch.object(self, field, value), self.assertRaises(ValueError):
                dev.control("stop", self.record)
            self.assertEqual([], self.mutations())

    def test_start_waits_for_runtime_and_external_readiness(self):
        self.state = "stopped"
        dev.control("start", self.record)
        self.assertEqual(["start-instances", "send-command"], self.mutations())
        self.assertEqual("verify", self.mode)
        self.assertEqual("application_verified", self.record["phase"])
        self.assertEqual("running", self.record["after"])

    def test_start_already_running_does_not_start_or_restart(self):
        dev.control("start", self.record)
        self.assertEqual(["send-command"], self.mutations())

    def test_start_failure_leaves_running_instance_for_diagnosis(self):
        self.state = "stopped"
        self.runtime_status = "Failed"
        self.runtime_code = 1
        with self.assertRaisesRegex(RuntimeError, "Runtime verify failed"):
            dev.control("start", self.record)
        self.assertEqual("running", self.state)
        self.assertNotIn("stop-instances", self.mutations())
        self.assertEqual("ec2_running", self.record["phase"])

    def test_stop_verifies_runtime_before_stopping(self):
        dev.control("stop", self.record)
        self.assertEqual(["send-command", "stop-instances"], self.mutations())
        self.assertEqual("stopped_verified", self.record["phase"])

    def test_stop_does_not_proceed_when_auto_start_verification_fails(self):
        self.runtime_status = "Failed"
        with self.assertRaises(RuntimeError):
            dev.control("stop", self.record)
        self.assertNotIn("stop-instances", self.mutations())

    def test_success_without_runtime_evidence_is_not_enough_to_stop(self):
        self.evidence = False
        with self.assertRaisesRegex(RuntimeError, "evidence"):
            dev.control("stop", self.record)
        self.assertNotIn("stop-instances", self.mutations())

    def test_already_stopped_is_an_idempotent_success(self):
        self.state = "stopped"
        dev.control("stop", self.record)
        self.assertEqual([], self.mutations())
        self.assertEqual("already_stopped", self.record["phase"])

    def test_prepare_does_not_change_power_state(self):
        dev.control("prepare", self.record)
        self.assertEqual(["send-command"], self.mutations())
        self.assertEqual("prepare", self.mode)

    def test_prepare_requires_running_instance(self):
        self.state = "stopped"
        with self.assertRaises(ValueError):
            dev.control("prepare", self.record)
        self.assertEqual([], self.mutations())

    def test_wait_has_a_deadline(self):
        with patch.object(dev.time, "monotonic", side_effect=[0, 11]), self.assertRaisesRegex(RuntimeError, "timed out"):
            dev.wait_for(lambda: False, 10, "SSM online")

    def test_https_200_html_does_not_count_as_readiness(self):
        # 실제 요청 없이 응답 검증 함수가 HTML 로그인 페이지를 거부하는지 확인합니다.
        with patch.object(dev.urllib.request, "urlopen") as opener:
            opener.return_value.__enter__.return_value.status = 200
            opener.return_value.__enter__.return_value.read.return_value = b"<html>login</html>"
            self.assertFalse(HEALTHY())


if __name__ == "__main__":
    unittest.main()
