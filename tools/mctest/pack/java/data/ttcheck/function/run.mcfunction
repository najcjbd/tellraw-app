# 自动生成：Java 侧检查（gen.py）。只打印不符的项；[..] 问号项打印实测值。
scoreboard objectives add tt dummy
# 初始化：-1 = 这一项没跑到（文件没加载/有语法错）
scoreboard players set #b1 tt -1
scoreboard players set #b1s tt -1
scoreboard players set #b2 tt -1
scoreboard players set #b2s tt -1
scoreboard players set #b3 tt -1
scoreboard players set #b3s tt -1
scoreboard players set #b4 tt -1
scoreboard players set #b4s tt -1
scoreboard players set #b5 tt -1
scoreboard players set #b5s tt -1
scoreboard players set #b6 tt -1
scoreboard players set #b6s tt -1
scoreboard players set #b7 tt -1
scoreboard players set #b7s tt -1
scoreboard players set #b8 tt -1
scoreboard players set #b8s tt -1
scoreboard players set #b9 tt -1
scoreboard players set #b9s tt -1
scoreboard players set #b10 tt -1
scoreboard players set #b10s tt -1
scoreboard players set #b11 tt -1
scoreboard players set #b11s tt -1
scoreboard players set #b12 tt -1
scoreboard players set #b12s tt -1
scoreboard players set #z5 tt -1
scoreboard players set #z5s tt -1
scoreboard players set #b14 tt -1
scoreboard players set #b14s tt -1
scoreboard players set #b15 tt -1
scoreboard players set #b15s tt -1
scoreboard players set #b16 tt -1
scoreboard players set #b16s tt -1
scoreboard players set #b17 tt -1
scoreboard players set #b18 tt -1
scoreboard players set #b18s tt -1
scoreboard players set #b19 tt -1
scoreboard players set #c1 tt -1
scoreboard players set #c1s tt -1
scoreboard players set #c2 tt -1
scoreboard players set #c2s tt -1
scoreboard players set #c3 tt -1
scoreboard players set #c3s tt -1
scoreboard players set #c4 tt -1
scoreboard players set #c4s tt -1
scoreboard players set #c5 tt -1
scoreboard players set #c5s tt -1
scoreboard players set #c6 tt -1
scoreboard players set #c6s tt -1
scoreboard players set #c7 tt -1
scoreboard players set #c7s tt -1
scoreboard players set #c8 tt -1
scoreboard players set #c8s tt -1
scoreboard players set #c9 tt -1
scoreboard players set #c9s tt -1
scoreboard players set #v1 tt -1
scoreboard players set #v1s tt -1
scoreboard players set #v2 tt -1
scoreboard players set #v2s tt -1
scoreboard players set #v3 tt -1
scoreboard players set #v3s tt -1
scoreboard players set #v4 tt -1
scoreboard players set #v4s tt -1
scoreboard players set #f1 tt -1
scoreboard players set #f2 tt -1
scoreboard players set #f2s tt -1
scoreboard players set #f3 tt -1
scoreboard players set #f3s tt -1
scoreboard players set #f4 tt -1
scoreboard players set #f4s tt -1
scoreboard players set #f5 tt -1
scoreboard players set #f5s tt -1
scoreboard players set #x1 tt -1
scoreboard players set #x1s tt -1
scoreboard players set #x2 tt -1
scoreboard players set #x2s tt -1
scoreboard players set #x3 tt -1
scoreboard players set #x3s tt -1
scoreboard players set #x4 tt -1
scoreboard players set #x4s tt -1
scoreboard players set #x5 tt -1
scoreboard players set #x5s tt -1
scoreboard players set #x6 tt -1
scoreboard players set #x6s tt -1
scoreboard players set #z7 tt -1
scoreboard players set #z7s tt -1
scoreboard players set #z8 tt -1
scoreboard players set #z8s tt -1
scoreboard players set #z9 tt -1
scoreboard players set #z9s tt -1
scoreboard players set #z10 tt -1
scoreboard players set #z10s tt -1
scoreboard players set #z11 tt -1
scoreboard players set #z11s tt -1
scoreboard players set #z13 tt -1
scoreboard players set #z13s tt -1
scoreboard players set #y1 tt -1
scoreboard players set #y1s tt -1
scoreboard players set #y2 tt -1
scoreboard players set #y2s tt -1
scoreboard players set #y3 tt -1
scoreboard players set #y3s tt -1
scoreboard players set #z1 tt -1
scoreboard players set #z1s tt -1
scoreboard players set #z2 tt -1
scoreboard players set #z2s tt -1

