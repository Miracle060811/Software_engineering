# -*- coding: utf-8 -*-
"""Lint a single slide XML; add it only when error_count == 0."""
import subprocess, json, sys, os

PID = "FSOZsZ9zYlPdEjdBlPDcK5i0nBg"
SKILL = r"C:\Users\28603\AppData\Local\Doubao\User Data\Default\.doubao\agent_mode\workspace\.skills\ppt\scripts\xml_lint.py"

path = sys.argv[1]
lint = subprocess.run([sys.executable, SKILL, "--input", path],
                      capture_output=True, text=True, encoding="utf-8")
if not lint.stdout.strip():
    print("LINT NO STDOUT. rc=", lint.returncode)
    print("STDERR:", lint.stderr[:1000])
    sys.exit(3)
report = json.loads(lint.stdout)
s = report["summary"]
print(f"LINT {path}: errors={s['error_count']} warnings={s['warning_count']} infos={s['info_count']}")
if s["error_count"] != 0:
    for sl in report["slides"]:
        for iss in sl["issues"]:
            if iss["level"] == "error":
                print("  ERROR", iss["code"], "-", iss["message"][:220])
                print("  HINT ", iss.get("hint", "")[:220])
    sys.exit(1)
for sl in report["slides"]:
    for iss in sl["issues"]:
        print("  ", iss["level"].upper(), iss["code"], "-", iss["message"][:180])
add = subprocess.run(["lark-cli", "slides", "+add-slide",
                      "--presentation", PID, "--slide", "@" + path],
                     capture_output=True, text=True, encoding="utf-8")
print(add.stdout)
if add.returncode != 0:
    print("STDERR:", add.stderr[:800])
    sys.exit(2)
