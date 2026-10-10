# [B18] 含0区间：有2个应命中 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 2
execute if items entity @s hotbar.5 minecraft:diamond[count=2] run scoreboard players set #b18s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count=2] run scoreboard players set #b18s tt 0
execute if items entity @s hotbar.5 minecraft:diamond[count~{min:0,max:4}] run scoreboard players set #b18 tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count~{min:0,max:4}] run scoreboard players set #b18 tt 0
