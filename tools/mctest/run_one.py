#!/usr/bin/env python3
"""在某一版本上无头起服 + mineflayer bot 进世界，跑自检数据包并回收结果。

用法:
  python3 run_one.py 1.20.5 --java /usr/lib/jvm/java-21-openjdk-arm64/bin/java
环境/参数:
  --servers  server.jar 缓存目录（默认 ./servers 或 $MC_SERVERS）
  --work     运行目录（默认 ./work 或 $MCTEST_WORK）
  --pack     自检数据包目录（默认 ../../ttellraw/need/autocheck/java）
  --java     启动服务端的 java（默认 $MCTEST_JAVA 或 java）
  --mf       mineflayer 路径（默认 $MF_PATH 或按 node_modules 解析）
  --name     bot 用户名（默认 ttcheck_test）
说明: server.jar 用 download_servers.py 下载；本脚本**不改任何 app 代码**，只把它当"真机"验证环境。
"""
import os, sys, json, time, subprocess, zipfile, shutil, argparse

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("version")
    ap.add_argument("--java", default=os.environ.get("MCTEST_JAVA", "java"))
    ap.add_argument("--servers", default=os.environ.get("MC_SERVERS", os.path.join(HERE, "servers")))
    ap.add_argument("--work", default=os.environ.get("MCTEST_WORK", os.path.join(HERE, "work")))
    ap.add_argument("--pack", default=os.environ.get("MCTEST_PACK", os.path.join(REPO, "ttellraw", "need", "autocheck", "java")))
    ap.add_argument("--mf", default=os.environ.get("MF_PATH", "mineflayer"))
    ap.add_argument("--name", default=os.environ.get("BOT_NAME", "ttcheck_test"))
    a = ap.parse_args()

    jar = os.path.join(a.servers, a.version, "server.jar")
    if not os.path.exists(jar):
        print("!! 缺 server.jar:", jar, "（先跑 download_servers.py）"); sys.exit(2)

    base = os.path.join(a.work, a.version)
    with zipfile.ZipFile(jar) as z:
        pv = json.loads(z.read("version.json")).get("pack_version", {})

    if os.path.isdir(base):
        shutil.rmtree(base)
    os.makedirs(base)
    open(os.path.join(base, "eula.txt"), "w").write("eula=true\n")
    open(os.path.join(base, "server.properties"), "w").write(
        "level-type=minecraft\\:flat\nonline-mode=false\nmax-tick-time=-1\n"
        "spawn-npcs=false\nspawn-animals=false\nspawn-monsters=false\n"
        "function-permission-level=4\nlevel-name=world\nmotd=ttcheck\n"
    )

    # 注册目录名本身版本敏感：<=1.20.6 复数 functions/tags/functions；1.21 起单数 function/tags/function
    data_fmt = pv.get("data", 0)
    plural = "s" if data_fmt < 48 else ""
    dst = os.path.join(base, "world", "datapacks", "ttcheck")
    os.makedirs(dst, exist_ok=True)
    shutil.copytree(os.path.join(a.pack, "data"), os.path.join(dst, "data"), dirs_exist_ok=True)
    if plural:
        fdir = os.path.join(dst, "data", "ttcheck", "function")
        if os.path.isdir(fdir):
            os.rename(fdir, os.path.join(dst, "data", "ttcheck", "functions"))
        tdir = os.path.join(dst, "data", "ttcheck", "tags", "function")
        if os.path.isdir(tdir):
            os.rename(tdir, os.path.join(dst, "data", "ttcheck", "tags", "functions"))
    print("注册目录名:", "functions" if plural else "function", "| data 格式:", data_fmt)
    if "data_major" in pv:
        mcmeta = {"pack": {"description": "ttcheck",
                           "min_format": [pv["data_major"], pv.get("data_minor", 0)],
                           "max_format": [pv["data_major"], 2147483647]}}
    else:
        mcmeta = {"pack": {"description": "ttcheck", "pack_format": pv.get("data", 0)}}
    json.dump(mcmeta, open(os.path.join(dst, "pack.mcmeta"), "w"), ensure_ascii=False, indent=2)

    fifo = os.path.join(base, "stdin.fifo")
    os.mkfifo(fifo)
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
        print("!! 未就绪"); print(open(os.path.join(base, "console.log"), errors="ignore").read()[-800:])
        srv.kill(); sys.exit(1)

    env = dict(os.environ, MF_PATH=a.mf, BOT_STOP_MS="40000", BOT_NAME=a.name)
    botlog = open(os.path.join(base, "bot.log"), "w")
    bot = subprocess.Popen(["node", os.path.join(HERE, "bot.js"), a.version],
                           cwd=HERE, env=env, stdout=botlog, stderr=subprocess.STDOUT)
    t0 = time.time()
    while time.time() - t0 < 60:
        if "BOT_SPAWNED" in open(os.path.join(base, "bot.log"), errors="ignore").read():
            print("bot 已进入世界"); break
        time.sleep(1)
    else:
        print("!! bot 未进入")

    send("execute as %s run function ttcheck:run" % a.name)
    time.sleep(20)
    send("say ===RUN_DONE===")
    time.sleep(5)
    for line in open(os.path.join(base, "bot.log"), errors="ignore").read().splitlines():
        if line.startswith("CHAT:"):
            print("  ", line[:200])
    send("stop")
    try:
        srv.wait(timeout=60)
    except Exception:
        srv.kill()
    bot.terminate()

if __name__ == "__main__":
    main()
