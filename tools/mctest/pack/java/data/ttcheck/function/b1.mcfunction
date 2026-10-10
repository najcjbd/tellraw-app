# [B1] hotbar.* 通配可用 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 1
execute if items entity @s hotbar.5 minecraft:diamond run scoreboard players set #b1s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond run scoreboard players set #b1s tt 0
execute if items entity @s hotbar.* minecraft:diamond run scoreboard players set #b1 tt 1
execute unless items entity @s hotbar.* minecraft:diamond run scoreboard players set #b1 tt 0
