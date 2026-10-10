# [E7] 不写 location | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #e7s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #e7s tt 0
execute if entity @s[hasitem={item=diamond}] run scoreboard players set #e7 tt 1
execute unless entity @s[hasitem={item=diamond}] run scoreboard players set #e7 tt 0
