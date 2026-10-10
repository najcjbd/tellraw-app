#!/usr/bin/env python3
"""按"用例文件"跑真机测试：每个用例给出命令 + 期望 bot 收到的聊天。

用例格式（cases.json）：
  [
    {"name": "java-tellraw-text",
     "edition": "java",                      # java | bedrock | both（默认 both）
     "java":      "tellraw @a {\"text\":\"hi\"}",
     "bedrock":   "tellraw @a {\"rawtext\":[{\"text\":\"hi\"}]}",
     "expectChat": "hi"}
  ]
这些命令通常是**程序生成**的（后续由生成器写入）；本脚本只负责"拿去真机跑、看结果对不对"。

用法:
  python3 run_cases.py 1.21.5 --cases cases.json --java /path/to/java21/bin/java
"""
import os, sys, json, time, argparse, subprocess, shutil

HERE = os.path.dirname(os.path.abspath(__file__))


def _ver_tuple(v):
    return tuple(int(x) for x in v.split(".") if x.isdigit())

def _version_ok(version, spec):
    """spec: None=全版本；list[str] 混合精确/比较（如 "1.20.5"、"<=1.20.4"、">=1.20.5"）。"""
    if not spec:
        return True
    for s in spec:
        if s.startswith(">="):
            if _ver_tuple(version) >= _ver_tuple(s[2:]): return True
        elif s.startswith("<="):
            if _ver_tuple(version) <= _ver_tuple(s[2:]): return True
        elif s.startswith(">"):
            if _ver_tuple(version) > _ver_tuple(s[1:]): return True
        elif s.startswith("<"):
            if _ver_tuple(version) < _ver_tuple(s[1:]): return True
        elif version == s:
            return True
    return False

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("version")
    ap.add_argument("--cases", required=True, action="append")
    ap.add_argument("--java", default=os.environ.get("MCTEST_JAVA", "java"))
    ap.add_argument("--servers", default=os.environ.get("MC_SERVERS", os.path.join(HERE, "servers")))
    ap.add_argument("--work", default=os.environ.get("MCTEST_WORK", os.path.join(HERE, "work")))
    ap.add_argument("--mf", default=os.environ.get("MF_PATH", "mineflayer"))
    ap.add_argument("--name", default=os.environ.get("BOT_NAME", "ttcheck_test"))
    a = ap.parse_args()

    jar = os.path.join(a.servers, a.version, "server.jar")
    if not os.path.exists(jar):
        print("!! 缺 server.jar:", jar); sys.exit(2)
    cases = []
    for f in a.cases:
        cases.extend(json.load(open(f, encoding="utf-8")))

    base = os.path.join(a.work, "cases-" + a.version)
    if os.path.isdir(base):
        shutil.rmtree(base)
    os.makedirs(base)
    open(os.path.join(base, "eula.txt"), "w").write("eula=true\n")
    open(os.path.join(base, "server.properties"), "w").write(
        "level-type=minecraft\\:flat\nonline-mode=false\nmax-tick-time=-1\n"
        "spawn-npcs=false\nspawn-animals=false\nspawn-monsters=false\n"
        "level-name=world\nmotd=ttcases\n")

    fifo = os.path.join(base, "stdin.fifo"); os.mkfifo(fifo)
    rfd = os.open(fifo, os.O_RDWR)
    log = open(os.path.join(base, "console.log"), "w")
    srv = subprocess.Popen([a.java, "-Xmx1500M", "-jar", jar, "nogui"],
                           cwd=base, stdin=rfd, stdout=log, stderr=subprocess.STDOUT)

    def send(cmd):
        os.write(rfd, (cmd + "\n").encode())

    logp = os.path.join(base, "logs", "latest.log")
    t0 = time.time()
    while time.time() - t0 < 300:
        if os.path.exists(logp) and "Done (" in open(logp, errors="ignore").read():
            print("服务器就绪（%.0fs）" % (time.time() - t0)); break
        time.sleep(2)
    else:
        print("!! 未就绪"); srv.kill(); sys.exit(1)

    env = dict(os.environ, MF_PATH=a.mf, BOT_STOP_MS="60000", BOT_NAME=a.name)
    botlog = os.path.join(base, "bot.log")
    bot = subprocess.Popen(["node", os.path.join(HERE, "bot.js"), a.version],
                           cwd=HERE, env=env, stdout=open(botlog, "w"), stderr=subprocess.STDOUT)
    t0 = time.time()
    while time.time() - t0 < 60:
        if "BOT_SPAWNED" in open(botlog, errors="ignore").read():
            print("bot 已进入世界"); break
        time.sleep(1)

    is_java = a.version not in ("bedrock",)   # 这里版本都是 Java 版；bedrock 走另一套 harness
    passed = failed = skipped = 0
    for c in cases:
        # 版本过滤：可选 "versions": ["1.20.5","1.21.5"] 或 ["<=1.20.4"] / [">=1.20.5"]（不写=所有版本）
        if not _version_ok(a.version, c.get("versions")):
            print("[SKIP] " + c.get("name", "?") + "  (versions=" + str(c.get("versions")) + ")"); skipped += 1; continue
        cmd = c.get("java") if is_java else c.get("bedrock")
        if not cmd:
            continue
        # 可选用例前置（摆物品等）；<bot> 占位替换成 bot 用户名
        for sc in c.get("setup", []) or []:
            send(sc.replace("<bot>", a.name))
            time.sleep(1)
        before = open(botlog, errors="ignore").read()
        send(cmd)
        time.sleep(3)
        after = open(botlog, errors="ignore").read()[len(before):]
        got = "\n".join(l for l in after.splitlines() if l.startswith("CHAT:"))
        exp = c.get("expectChat", "")
        present = (exp != "") and (exp in got)
        mode = c.get("expect", "present")
        ok = (present if mode == "present" else (not present)) if exp != "" else True
        print(("[PASS] " if ok else "[FAIL] ") + c.get("name", "?") + "  expect=" + mode + "  got=" + got[:120].replace("\n", " | "))
        passed += 1 if ok else 0
        failed += 0 if ok else 1

    print("=== cases: %d passed, %d failed, %d skipped ===" % (passed, failed, skipped))
    send("stop")
    try:
        srv.wait(timeout=60)
    except Exception:
        srv.kill()
    bot.terminate()
    sys.exit(1 if failed else 0)

if __name__ == "__main__":
    main()