# 先验证'条件子命令'可用（看不到 PROBE_COND_OK 就是 min_engine_version 太低/语法不对）
execute if entity @s run say PROBE_COND_OK
# 依次调用每一项
function ttcheck:b1
function ttcheck:b2
function ttcheck:b3
function ttcheck:b4
function ttcheck:b5
function ttcheck:b6
function ttcheck:b7
function ttcheck:b8
function ttcheck:b9
function ttcheck:b10
function ttcheck:b11
function ttcheck:b12
function ttcheck:z5
function ttcheck:b14
function ttcheck:b15
function ttcheck:b16
function ttcheck:b17
function ttcheck:b18
function ttcheck:b19
function ttcheck:c1
function ttcheck:c2
function ttcheck:c3
function ttcheck:c4
function ttcheck:c5
function ttcheck:c6
function ttcheck:c7
function ttcheck:c8
function ttcheck:c9
function ttcheck:v1
function ttcheck:v2
function ttcheck:v3
function ttcheck:v4
function ttcheck:f1
function ttcheck:f2
function ttcheck:f3
function ttcheck:f4
function ttcheck:f5
function ttcheck:x1
function ttcheck:x2
function ttcheck:x3
function ttcheck:x4
function ttcheck:x5
function ttcheck:x6
function ttcheck:z7
function ttcheck:z8
function ttcheck:z9
function ttcheck:z10
function ttcheck:z11
function ttcheck:z13
function ttcheck:y1
function ttcheck:y2
function ttcheck:y3
function ttcheck:z1
function ttcheck:z2

