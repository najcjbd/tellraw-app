# [R4] 0..2：第5格不命中 | 期望不匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #r4s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #r4s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0..2}] run scoreboard players set #r4 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0..2}] run scoreboard players set #r4 tt 0
