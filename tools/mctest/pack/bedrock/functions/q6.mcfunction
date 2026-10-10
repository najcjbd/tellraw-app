# [Q6] 0.. 不做过滤（已测） | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #q6s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #q6s tt 0
execute if entity @s[hasitem={item=diamond,quantity=0..}] run scoreboard players set #q6 tt 1
execute unless entity @s[hasitem={item=diamond,quantity=0..}] run scoreboard players set #q6 tt 0