say ===== done. RESULT line below =====
tellraw @a [{"text": "b1="},{"score": {"name": "#b1", "objective": "tt"}},{"text": "b1s="},{"score": {"name": "#b1s", "objective": "tt"}},{"text": "b2="},{"score": {"name": "#b2", "objective": "tt"}},{"text": "b2s="},{"score": {"name": "#b2s", "objective": "tt"}},{"text": "b3="},{"score": {"name": "#b3", "objective": "tt"}},{"text": "b3s="},{"score": {"name": "#b3s", "objective": "tt"}},{"text": "b4="},{"score": {"name": "#b4", "objective": "tt"}},{"text": "b4s="},{"score": {"name": "#b4s", "objective": "tt"}},{"text": "b5="},{"score": {"name": "#b5", "objective": "tt"}},{"text": "b5s="},{"score": {"name": "#b5s", "objective": "tt"}},{"text": "b6="},{"score": {"name": "#b6", "objective": "tt"}},{"text": "b6s="},{"score": {"name": "#b6s", "objective": "tt"}},{"text": "b7="},{"score": {"name": "#b7", "objective": "tt"}},{"text": "b7s="},{"score": {"name": "#b7s", "objective": "tt"}},{"text": "b8="},{"score": {"name": "#b8", "objective": "tt"}},{"text": "b8s="},{"score": {"name": "#b8s", "objective": "tt"}},{"text": "b9="},{"score": {"name": "#b9", "objective": "tt"}},{"text": "b9s="},{"score": {"name": "#b9s", "objective": "tt"}},{"text": "b10="},{"score": {"name": "#b10", "objective": "tt"}},{"text": "b10s="},{"score": {"name": "#b10s", "objective": "tt"}},{"text": "b11="},{"score": {"name": "#b11", "objective": "tt"}},{"text": "b11s="},{"score": {"name": "#b11s", "objective": "tt"}},{"text": "b12="},{"score": {"name": "#b12", "objective": "tt"}},{"text": "b12s="},{"score": {"name": "#b12s", "objective": "tt"}},{"text": "z5="},{"score": {"name": "#z5", "objective": "tt"}},{"text": "z5s="},{"score": {"name": "#z5s", "objective": "tt"}},{"text": "b14="},{"score": {"name": "#b14", "objective": "tt"}},{"text": "b14s="},{"score": {"name": "#b14s", "objective": "tt"}},{"text": "b15="},{"score": {"name": "#b15", "objective": "tt"}},{"text": "b15s="},{"score": {"name": "#b15s", "objective": "tt"}},{"text": "b16="},{"score": {"name": "#b16", "objective": "tt"}},{"text": "b16s="},{"score": {"name": "#b16s", "objective": "tt"}},{"text": "b17="},{"score": {"name": "#b17", "objective": "tt"}},{"text": "b18="},{"score": {"name": "#b18", "objective": "tt"}},{"text": "b18s="},{"score": {"name": "#b18s", "objective": "tt"}},{"text": "b19="},{"score": {"name": "#b19", "objective": "tt"}},{"text": "c1="},{"score": {"name": "#c1", "objective": "tt"}},{"text": "c1s="},{"score": {"name": "#c1s", "objective": "tt"}},{"text": "c2="},{"score": {"name": "#c2", "objective": "tt"}},{"text": "c2s="},{"score": {"name": "#c2s", "objective": "tt"}},{"text": "c3="},{"score": {"name": "#c3", "objective": "tt"}},{"text": "c3s="},{"score": {"name": "#c3s", "objective": "tt"}},{"text": "c4="},{"score": {"name": "#c4", "objective": "tt"}},{"text": "c4s="},{"score": {"name": "#c4s", "objective": "tt"}},{"text": "c5="},{"score": {"name": "#c5", "objective": "tt"}},{"text": "c5s="},{"score": {"name": "#c5s", "objective": "tt"}},{"text": "c6="},{"score": {"name": "#c6", "objective": "tt"}},{"text": "c6s="},{"score": {"name": "#c6s", "objective": "tt"}},{"text": "c7="},{"score": {"name": "#c7", "objective": "tt"}},{"text": "c7s="},{"score": {"name": "#c7s", "objective": "tt"}},{"text": "c8="},{"score": {"name": "#c8", "objective": "tt"}},{"text": "c8s="},{"score": {"name": "#c8s", "objective": "tt"}},{"text": "c9="},{"score": {"name": "#c9", "objective": "tt"}},{"text": "c9s="},{"score": {"name": "#c9s", "objective": "tt"}},{"text": "v1="},{"score": {"name": "#v1", "objective": "tt"}},{"text": "v1s="},{"score": {"name": "#v1s", "objective": "tt"}},{"text": "v2="},{"score": {"name": "#v2", "objective": "tt"}},{"text": "v2s="},{"score": {"name": "#v2s", "objective": "tt"}},{"text": "v3="},{"score": {"name": "#v3", "objective": "tt"}},{"text": "v3s="},{"score": {"name": "#v3s", "objective": "tt"}},{"text": "v4="},{"score": {"name": "#v4", "objective": "tt"}},{"text": "v4s="},{"score": {"name": "#v4s", "objective": "tt"}},{"text": "f1="},{"score": {"name": "#f1", "objective": "tt"}},{"text": "f2="},{"score": {"name": "#f2", "objective": "tt"}},{"text": "f2s="},{"score": {"name": "#f2s", "objective": "tt"}},{"text": "f3="},{"score": {"name": "#f3", "objective": "tt"}},{"text": "f3s="},{"score": {"name": "#f3s", "objective": "tt"}},{"text": "f4="},{"score": {"name": "#f4", "objective": "tt"}},{"text": "f4s="},{"score": {"name": "#f4s", "objective": "tt"}},{"text": "f5="},{"score": {"name": "#f5", "objective": "tt"}},{"text": "f5s="},{"score": {"name": "#f5s", "objective": "tt"}},{"text": "x1="},{"score": {"name": "#x1", "objective": "tt"}},{"text": "x1s="},{"score": {"name": "#x1s", "objective": "tt"}},{"text": "x2="},{"score": {"name": "#x2", "objective": "tt"}},{"text": "x2s="},{"score": {"name": "#x2s", "objective": "tt"}},{"text": "x3="},{"score": {"name": "#x3", "objective": "tt"}},{"text": "x3s="},{"score": {"name": "#x3s", "objective": "tt"}},{"text": "x4="},{"score": {"name": "#x4", "objective": "tt"}},{"text": "x4s="},{"score": {"name": "#x4s", "objective": "tt"}},{"text": "x5="},{"score": {"name": "#x5", "objective": "tt"}},{"text": "x5s="},{"score": {"name": "#x5s", "objective": "tt"}},{"text": "x6="},{"score": {"name": "#x6", "objective": "tt"}},{"text": "x6s="},{"score": {"name": "#x6s", "objective": "tt"}},{"text": "z7="},{"score": {"name": "#z7", "objective": "tt"}},{"text": "z7s="},{"score": {"name": "#z7s", "objective": "tt"}},{"text": "z8="},{"score": {"name": "#z8", "objective": "tt"}},{"text": "z8s="},{"score": {"name": "#z8s", "objective": "tt"}},{"text": "z9="},{"score": {"name": "#z9", "objective": "tt"}},{"text": "z9s="},{"score": {"name": "#z9s", "objective": "tt"}},{"text": "z10="},{"score": {"name": "#z10", "objective": "tt"}},{"text": "z10s="},{"score": {"name": "#z10s", "objective": "tt"}},{"text": "z11="},{"score": {"name": "#z11", "objective": "tt"}},{"text": "z11s="},{"score": {"name": "#z11s", "objective": "tt"}},{"text": "z13="},{"score": {"name": "#z13", "objective": "tt"}},{"text": "z13s="},{"score": {"name": "#z13s", "objective": "tt"}},{"text": "y1="},{"score": {"name": "#y1", "objective": "tt"}},{"text": "y1s="},{"score": {"name": "#y1s", "objective": "tt"}},{"text": "y2="},{"score": {"name": "#y2", "objective": "tt"}},{"text": "y2s="},{"score": {"name": "#y2s", "objective": "tt"}},{"text": "y3="},{"score": {"name": "#y3", "objective": "tt"}},{"text": "y3s="},{"score": {"name": "#y3s", "objective": "tt"}},{"text": "z1="},{"score": {"name": "#z1", "objective": "tt"}},{"text": "z1s="},{"score": {"name": "#z1s", "objective": "tt"}},{"text": "z2="},{"score": {"name": "#z2", "objective": "tt"}},{"text": "z2s="},{"score": {"name": "#z2s", "objective": "tt"}}]
clear @s
scoreboard players reset @s tt
