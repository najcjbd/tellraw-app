#!/usr/bin/env python3
"""跨版本对比实体 NBT（输入：artifacts 目录，下含 entities-<ver>/entities.json）。
输出按实体分组：哪些版本 NBT 相同（归为一类）、哪些不同。
"相同" 判断前会去掉本质是变量的字段（Pos/Motion/Rotation/UUID/Air/HurtTime/...）。
"""
import os, sys, json, re, glob

VARIABLE_KEYS = ["Pos", "Motion", "Rotation", "UUID", "Air", "HurtTime", "HurtByTimestamp",
                 "Fire", "FallDistance", "OnGround", "PortalCooldown", "TicksFrozen"]

def normalize(raw: str) -> str:
    s = raw
    s = re.sub(r"\b(UUID|Pos|Motion|Rotation)\s*:\s*\[[^\]]*\]", r"\1:[]", s)
    for k in VARIABLE_KEYS:
        s = re.sub(r"\b%s\s*:\s*[-\d.]+[bdfsL]?" % re.escape(k), "%s:0" % k, s)
    return s.strip()

def main():
    root = sys.argv[1] if len(sys.argv) > 1 else "artifacts"
    data = {}
    for f in glob.glob(os.path.join(root, "**", "entities.json"), recursive=True):
        ver = os.path.basename(os.path.dirname(f)).replace("entities-", "")
        data[ver] = json.load(open(f, encoding="utf-8"))
    vers = sorted(data.keys())
    print("versions:", " ".join(vers))
    ids = sorted({k for v in data.values() for k in v})
    for eid in ids:
        by_ver = {}
        for v in vers:
            r = data[v].get(eid)
            if not r:
                continue
            by_ver[v] = r["status"] if r["status"] != "ok" else normalize(r["raw"])
        # 按"普通化后的值"分组
        groups = {}
        for v, val in by_ver.items():
            key = "__NONOK__" + val if not val.startswith("{") else val
            groups.setdefault(key, []).append(v)
        changed = len([g for g in groups if not g.startswith("__NONOK__")]) > 1
        tag = "CHANGED" if changed else "SAME"
        print("\n[%s] %s" % (tag, eid))
        for key, vs in groups.items():
            label = key if key.startswith("__NONOK__") else (key[:120] + ("..." if len(key) > 120 else ""))
            print("   ", ",".join(vs), "=>", label)

if __name__ == "__main__":
    main()
