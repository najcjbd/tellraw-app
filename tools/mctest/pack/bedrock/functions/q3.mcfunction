# [Q3] 两格各1：总量2，quantity=1 命中吗 | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
replaceitem entity @s slot.hotbar 3 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=3}] run scoreboard players set #q3s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=3}] run scoreboard players set #q3s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,quantity=1}] run scoreboard players set #q3 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,quantity=1}] run scoreboard players set #q3 tt 0
