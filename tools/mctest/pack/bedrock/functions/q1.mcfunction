# [Q1] 单格 quantity=3 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 3
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q1s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q1s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q1 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q1 tt 0
