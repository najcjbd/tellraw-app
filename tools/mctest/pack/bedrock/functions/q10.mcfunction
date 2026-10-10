# [Q10] 2..：总量3应命中 | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 3
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q10s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q10s tt 0
execute if entity @s[hasitem={item=diamond,quantity=2..}] run scoreboard players set #q10 tt 1
execute unless entity @s[hasitem={item=diamond,quantity=2..}] run scoreboard players set #q10 tt 0
