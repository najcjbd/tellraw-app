# [Q8] 0..4：总量3应命中 | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 3
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q8s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q8s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,quantity=0..4}] run scoreboard players set #q8 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,quantity=0..4}] run scoreboard players set #q8 tt 0
