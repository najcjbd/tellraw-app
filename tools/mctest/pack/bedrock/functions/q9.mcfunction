# [Q9] 0..4：空也应命中(含0) | 只看实测值
scoreboard objectives add tt dummy
clear @s
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,quantity=0..4}] run scoreboard players set #q9 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,quantity=0..4}] run scoreboard players set #q9 tt 0
