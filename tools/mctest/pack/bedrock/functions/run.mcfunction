# 自动生成：基岩侧检查（gen.py）。只打印不符的项；[..] 问号项打印实测值。
scoreboard objectives add tt dummy
# 初始化：-1 = 这一项没跑到（文件没加载/有语法错）
scoreboard players set #e1 tt -1
scoreboard players set #e1s tt -1
scoreboard players set #e2 tt -1
scoreboard players set #e2s tt -1
scoreboard players set #e3 tt -1
scoreboard players set #e3s tt -1
scoreboard players set #e4 tt -1
scoreboard players set #e4s tt -1
scoreboard players set #e5 tt -1
scoreboard players set #e5s tt -1
scoreboard players set #e6 tt -1
scoreboard players set #e6s tt -1
scoreboard players set #e7 tt -1
scoreboard players set #e7s tt -1
scoreboard players set #e8 tt -1
scoreboard players set #e8s tt -1
scoreboard players set #z6 tt -1
scoreboard players set #z6s tt -1
scoreboard players set #q1 tt -1
scoreboard players set #q1s tt -1
scoreboard players set #q2 tt -1
scoreboard players set #q2s tt -1
scoreboard players set #q3 tt -1
scoreboard players set #q3s tt -1
scoreboard players set #q4 tt -1
scoreboard players set #q5 tt -1
scoreboard players set #q5s tt -1
scoreboard players set #q6 tt -1
scoreboard players set #q6s tt -1
scoreboard players set #q7 tt -1
scoreboard players set #q8 tt -1
scoreboard players set #q8s tt -1
scoreboard players set #q9 tt -1
scoreboard players set #q10 tt -1
scoreboard players set #q10s tt -1
scoreboard players set #q11 tt -1
scoreboard players set #q11s tt -1
scoreboard players set #q12 tt -1
scoreboard players set #q12s tt -1
scoreboard players set #q13 tt -1
scoreboard players set #q13s tt -1
scoreboard players set #a1 tt -1
scoreboard players set #a2 tt -1
scoreboard players set #a2s tt -1
scoreboard players set #r1 tt -1
scoreboard players set #r1s tt -1
scoreboard players set #r2 tt -1
scoreboard players set #r2s tt -1
scoreboard players set #r3 tt -1
scoreboard players set #r3s tt -1
scoreboard players set #r4 tt -1
scoreboard players set #r4s tt -1
scoreboard players set #r5 tt -1
scoreboard players set #r5s tt -1
scoreboard players set #r6 tt -1
scoreboard players set #r6s tt -1
scoreboard players set #r7 tt -1
scoreboard players set #r7s tt -1
scoreboard players set #y4 tt -1
scoreboard players set #y4s tt -1
scoreboard players set #y5 tt -1
scoreboard players set #y5s tt -1
scoreboard players set #x7 tt -1
scoreboard players set #x7s tt -1
scoreboard players set #z3 tt -1
scoreboard players set #z3s tt -1
scoreboard players set #z4 tt -1
scoreboard players set #z4s tt -1
scoreboard players set #s1 tt -1
scoreboard players set #s2 tt -1
scoreboard players set #s2s tt -1
scoreboard players set #s3 tt -1
scoreboard players set #s3s tt -1
scoreboard players set #s4 tt -1
scoreboard players set #s4s tt -1

# 先验证'条件子命令'可用（看不到 PROBE_COND_OK 就是 min_engine_version 太低/语法不对）
execute if entity @s run say PROBE_COND_OK
# 依次调用每一项
function e1
function e2
function e3
function e4
function e5
function e6
function e7
function e8
function z6
function q1
function q2
function q3
function q4
function q5
function q6
function q7
function q8
function q9
function q10
function q11
function q12
function q13
function a1
function a2
function r1
function r2
function r3
function r4
function r5
function r6
function r7
function y4
function y5
function x7
function z3
function z4
function s1
function s2
function s3
function s4

