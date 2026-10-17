#!/usr/bin/env python3
"""在某一版本上尽量召唤每个实体 -> data get 记录 NBT -> kill。

用法: python3 probe_entities.py 1.21.5 --entities entities.txt --java /path/to/java
输出: <work>/<ver>/entities.json = {entityId: {"status": ..., "raw": "..."}}
status: ok | summon_fail | no_entity | skipped
- 每个实体用唯一 tag 定位（kill 旧 -> summon -> data get @e[tag] -> kill）。
- 召唤前/后把非玩家实体 tp 到 ~ -78 ~，减少 kill 掉落物。
- 需要特殊 NBT 才能召唤的实体用 SPECIAL；不能召唤的（player）跳过。
"""
import os, sys, json, time, argparse, subprocess, shutil, re

HERE = os.path.dirname(os.path.abspath(__file__))
TAG = "mctest_probe"

SPECIAL = {
    "item": '{Item:{id:"minecraft:stone",Count:1}}',
    "falling_block": '{BlockState:{Name:"minecraft:stone"}}',
    "experience_orb": '{Value:1}',
    "painting": '{variant:"minecraft:kebab"}',
}
SKIP = {"player"}          # 不能 summon
# 无需特殊 NBT 也能召唤的其余实体：直接 summon
# （召唤失败会在结果里记为 summon_fail，并带出报错原文）


def add_tag(nbt: str, tag: str) -> str:
    body = nbt.strip()
    if not body.startswith("{"):
        return '{Tags:["%s"]}' % tag
    inner = body[1:-1].strip()
    if not inner:
        return '{Tags:["%s"]}' % tag
    return "{%s,Tags:[\"%s\"]}" % (inner, tag)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("version")
    ap.add_argument("--entities", default=os.path.join(HERE, "entities.txt"))
    ap.add_argument("--java", default=os.environ.get("MCTEST_JAVA", "java"))
    ap.add_argument("--servers", default=os.environ.get("MC_SERVERS", os.path.join(HERE, ".servers")))
    ap.add_argument("--work", default=os.environ.get("MCTEST_WORK", os.path.join(HERE, "work")))
    a = ap.parse_args()

    jar = os.path.join(a.servers, a.version, "server.jar")
    if not os.path.exists(jar):
        print("!! 缺 server.jar:", jar); sys.exit(2)

    ids = [l.strip() for l in open(a.entities, encoding="utf-8") if l.strip()]
    base = os.path.join(a.work, "probe-" + a.version)
    if os.path.isdir(base):
        shutil.rmtree(base)
    os.makedirs(base)
    open(os.path.join(base, "eula.txt"), "w").write("eula=true\n")
    open(os.path.join(base, "server.properties"), "w").write(
        "level-type=minecraft\\:flat\nonline-mode=false\nmax-tick-time=-1\n"
        "spawn-npcs=false\nspawn-animals=false\nspawn-monsters=false\n"
        "level-name=world\nmotd=entityprobe\n")

    fifo = os.path.join(base, "stdin.fifo"); os.mkfifo(fifo)
    rfd = os.open(fifo, os.O_RDWR)
    logpath = os.path.join(base, "console.log")
    srv = subprocess.Popen([a.java, "-Xmx1500M", "-jar", jar, "nogui"],
                           cwd=base, stdin=rfd, stdout=open(logpath, "w"), stderr=subprocess.STDOUT)

    def send(c):
        os.write(rfd, (c + "\n").encode())

    def readlog():
        try:
            return open(logpath, errors="ignore").read()
        except Exception:
            return ""

    latest = os.path.join(base, "logs", "latest.log")
    t0 = time.time()
    while time.time() - t0 < 300:
        if os.path.exists(latest) and "Done (" in open(latest, errors="ignore").read():
            print("服务器就绪（%.0fs）" % (time.time() - t0)); break
        time.sleep(2)
    else:
        print("!! 未就绪"); srv.kill(); sys.exit(1)

    send("gamerule sendCommandFeedback true")
    send("gamerule logAdminCommands true")
    # 强制加载 0,0 区块：新版 spawn-chunk 半径变小后，召唤到未加载区块的实体会被 @e 忽略
    send("forceload add 0 0")
    send("kill @e[type=!player]")
    time.sleep(1)

    def wait_for(before_len, needles, timeout=6.0):
        t0 = time.time()
        while time.time() - t0 < timeout:
            txt = readlog()[before_len:]
            if any(n in txt for n in needles):
                return txt
            time.sleep(0.1)
        return readlog()[before_len:]

    results = {}
    for i, eid in enumerate(ids):
        if eid in SKIP:
            results[eid] = {"status": "skipped", "raw": ""}
            continue
        special = SPECIAL.get(eid)
        tag = "probe_" + re.sub(r"[^a-z0-9_]", "_", eid)
        nbt = add_tag(special, tag) if special else ('{Tags:["%s"]}' % tag)
        send("kill @e[tag=%s]" % tag)
        before_len = len(readlog())
        sentinel = "MCTESTSENT%d" % i
        send("summon minecraft:%s 0 -60 0 %s" % (eid, nbt))
        send("data get entity @e[tag=%s,limit=1]" % tag)
        send("say %s" % sentinel)
        raw = wait_for(before_len, [sentinel], timeout=8.0)
        # 命令按序处理：哨兵之前的输出一定属于当前实体
        delta = raw.split(sentinel)[0] if sentinel in raw else raw
        send("kill @e[tag=%s]" % tag)

        def first(sub):
            for ln in delta.splitlines():
                if sub in ln:
                    return ln.strip()
            return ""

        m = re.search(r"has the following entity data:\s*(.+)$", delta, re.M)
        if m:
            results[eid] = {"status": "ok", "raw": m.group(1).strip()}
        elif "of type 'minecraft:entity_type'" in delta or "Unknown entity:" in delta:
            # 该版本还没有这个实体（新版本才加的）——正是要记录的版本差异
            results[eid] = {"status": "unknown_in_version", "raw": first("entity_type") or first("Unknown entity:")}
        elif "Can't summon entity of type" in delta:
            results[eid] = {"status": "cannot_summon", "raw": first("Can't summon entity of type")}
        elif "No entity was found" in delta:
            results[eid] = {"status": "no_entity", "raw": first("No entity was found")}
        else:
            results[eid] = {"status": "unknown", "raw": delta.strip()[-300:]}

    # 全部测完：清掉残留，再把所有非玩家实体 tp 下去（避免掉落物堆积）
    send("kill @e[type=!player]")
    time.sleep(0.5)
    send("tp @e[type=!player] ~ -78 ~")
    send("forceload remove all")
    time.sleep(0.5)

    out = os.path.join(base, "entities.json")
    json.dump(results, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    from collections import Counter
    cnt = Counter(v["status"] for v in results.values())
    print("=== %s: %s ===" % (a.version, dict(cnt)))
    send("stop")
    try:
        srv.wait(timeout=60)
    except Exception:
        srv.kill()
    print("结果:", out)


if __name__ == "__main__":
    main()
