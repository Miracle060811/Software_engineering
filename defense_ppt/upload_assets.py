# -*- coding: utf-8 -*-
import subprocess, json, os, glob

PID = "FSOZsZ9zYlPdEjdBlPDcK5i0nBg"
ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "assets")
tokens = {}
for f in sorted(glob.glob(os.path.join(ASSETS, "*"))):
    name = os.path.basename(f)
    rel = "./assets/" + name
    p = subprocess.run(["lark-cli", "slides", "+media-upload",
                        "--file", rel, "--presentation", PID],
                       capture_output=True, text=True, encoding="utf-8")
    out = p.stdout.strip()
    try:
        j = json.loads(out)
        tokens[name] = j["data"]["file_token"]
        print("OK", name, "->", tokens[name])
    except Exception as e:
        print("FAIL", name, "rc=", p.returncode)
        print("STDOUT:", out[:500])
        print("STDERR:", p.stderr[:500])

with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "tokens.json"), "w", encoding="utf-8") as f:
    json.dump(tokens, f, ensure_ascii=False, indent=2)
print("=== TOKENS SAVED ===")
