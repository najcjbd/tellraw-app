# [Q13] quantity 取反：1个应命中 | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #q13s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #q13s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=!3}] run scoreboard players set #q13 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=!3}] run scoreboard players set #q13 tt 0
