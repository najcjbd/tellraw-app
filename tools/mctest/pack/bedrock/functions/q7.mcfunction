# [Q7] 0.. 不做过滤 / 空也命中（已测） | 期望匹配
scoreboard objectives add tt dummy
clear @s
execute if entity @s[hasitem={item=diamond,quantity=0..}] run scoreboard players set #q7 tt 1
execute unless entity @s[hasitem={item=diamond,quantity=0..}] run scoreboard players set #q7 tt 0