say ===== done. RESULT line below =====
tellraw @a {"rawtext":[{"text": "e1="},{"score": {"name": "#e1", "objective": "tt"}},{"text": "e1s="},{"score": {"name": "#e1s", "objective": "tt"}},{"text": "e2="},{"score": {"name": "#e2", "objective": "tt"}},{"text": "e2s="},{"score": {"name": "#e2s", "objective": "tt"}},{"text": "e3="},{"score": {"name": "#e3", "objective": "tt"}},{"text": "e3s="},{"score": {"name": "#e3s", "objective": "tt"}},{"text": "e4="},{"score": {"name": "#e4", "objective": "tt"}},{"text": "e4s="},{"score": {"name": "#e4s", "objective": "tt"}},{"text": "e5="},{"score": {"name": "#e5", "objective": "tt"}},{"text": "e5s="},{"score": {"name": "#e5s", "objective": "tt"}},{"text": "e6="},{"score": {"name": "#e6", "objective": "tt"}},{"text": "e6s="},{"score": {"name": "#e6s", "objective": "tt"}},{"text": "e7="},{"score": {"name": "#e7", "objective": "tt"}},{"text": "e7s="},{"score": {"name": "#e7s", "objective": "tt"}},{"text": "e8="},{"score": {"name": "#e8", "objective": "tt"}},{"text": "e8s="},{"score": {"name": "#e8s", "objective": "tt"}},{"text": "z6="},{"score": {"name": "#z6", "objective": "tt"}},{"text": "z6s="},{"score": {"name": "#z6s", "objective": "tt"}},{"text": "q1="},{"score": {"name": "#q1", "objective": "tt"}},{"text": "q1s="},{"score": {"name": "#q1s", "objective": "tt"}},{"text": "q2="},{"score": {"name": "#q2", "objective": "tt"}},{"text": "q2s="},{"score": {"name": "#q2s", "objective": "tt"}},{"text": "q3="},{"score": {"name": "#q3", "objective": "tt"}},{"text": "q3s="},{"score": {"name": "#q3s", "objective": "tt"}},{"text": "q4="},{"score": {"name": "#q4", "objective": "tt"}},{"text": "q5="},{"score": {"name": "#q5", "objective": "tt"}},{"text": "q5s="},{"score": {"name": "#q5s", "objective": "tt"}},{"text": "q6="},{"score": {"name": "#q6", "objective": "tt"}},{"text": "q6s="},{"score": {"name": "#q6s", "objective": "tt"}},{"text": "q7="},{"score": {"name": "#q7", "objective": "tt"}},{"text": "q8="},{"score": {"name": "#q8", "objective": "tt"}},{"text": "q8s="},{"score": {"name": "#q8s", "objective": "tt"}},{"text": "q9="},{"score": {"name": "#q9", "objective": "tt"}},{"text": "q10="},{"score": {"name": "#q10", "objective": "tt"}},{"text": "q10s="},{"score": {"name": "#q10s", "objective": "tt"}},{"text": "q11="},{"score": {"name": "#q11", "objective": "tt"}},{"text": "q11s="},{"score": {"name": "#q11s", "objective": "tt"}},{"text": "q12="},{"score": {"name": "#q12", "objective": "tt"}},{"text": "q12s="},{"score": {"name": "#q12s", "objective": "tt"}},{"text": "q13="},{"score": {"name": "#q13", "objective": "tt"}},{"text": "q13s="},{"score": {"name": "#q13s", "objective": "tt"}},{"text": "a1="},{"score": {"name": "#a1", "objective": "tt"}},{"text": "a2="},{"score": {"name": "#a2", "objective": "tt"}},{"text": "a2s="},{"score": {"name": "#a2s", "objective": "tt"}},{"text": "r1="},{"score": {"name": "#r1", "objective": "tt"}},{"text": "r1s="},{"score": {"name": "#r1s", "objective": "tt"}},{"text": "r2="},{"score": {"name": "#r2", "objective": "tt"}},{"text": "r2s="},{"score": {"name": "#r2s", "objective": "tt"}},{"text": "r3="},{"score": {"name": "#r3", "objective": "tt"}},{"text": "r3s="},{"score": {"name": "#r3s", "objective": "tt"}},{"text": "r4="},{"score": {"name": "#r4", "objective": "tt"}},{"text": "r4s="},{"score": {"name": "#r4s", "objective": "tt"}},{"text": "r5="},{"score": {"name": "#r5", "objective": "tt"}},{"text": "r5s="},{"score": {"name": "#r5s", "objective": "tt"}},{"text": "r6="},{"score": {"name": "#r6", "objective": "tt"}},{"text": "r6s="},{"score": {"name": "#r6s", "objective": "tt"}},{"text": "r7="},{"score": {"name": "#r7", "objective": "tt"}},{"text": "r7s="},{"score": {"name": "#r7s", "objective": "tt"}},{"text": "y4="},{"score": {"name": "#y4", "objective": "tt"}},{"text": "y4s="},{"score": {"name": "#y4s", "objective": "tt"}},{"text": "y5="},{"score": {"name": "#y5", "objective": "tt"}},{"text": "y5s="},{"score": {"name": "#y5s", "objective": "tt"}},{"text": "x7="},{"score": {"name": "#x7", "objective": "tt"}},{"text": "x7s="},{"score": {"name": "#x7s", "objective": "tt"}},{"text": "z3="},{"score": {"name": "#z3", "objective": "tt"}},{"text": "z3s="},{"score": {"name": "#z3s", "objective": "tt"}},{"text": "z4="},{"score": {"name": "#z4", "objective": "tt"}},{"text": "z4s="},{"score": {"name": "#z4s", "objective": "tt"}},{"text": "s1="},{"score": {"name": "#s1", "objective": "tt"}},{"text": "s2="},{"score": {"name": "#s2", "objective": "tt"}},{"text": "s2s="},{"score": {"name": "#s2s", "objective": "tt"}},{"text": "s3="},{"score": {"name": "#s3", "objective": "tt"}},{"text": "s3s="},{"score": {"name": "#s3s", "objective": "tt"}},{"text": "s4="},{"score": {"name": "#s4", "objective": "tt"}},{"text": "s4s="},{"score": {"name": "#s4s", "objective": "tt"}}]}
clear @s
scoreboard players reset @s tt
