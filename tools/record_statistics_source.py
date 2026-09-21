#!/usr/bin/env python3
"""Bind the statistics regression reports to the checked-out commit and exact source blobs."""
import hashlib
import json
import os
from pathlib import Path
import subprocess


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], text=True).strip()


def main() -> None:
    root = Path(git("rev-parse", "--show-toplevel"))
    os.chdir(root)
    package = "top/geek_studio/chenlongcould/musicplayer/ui"
    paths = [
        ".github/workflows/android.yml",
        "tools/record_statistics_source.py",
        "tools/check_statistics_import_preparation.sh",
        *[f"modern-app/src/main/kotlin/{package}/{name}.kt" for name in (
            "ImportPreviewPreparation", "StatisticsOperations", "StatisticsViewModel",
            "StatisticsImportPreparationStatus", "ListeningStatisticsCard",
        )],
        f"modern-app/src/test/kotlin/{package}/ImportPreviewPreparationCases.kt",
        f"modern-app/src/test/kotlin/{package}/ImportPreviewPreparationTest.kt",
        f"modern-app/src/androidTest/kotlin/{package}/StatisticsImportPreparationInstrumentedTest.kt",
    ]
    files = {}
    for path in paths:
        expected = git("rev-parse", f"HEAD:{path}")
        actual = git("hash-object", "--", path)
        if actual != expected:
            raise RuntimeError(f"Uncommitted source change: {path}")
        files[path] = {"gitBlob": actual, "sha256": hashlib.sha256(Path(path).read_bytes()).hexdigest()}
    report = {
        "schemaVersion": 1,
        "checkedOutCommit": git("rev-parse", "HEAD"),
        "checkedOutTree": git("rev-parse", "HEAD^{tree}"),
        "parents": git("show", "-s", "--format=%P", "HEAD").split(),
        "runId": os.getenv("GITHUB_RUN_ID"),
        "runAttempt": os.getenv("GITHUB_RUN_ATTEMPT"),
        "job": os.getenv("GITHUB_JOB"),
        "event": os.getenv("GITHUB_EVENT_NAME"),
        "workflowSha": os.getenv("GITHUB_SHA"),
        "files": files,
    }
    output = root / "build/source-identity.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    content = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    output.write_text(content, encoding="utf-8")
    print(content, end="")


if __name__ == "__main__":
    main()
