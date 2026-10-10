# [Q5] 有钻石时 quantity=0 不命中 | 期望不匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #q5s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #q5s tt 0
execute if entity @s[hasitem={item=diamond,quantity=0}] run scoreboard players set #q5 tt 1
execute unless entity @s[hasitem={item=diamond,quantity=0}] run scoreboard players set #q5 tt 0
