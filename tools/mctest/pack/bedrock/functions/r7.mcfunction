# [R7] 0.. 开放区间 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #r7s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #r7s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0..}] run scoreboard players set #r7 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0..}] run scoreboard players set #r7 tt 0
