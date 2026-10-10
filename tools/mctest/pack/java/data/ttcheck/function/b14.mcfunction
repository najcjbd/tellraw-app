# [B14] count=3 命中 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 3
execute if items entity @s hotbar.5 minecraft:diamond[count=3] run scoreboard players set #b14s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count=3] run scoreboard players set #b14s tt 0
execute if items entity @s hotbar.5 minecraft:diamond[count=3] run scoreboard players set #b14 tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count=3] run scoreboard players set #b14 tt 0
