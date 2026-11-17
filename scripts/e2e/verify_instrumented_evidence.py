#!/usr/bin/env python3
"""Verify the instrumented E2E evidence produced by AGP on the emulator.

AGP writes a single device-level XML per connected run (root ``<testsuites>``)
whose children are per-class ``<testsuite>`` elements. The E2E suite is therefore
selected by name, not by file name.

Exit code 0 means: the expected suite ran at least ``--min-tests`` tests and
reported no failure, error or skipped test. The matching XML is copied to the
output directory for the CI artifact.
"""

from __future__ import annotations

import argparse
import glob
import os
import shutil
import sys
import xml.etree.ElementTree as ET


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--results-dir", required=True)
    parser.add_argument("--out-dir", required=True)
    parser.add_argument("--suite", default="VpnTunnelE2ETest")
    parser.add_argument("--min-tests", type=int, default=3)
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    pattern = os.path.join(args.results_dir, "**", "*.xml")
    matched: list[str] = []
    tests = failures = errors = skipped = 0

    for path in sorted(glob.glob(pattern, recursive=True)):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as exc:
            print(f"unreadable XML {path}: {exc}")
            continue
        suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
        for suite in suites:
            name = suite.get("name", "")
            if args.suite not in name:
                continue
            matched.append(path)
            tests += int(suite.get("tests", "0"))
            failures += int(suite.get("failures", "0"))
            errors += int(suite.get("errors", "0"))
            skipped += int(suite.get("skipped", "0"))
            print(
                f"suite {name}: tests={suite.get('tests')} "
                f"failures={suite.get('failures')} errors={suite.get('errors')} "
                f"skipped={suite.get('skipped')}"
            )

    if not matched:
        print(f"E2E_XML_MISSING (no {args.suite} suite under {args.results_dir})")
        return 1

    shutil.copyfile(matched[0], os.path.join(args.out_dir, f"{args.suite}.xml"))
    print(f"total tests={tests} failures={failures} errors={errors} skipped={skipped}")
    if tests < args.min_tests or failures or errors or skipped:
        print(
            f"E2E_XML_INVALID (expected >= {args.min_tests} tests with no "
            "failure, error or skip)"
        )
        return 1
    print("E2E_XML_VALIDATED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
