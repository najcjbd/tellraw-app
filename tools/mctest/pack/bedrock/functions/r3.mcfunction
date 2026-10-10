# [R3] 0..2：第0格命中 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #r3s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #r3s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0..2}] run scoreboard players set #r3 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0..2}] run scoreboard players set #r3 tt 0
