# [Q4] quantity=0 = 没有 | 期望匹配
scoreboard objectives add tt dummy
clear @s
execute if entity @s[hasitem={item=diamond,quantity=0}] run scoreboard players set #q4 tt 1
execute unless entity @s[hasitem={item=diamond,quantity=0}] run scoreboard players set #q4 tt 0
