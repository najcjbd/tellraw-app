# [Z3] 数组是"且"：只有钻石 -> 不命中 | 期望不匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #z3s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #z3s tt 0
execute if entity @s[hasitem=[{item=diamond},{item=iron_ingot}]] run scoreboard players set #z3 tt 1
execute unless entity @s[hasitem=[{item=diamond},{item=iron_ingot}]] run scoreboard players set #z3 tt 0
