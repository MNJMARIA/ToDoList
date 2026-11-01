#!/usr/bin/env python3
"""Builds a Markdown report from Gradle unit-test results (JUnit XML) and Android Lint XML.

Used by the CI workflows for the job summary, the release notes and the weekly report issue.
Only the Python standard library is needed.

Examples:
  make_report.py --tests-dir app/build/test-results/testDebugUnitTest \
                 --lint-xml app/build/reports/lint-results-debug.xml --title "CI report"
  make_report.py --tests-dir ... --compact        # short block for release notes
Exit code is 0 even when tests fail (the report describes failures); use --fail-on-test-failure to change that.
"""
import argparse
import glob
import os
import sys
import xml.etree.ElementTree as ET


def read_tests(tests_dir):
    """Returns (totals, failures). totals: dict tests/failures/errors/skipped/time/suites."""
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0, "time": 0.0, "suites": 0}
    failures = []
    for path in sorted(glob.glob(os.path.join(tests_dir or "", "*.xml"))):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            failures.append(("(unreadable result file)", os.path.basename(path), "XML could not be parsed"))
            continue
        suites = [root] if root.tag == "testsuite" else list(root.iter("testsuite"))
        for suite in suites:
            totals["suites"] += 1
            for key in ("tests", "failures", "errors", "skipped"):
                totals[key] += int(suite.get(key, 0) or 0)
            totals["time"] += float(suite.get("time", 0) or 0)
            for case in suite.iter("testcase"):
                for tag in ("failure", "error"):
                    node = case.find(tag)
                    if node is not None:
                        message = (node.get("message") or (node.text or "")).strip().splitlines()
                        failures.append((case.get("classname", ""), case.get("name", ""),
                                         message[0] if message else tag))
    return totals, failures


def read_lint(lint_xml):
    """Returns (counts by severity, top issues list of (severity, id, message, location))."""
    counts = {}
    issues = []
    if not lint_xml or not os.path.exists(lint_xml):
        return None, []
    try:
        root = ET.parse(lint_xml).getroot()
    except ET.ParseError:
        return None, []
    for issue in root.iter("issue"):
        severity = issue.get("severity", "Warning")
        counts[severity] = counts.get(severity, 0) + 1
        location = issue.find("location")
        where = ""
        if location is not None:
            where = os.path.basename(location.get("file", ""))
            if location.get("line"):
                where += ":" + location.get("line")
        issues.append((severity, issue.get("id", ""), issue.get("message", ""), where))
    order = {"Fatal": 0, "Error": 1, "Warning": 2, "Informational": 3}
    issues.sort(key=lambda i: (order.get(i[0], 9), i[1]))
    return counts, issues


def build(args):
    totals, failures = read_tests(args.tests_dir)
    lint_counts, lint_issues = read_lint(args.lint_xml)
    passed = totals["tests"] - totals["failures"] - totals["errors"] - totals["skipped"]
    ok = totals["tests"] > 0 and totals["failures"] == 0 and totals["errors"] == 0

    lines = []
    if not args.compact:
        lines.append("## " + args.title)
        lines.append("")
    if totals["tests"] == 0:
        lines.append("**Unit tests:** ⚠️ no test results found (the build may have failed before the tests ran)")
    else:
        status = "✅ passed" if ok else "❌ FAILED"
        lines.append("**Unit tests:** %s – %d tests, %d passed, %d failed, %d errors, %d skipped (%.1f s)" % (
            status, totals["tests"], passed, totals["failures"], totals["errors"], totals["skipped"], totals["time"]))
    if failures:
        lines.append("")
        lines.append("Failing tests:")
        for cls, name, message in failures[:args.max_items]:
            short = cls.rsplit(".", 1)[-1]
            lines.append("- `%s.%s` – %s" % (short.replace("`", "'"), name.replace("`", "'"), message.replace("`", "'")[:200]))
        if len(failures) > args.max_items:
            lines.append("- … and %d more" % (len(failures) - args.max_items))

    if args.lint_xml is None:
        pass  # the caller did not run lint (e.g. the release workflow): say nothing about it
    elif lint_counts is None:
        lines.append("")
        lines.append("**Lint:** no report found")
    else:
        lines.append("")
        total = sum(lint_counts.values())
        parts = ", ".join("%d %s" % (lint_counts[k], k.lower()) for k in
                          ("Fatal", "Error", "Warning", "Informational") if k in lint_counts)
        lines.append("**Lint:** %d issues%s" % (total, " (" + parts + ")" if parts else ""))
        serious = [i for i in lint_issues if i[0] in ("Fatal", "Error")]
        shown = serious[:args.max_items]
        if shown:
            lines.append("")
            lines.append("Errors to look at:")
            for severity, issue_id, message, where in shown:
                lines.append("- **%s** `%s` %s – %s" % (severity, issue_id, where, message.replace("`", "'")[:160]))
            if len(serious) > len(shown):
                lines.append("- … and %d more" % (len(serious) - len(shown)))

    for extra in args.extra or []:
        lines.append("")
        lines.append(extra.replace("\\n", "\n"))
    return "\n".join(lines).rstrip() + "\n", ok


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--tests-dir", help="directory with JUnit XML files")
    parser.add_argument("--lint-xml", help="Android Lint XML report")
    parser.add_argument("--title", default="Build report")
    parser.add_argument("--compact", action="store_true", help="no heading; for release notes")
    parser.add_argument("--max-items", type=int, default=15)
    parser.add_argument("--extra", action="append", help="extra Markdown paragraph (\\n allowed); repeatable")
    parser.add_argument("--out", help="write here instead of stdout")
    parser.add_argument("--fail-on-test-failure", action="store_true")
    args = parser.parse_args()

    text, ok = build(args)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(text)
    else:
        sys.stdout.write(text)
    if args.fail_on_test_failure and not ok:
        sys.exit(1)


if __name__ == "__main__":
    main()
