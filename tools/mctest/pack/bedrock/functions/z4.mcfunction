# [Z4] 数组是"且"：两样都有 -> 命中 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
replaceitem entity @s slot.hotbar 1 iron_ingot 1
execute if entity @s[hasitem={item=iron_ingot,location=slot.hotbar,slot=1}] run scoreboard players set #z4s tt 1
execute unless entity @s[hasitem={item=iron_ingot,location=slot.hotbar,slot=1}] run scoreboard players set #z4s tt 0
execute if entity @s[hasitem=[{item=diamond},{item=iron_ingot}]] run scoreboard players set #z4 tt 1
execute unless entity @s[hasitem=[{item=diamond},{item=iron_ingot}]] run scoreboard players set #z4 tt 0
