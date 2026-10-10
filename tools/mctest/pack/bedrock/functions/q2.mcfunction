# [Q2] 多槽位 quantity=3 = 整栏总量? | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 3
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q2s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q2s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,quantity=3}] run scoreboard players set #q2 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,quantity=3}] run scoreboard players set #q2 tt 0
