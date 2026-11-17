#!/usr/bin/env python3
"""Verify the instrumented E2E evidence produced by AGP on the emulator.

AGP writes one device-level XML per connected run whose root is a single
``<testsuite>`` element named after the first test class, but whose
``<testcase>`` children belong to many classes (each one carries a
``classname`` attribute). Selecting the suite by name therefore misses the E2E
tests; this script selects the test cases by ``classname`` instead.

Exit code 0 means: the expected class ran at least ``--min-tests`` tests and
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


def cases_for(root: ET.Element, suite: str) -> list[ET.Element]:
    """Return test cases whose classname (or enclosing suite name) matches."""
    parents = {child: parent for parent in root.iter() for child in parent}
    selected: list[ET.Element] = []
    for case in root.iter("testcase"):
        classname = case.get("classname", "")
        if not classname:
            parent = parents.get(case)
            if parent is not None and parent.tag == "testsuite":
                classname = parent.get("name", "")
        if suite in classname:
            selected.append(case)
    return selected


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
        cases = cases_for(root, args.suite)
        if not cases:
            continue
        matched.append(path)
        for case in cases:
            tests += 1
            name = f"{case.get('classname')} > {case.get('name')}"
            if case.find("failure") is not None:
                failures += 1
                print(f"FAIL {name}")
            if case.find("error") is not None:
                errors += 1
                print(f"ERROR {name}")
            if case.find("skipped") is not None:
                skipped += 1
                print(f"SKIPPED {name}")
            if case.find("failure") is None and case.find("error") is None and case.find("skipped") is None:
                print(f"PASS {name}")

    if not matched:
        print(f"E2E_XML_MISSING (no {args.suite} test cases under {args.results_dir})")
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
