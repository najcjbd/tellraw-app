#!/usr/bin/env python3
"""下载指定版本的官方 server.jar 到缓存目录（server.jar 不入 git）。
用法: python3 download_servers.py 1.20.4 1.20.5 [--dir DIR]
"""
import os, sys, json, urllib.request

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

def get(url):
    with urllib.request.urlopen(url, timeout=120) as r:
        return r.read()

def main():
    argv = sys.argv[1:]
    d = os.environ.get("MC_SERVERS", os.path.join(os.path.dirname(os.path.abspath(__file__)), ".servers"))
    if "--dir" in argv:
        d = argv[argv.index("--dir") + 1]
        del argv[argv.index("--dir") - 1:argv.index("--dir") + 1]
    os.makedirs(d, exist_ok=True)
    byid = {v["id"]: v for v in json.loads(get(MANIFEST))["versions"]}
    for ver in argv:
        out = os.path.join(d, ver, "server.jar")
        if os.path.exists(out):
            print("已存在:", out); continue
        v = byid.get(ver)
        if not v:
            print("!! 版本不存在:", ver); continue
        url = json.loads(get(v["url"]))["downloads"]["server"]["url"]
        sys.stdout.write("下载 %s %s\n" % (ver, url)); sys.stdout.flush()
        data = get(url)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        open(out, "wb").write(data)
        print("  ->", out, len(data), "bytes")

if __name__ == "__main__":
    main()
