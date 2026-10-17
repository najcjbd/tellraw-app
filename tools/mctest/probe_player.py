#!/usr/bin/env python3
"""玩家实体 NBT 采样：起服 + mineflayer bot 进世界，按若干状态摆物品后 data get 玩家。

用法: python3 probe_player.py 1.21.5 --java ... [--mf ...]
输出: <work>/player-<ver>/player.json = {state: {"full": "...", "inventory": "...", "ender": "...",
                                                "selected": "...", "equipment": "..."}}
为什么需要它：player 不能 summon（实体采样里被 skipped），但网格/转换要用玩家的 Inventory/装备等 NBT。
"""
import os, sys, json, time, argparse, subprocess, shutil, re

HERE = os.path.dirname(os.path.abspath(__file__))
BOT = "ttprobe_player"

# state -> [(item replace ...)]（相对 bot）
STATES = {
    "empty": [],
    "hotbar0": ["item replace entity {b} hotbar.0 with minecraft:diamond 1"],
    "mainhand": ["item replace entity {b} weapon.mainhand with minecraft:diamond_sword 1"],
    "offhand": ["item replace entity {b} weapon.offhand with minecraft:diamond_sword 1"],
    "head": ["item replace entity {b} armor.head with minecraft:diamond_helmet 1"],
    "enderchest0": ["item replace entity {b} enderchest.0 with minecraft:diamond 1"],
    "full": [
        "item replace entity {b} hotbar.0 with minecraft:diamond 1",
        "item replace entity {b} weapon.mainhand with minecraft:diamond_sword 1",
        "item replace entity {b} weapon.offhand with minecraft:diamond_sword 1",
        "item replace entity {b} armor.head with minecraft:diamond_helmet 1",
        "item replace entity {b} armor.chest with minecraft:diamond_chestplate 1",
        "item replace entity {b} enderchest.0 with minecraft:diamond 1",
    ],
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("version")
    ap.add_argument("--java", default=os.environ.get("MCTEST_JAVA", "java"))
    ap.add_argument("--servers", default=os.environ.get("MC_SERVERS", os.path.join(HERE, ".servers")))
    ap.add_argument("--work", default=os.environ.get("MCTEST_WORK", os.path.join(HERE, "work")))
    ap.add_argument("--mf", default=os.environ.get("MF_PATH", "mineflayer"))
    a = ap.parse_args()

    jar = os.path.join(a.servers, a.version, "server.jar")
    if not os.path.exists(jar):
        print("!! 缺 server.jar:", jar); sys.exit(2)

    base = os.path.join(a.work, "player-" + a.version)
    if os.path.isdir(base):
        shutil.rmtree(base)
    os.makedirs(base)
    open(os.path.join(base, "eula.txt"), "w").write("eula=true\n")
    open(os.path.join(base, "server.properties"), "w").write(
        "level-type=minecraft\\:flat\nonline-mode=false\nmax-tick-time=-1\n"
        "spawn-npcs=false\nspawn-animals=false\nspawn-monsters=false\n"
        "level-name=world\nmotd=playerprobe\n")

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

    # bot 进世界
    env = dict(os.environ, MF_PATH=a.mf, BOT_STOP_MS="180000", BOT_NAME=BOT)
    botlog = os.path.join(base, "bot.log")
    bot = subprocess.Popen(["node", os.path.join(HERE, "bot.js"), a.version],
                           cwd=HERE, env=env, stdout=open(botlog, "w"), stderr=subprocess.STDOUT)
    t0 = time.time()
    while time.time() - t0 < 90:
        if "BOT_SPAWNED" in open(botlog, errors="ignore").read():
            print("bot 已进入世界"); break
        time.sleep(1)
    else:
        print("!! bot 未进入"); print(open(botlog, errors="ignore").read()[-400:])

    send("gamerule sendCommandFeedback true")
    send("forceload add 0 0")
    time.sleep(1)

    results = {}
    for state, cmds in STATES.items():
        send("clear %s" % BOT)
        for c in cmds:
            send(c.format(b=BOT))
        time.sleep(0.8)

        def get(expr):
            before = len(readlog())
            send("data get entity %s %s" % (BOT, expr) if expr else ("data get entity %s" % BOT))
            time.sleep(1.0)
            delta = readlog()[before:]
            m = re.search(r"has the following entity data:\s*(.+)$", delta, re.M)
            if m:
                return m.group(1).strip()
            m = re.search(r"has (?:the following|no) [^:]*:?\s*(.+)$", delta, re.M)
            return m.group(1).strip() if m else delta.strip()[-200:]

        results[state] = {
            "full": get(None),
            "inventory": get("Inventory"),
            "ender": get("EnderItems"),
            "selected": get("SelectedItem"),
            "equipment": get("equipment"),
        }
        print("[%s] captured" % state)

    out = os.path.join(base, "player.json")
    json.dump(results, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("结果:", out)
    send("stop")
    try:
        srv.wait(timeout=60)
    except Exception:
        srv.kill()
    bot.terminate()


if __name__ == "__main__":
    main()
