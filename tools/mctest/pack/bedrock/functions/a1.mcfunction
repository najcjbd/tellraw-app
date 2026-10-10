# [A1] air：空槽也不命中（已测） | 期望不匹配
scoreboard objectives add tt dummy
clear @s
execute if entity @s[hasitem={item=air,location=slot.hotbar,slot=5}] run scoreboard players set #a1 tt 1
execute unless entity @s[hasitem={item=air,location=slot.hotbar,slot=5}] run scoreboard players set #a1 tt 0
