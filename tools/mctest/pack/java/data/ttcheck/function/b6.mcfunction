# [B6] hotbar.0 单格 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.0 with minecraft:diamond 1
execute if items entity @s hotbar.0 minecraft:diamond run scoreboard players set #b6s tt 1
execute unless items entity @s hotbar.0 minecraft:diamond run scoreboard players set #b6s tt 0
execute if items entity @s hotbar.0 minecraft:diamond run scoreboard players set #b6 tt 1
execute unless items entity @s hotbar.0 minecraft:diamond run scoreboard players set #b6 tt 0
