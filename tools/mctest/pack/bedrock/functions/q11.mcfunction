# [Q11] 2..：总量1应不命中 | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #q11s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #q11s tt 0
execute if entity @s[hasitem={item=diamond,quantity=2..}] run scoreboard players set #q11 tt 1
execute unless entity @s[hasitem={item=diamond,quantity=2..}] run scoreboard players set #q11 tt 0
