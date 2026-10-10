# [B7] container.0 是否 = hotbar.0 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.0 with minecraft:diamond 1
execute if items entity @s hotbar.0 minecraft:diamond run scoreboard players set #b7s tt 1
execute unless items entity @s hotbar.0 minecraft:diamond run scoreboard players set #b7s tt 0
execute if items entity @s container.0 minecraft:diamond run scoreboard players set #b7 tt 1
execute unless items entity @s container.0 minecraft:diamond run scoreboard players set #b7 tt 0
